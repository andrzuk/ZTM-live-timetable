package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteStopEntity
import com.example.data.model.LiveDeparture
import com.example.data.model.StopDetails
import com.example.data.model.TransitStop
import com.example.data.model.VehicleType
import com.example.data.repository.PoznanTransitRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TransitFilterType(val label: String) {
    ALL("Wszystkie"),
    TRAMS("Tramwaje"),
    BUSES("Autobusy"),
    PST("PST")
}

enum class AppScreen {
    HOME,
    SEARCH,
    NEARBY,
    STOP_DETAILS
}

data class TransitUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val selectedStop: TransitStop? = null,
    val stopDetails: StopDetails? = null,
    val isDeparturesLoading: Boolean = false,
    val departuresError: String? = null,
    val selectedLineFilter: String? = null, // null for all lines at stop
    val searchQuery: String = "",
    val activeFilter: TransitFilterType = TransitFilterType.ALL,
    val userLat: Double? = null,
    val userLng: Double? = null,
    val isLocating: Boolean = false,
    val locationMessage: String? = null,
    val isRefreshing: Boolean = false
)

class TransitViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = PoznanTransitRepository(database.favoriteStopDao())
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _uiState = MutableStateFlow(TransitUiState())
    val uiState: StateFlow<TransitUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    // All stops combined with favorites and calculated GPS distances
    val allStopsState: StateFlow<List<TransitStop>> = combine(
        repository.favoriteStopsFlow,
        _uiState
    ) { favorites, state ->
        val favIds = favorites.map { it.stopId }.toSet()
        val allStops = repository.getAllStops()
        
        allStops.map { stop ->
            var dist: Float? = null
            if (state.userLat != null && state.userLng != null) {
                val result = FloatArray(1)
                android.location.Location.distanceBetween(
                    state.userLat, state.userLng,
                    stop.latitude, stop.longitude,
                    result
                )
                dist = result[0]
            }
            stop.copy(
                isFavorite = favIds.contains(stop.id),
                distanceMeters = dist
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repository.getAllStops()
    )

    // Filtered stops based on search query and filter chip
    val filteredStopsState: StateFlow<List<TransitStop>> = combine(
        allStopsState,
        _uiState
    ) { stops, state ->
        val query = state.searchQuery.trim().lowercase()
        stops.filter { stop ->
            val matchesQuery = query.isEmpty() ||
                    stop.name.lowercase().contains(query) ||
                    stop.symbol.lowercase().contains(query) ||
                    stop.lines.any { it.lowercase() == query || it.lowercase().contains(query) } ||
                    stop.description.lowercase().contains(query)

            val matchesFilter = when (state.activeFilter) {
                TransitFilterType.ALL -> true
                TransitFilterType.TRAMS -> stop.isTram
                TransitFilterType.BUSES -> stop.isBus
                TransitFilterType.PST -> stop.hasPst
            }

            matchesQuery && matchesFilter
        }.sortedWith(
            if (state.userLat != null && state.userLng != null) {
                compareBy<TransitStop> { it.distanceMeters ?: Float.MAX_VALUE }
            } else {
                compareBy<TransitStop> { !it.isFavorite }.thenBy { it.name }
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Favorite stops with their current info
    val favoriteStops: StateFlow<List<TransitStop>> = allStopsState.map { stops ->
        stops.filter { it.isFavorite }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Nearby stops (closest by distance)
    val nearbyStops: StateFlow<List<TransitStop>> = allStopsState.map { stops ->
        stops.filter { it.distanceMeters != null }
            .sortedBy { it.distanceMeters }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Automatically attempt to fetch GPS location on startup
        refreshUserLocation()
    }

    fun navigateTo(screen: AppScreen) {
        if (screen != AppScreen.STOP_DETAILS) {
            stopAutoRefresh()
        }
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun selectStop(stop: TransitStop) {
        _uiState.value = _uiState.value.copy(
            selectedStop = stop,
            currentScreen = AppScreen.STOP_DETAILS,
            selectedLineFilter = null,
            isDeparturesLoading = true,
            departuresError = null
        )
        loadDeparturesForStop(stop)
        startAutoRefresh(stop)
    }

    fun toggleLineFilter(line: String) {
        val current = _uiState.value.selectedLineFilter
        _uiState.value = _uiState.value.copy(
            selectedLineFilter = if (current == line) null else line
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setFilter(filter: TransitFilterType) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }

    fun toggleFavorite(stop: TransitStop) {
        viewModelScope.launch {
            repository.toggleFavorite(stop)
            // Update selectedStop isFavorite status if active
            if (_uiState.value.selectedStop?.id == stop.id) {
                val updatedStop = _uiState.value.selectedStop?.copy(isFavorite = !stop.isFavorite)
                _uiState.value = _uiState.value.copy(selectedStop = updatedStop)
            }
        }
    }

    fun refreshDepartures() {
        val currentStop = _uiState.value.selectedStop ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                val details = repository.getLiveDepartures(currentStop)
                _uiState.value = _uiState.value.copy(
                    stopDetails = details,
                    isDeparturesLoading = false,
                    departuresError = null,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeparturesLoading = false,
                    departuresError = "Błąd pobierania danych: ${e.localizedMessage}",
                    isRefreshing = false
                )
            }
        }
    }

    private fun loadDeparturesForStop(stop: TransitStop) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeparturesLoading = true, departuresError = null)
            try {
                val details = repository.getLiveDepartures(stop)
                _uiState.value = _uiState.value.copy(
                    stopDetails = details,
                    isDeparturesLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeparturesLoading = false,
                    departuresError = "Nie udało się pobrać odjazdów: ${e.message}"
                )
            }
        }
    }

    private fun startAutoRefresh(stop: TransitStop) {
        stopAutoRefresh()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(15000) // Refresh every 15s
                try {
                    val details = repository.getLiveDepartures(stop)
                    _uiState.value = _uiState.value.copy(stopDetails = details)
                } catch (_: Exception) {}
            }
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun refreshUserLocation() {
        _uiState.value = _uiState.value.copy(isLocating = true, locationMessage = null)
        try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    _uiState.value = _uiState.value.copy(
                        userLat = location.latitude,
                        userLng = location.longitude,
                        isLocating = false,
                        locationMessage = "Lokalizacja zaktualizowana (${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)})"
                    )
                } else {
                    // Fallback to Poznan Center location (Rondo Kaponiera) for graceful simulator display
                    val fallbackLat = 52.4082
                    val fallbackLng = 16.9133
                    _uiState.value = _uiState.value.copy(
                        userLat = fallbackLat,
                        userLng = fallbackLng,
                        isLocating = false,
                        locationMessage = "Wykryto rejon: Poznań Centrum (Kaponiera)"
                    )
                }
            }.addOnFailureListener {
                // Fallback to Poznan Center
                _uiState.value = _uiState.value.copy(
                    userLat = 52.4082,
                    userLng = 16.9133,
                    isLocating = false,
                    locationMessage = "Pozycja domyślna: Poznań Centrum"
                )
            }
        } catch (e: SecurityException) {
            // Permission not granted yet, set fallback default
            _uiState.value = _uiState.value.copy(
                userLat = 52.4082,
                userLng = 16.9133,
                isLocating = false,
                locationMessage = "Brak uprawnień GPS (Użyto centrum Poznania)"
            )
        }
    }

    fun openExternalUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
