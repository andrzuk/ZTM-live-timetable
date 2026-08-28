package com.example.data.repository

import android.location.Location
import com.example.data.api.PoznanZtmApiClient
import com.example.data.datasource.PoznanGtfsData
import com.example.data.local.FavoriteStopDao
import com.example.data.local.FavoriteStopEntity
import com.example.data.model.AlertSeverity
import com.example.data.model.LiveDeparture
import com.example.data.model.StopAlert
import com.example.data.model.StopDataSource
import com.example.data.model.StopDetails
import com.example.data.model.TransitStop
import com.example.data.model.VehicleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

class PoznanTransitRepository(
    private val favoriteDao: FavoriteStopDao,
    private val apiClient: PoznanZtmApiClient = PoznanZtmApiClient()
) {

    val favoriteStopsFlow: Flow<List<FavoriteStopEntity>> = favoriteDao.getAllFavorites()

    fun getAllStops(): List<TransitStop> = PoznanGtfsData.STOPS

    suspend fun isStopFavorite(stopId: String): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavoriteDirect(stopId)
    }

    suspend fun toggleFavorite(stop: TransitStop) = withContext(Dispatchers.IO) {
        if (favoriteDao.isFavoriteDirect(stop.id)) {
            favoriteDao.deleteFavoriteById(stop.id)
        } else {
            favoriteDao.insertFavorite(
                FavoriteStopEntity(
                    stopId = stop.id,
                    name = stop.name,
                    symbol = stop.symbol,
                    code = stop.code,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    linesCsv = stop.lines.joinToString(","),
                    isTram = stop.isTram,
                    isBus = stop.isBus,
                    hasPst = stop.hasPst,
                    zone = stop.zone,
                    description = stop.description
                )
            )
        }
    }

    suspend fun getStopsWithFavoritesAndLocation(
        userLat: Double?,
        userLng: Double?
    ): Flow<List<TransitStop>> {
        return favoriteStopsFlow.map { favList ->
            val favIds = favList.map { it.stopId }.toSet()
            PoznanGtfsData.STOPS.map { stop ->
                var dist: Float? = null
                if (userLat != null && userLng != null) {
                    val result = FloatArray(1)
                    Location.distanceBetween(
                        userLat, userLng,
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
        }
    }

    suspend fun getLiveDepartures(stop: TransitStop): StopDetails = withContext(Dispatchers.IO) {
        // 1. Fetch real-time live departures from Poznań ZTM / PEKA Virtual Monitor API
        val apiResult = apiClient.fetchLiveDeparturesForStop(
            stopSymbol = stop.symbol,
            stopName = stop.name,
            stopCode = stop.code,
            hasPst = stop.hasPst
        )

        val liveResponse = apiResult.getOrNull()
        val alerts = mutableListOf<StopAlert>()
        alerts.addAll(liveResponse?.alerts ?: emptyList())
        if (stop.hasPst && alerts.none { it.id == "alert_pst" }) {
            alerts.add(
                StopAlert(
                    id = "alert_pst",
                    title = "Trasa PST w pełnym ruchu",
                    message = "Linie 12, 14, 15, 16 kursują ze zwiększoną częstotliwością w godzinach szczytu.",
                    severity = AlertSeverity.INFO,
                    affectedLines = listOf("12", "14", "15", "16")
                )
            )
        }

        if (apiResult.isSuccess && liveResponse != null) {
            if (liveResponse.departures.isNotEmpty()) {
                return@withContext StopDetails(
                    stop = stop,
                    departures = liveResponse.departures,
                    alerts = alerts,
                    lastUpdated = System.currentTimeMillis(),
                    isOnlineData = true,
                    dataSource = StopDataSource.LIVE_API
                )
            }

            // API reached successfully but no currently scheduled departures.
            return@withContext StopDetails(
                stop = stop,
                departures = emptyList(),
                alerts = alerts,
                lastUpdated = System.currentTimeMillis(),
                isOnlineData = true,
                dataSource = StopDataSource.ONLINE_NO_DEPARTURES
            )
        }

        // Fallback to generated timetable data only when online APIs are unavailable.
        val fallbackDepartures = generateFallbackDepartures(stop)

        StopDetails(
            stop = stop,
            departures = fallbackDepartures,
            alerts = alerts,
            lastUpdated = System.currentTimeMillis(),
            isOnlineData = false,
            dataSource = StopDataSource.FALLBACK_GENERATED
        )
    }

    private fun generateFallbackDepartures(stop: TransitStop): List<LiveDeparture> {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val departures = mutableListOf<LiveDeparture>()
        val randomSeed = (stop.id.hashCode() + currentHour * 60 + currentMinute).toLong()
        val rnd = Random(randomSeed)

        // Generate dynamic departures for all lines serving this stop
        stop.lines.forEachIndexed { index, lineStr ->
            val lineInfo = getLineMetadata(lineStr, stop.name)
            val vehicleType = getVehicleTypeForLine(lineStr)

            // Calculate departures in the next 45 minutes
            val frequencyMinutes = when {
                stop.hasPst && vehicleType == VehicleType.TRAM -> 5 + (index % 3)
                vehicleType == VehicleType.TRAM -> 8 + (index % 4)
                vehicleType == VehicleType.NIGHT_BUS -> 30
                else -> 12 + (index % 5)
            }

            val offset1 = ((currentMinute * 7 + index * 9 + stop.name.length) % frequencyMinutes)
            val offset2 = offset1 + frequencyMinutes
            val offset3 = offset2 + frequencyMinutes

            listOf(offset1, offset2, offset3).filter { it in 0..50 }.forEach { minRemaining ->
                val departureCal = (now.clone() as Calendar).apply {
                    add(Calendar.MINUTE, minRemaining)
                }
                val depTimeStr = timeFormat.format(departureCal.time)
                
                // Realtime GPS delay simulation (+0 to +3 min)
                val delay = if (minRemaining <= 15 && rnd.nextBoolean()) rnd.nextInt(0, 3) else 0
                val isGpsActive = minRemaining < 35

                val vehicleModel = when (vehicleType) {
                    VehicleType.TRAM -> if (rnd.nextBoolean()) "Moderus Gamma LF 03 AC" else "Solaris Tramino S105p"
                    VehicleType.BUS -> if (rnd.nextBoolean()) "Solaris Urbino 18 Electric" else "Mercedes Citaro C2"
                    VehicleType.NIGHT_BUS -> "Solaris Urbino 12 Hybrid"
                    VehicleType.SUBURBAN -> "Solaris Urbino 12 IV"
                }
                val fleetNumber = "#${900 + (abs(lineStr.hashCode() + minRemaining) % 150)}"

                departures.add(
                    LiveDeparture(
                        id = "${stop.id}_${lineStr}_$minRemaining",
                        line = lineStr,
                        direction = lineInfo.direction,
                        departureTime = depTimeStr,
                        minutesLeft = minRemaining,
                        isRealtime = isGpsActive,
                        delayMinutes = delay,
                        platform = if (stop.hasPst) "Peron PST" else "Słupek ${stop.code}",
                        vehicleType = vehicleType,
                        isLowFloor = true,
                        hasAirConditioning = true,
                        vehicleModel = "$vehicleModel $fleetNumber",
                        stopsRemaining = maxOf(1, 10 - minRemaining)
                    )
                )
            }
        }

        return departures.sortedBy { it.minutesLeft }
    }

    private fun getVehicleTypeForLine(line: String): VehicleType {
        val num = line.toIntOrNull() ?: return VehicleType.BUS
        return when {
            num in 1..99 -> VehicleType.TRAM
            num in 200..299 -> VehicleType.NIGHT_BUS
            num in 300..999 -> VehicleType.SUBURBAN
            else -> VehicleType.BUS
        }
    }

    private data class LineMeta(val direction: String, val returnDirection: String)

    private fun getLineMetadata(line: String, stopName: String): LineMeta {
        return when (line) {
            "1" -> LineMeta("Franowo", "Junikowo PKM")
            "2" -> LineMeta("Dębiec PKM", "Ogrody")
            "3" -> LineMeta("Błażeja (Naramowice)", "Unii Lubelskiej")
            "5" -> LineMeta("Górczyn PKM", "Unii Lubelskiej")
            "6" -> LineMeta("Miłostowo", "Junikowo PKM")
            "7" -> LineMeta("Połabska", "Ogrody")
            "8" -> LineMeta("Miłostowo", "Górczyn PKM")
            "9" -> LineMeta("Dębiec PKM", "Piątkowska")
            "10" -> LineMeta("Błażeja (Naramowice)", "Dębiec PKM")
            "11" -> LineMeta("Piątkowska", "Unii Lubelskiej")
            "12" -> LineMeta("Os. Sobieskiego (PST)", "Starołęka PKM")
            "13" -> LineMeta("Starołęka PKM", "Junikowo PKM")
            "14" -> LineMeta("Os. Sobieskiego (PST)", "Górczyn PKM")
            "15" -> LineMeta("Os. Sobieskiego (PST)", "Junikowo PKM")
            "16" -> LineMeta("Os. Sobieskiego (PST)", "Franowo")
            "17" -> LineMeta("Górczyn PKM", "Starołęka PKM")
            "18" -> LineMeta("Franowo", "Ogrody")
            "99" -> LineMeta("Plac Wielkopolski", "Górczyn PKM")
            "148" -> LineMeta("Port Lotniczy Ławica", "Rondo Kaponiera")
            "159" -> LineMeta("Port Lotniczy Ławica", "Poznań Główny")
            "163" -> LineMeta("Górczyn PKM", "Garbary PKM")
            "164" -> LineMeta("Kacza", "Puszkina")
            "168" -> LineMeta("Poznań Główny", "Krzyżowniki")
            "169" -> LineMeta("Os. Sobieskiego", "Os. Kopernika")
            "171" -> LineMeta("Os. Wichrowe Wzgórze", "Os. Dębina")
            "174" -> LineMeta("Os. Sobieskiego", "Unii Lubelskiej")
            "177" -> LineMeta("Rondo Kaponiera", "Junikowo PKM")
            "182" -> LineMeta("Ogrody", "Górczyn PKM")
            "184" -> LineMeta("Rondo Rataje", "Termy Maltańskie")
            "185" -> LineMeta("Os. Sobieskiego", "Rondo Śródka")
            "190" -> LineMeta("Os. Sobieskiego", "Rondo Rataje")
            "191" -> LineMeta("Os. Sobieskiego", "Os. Kopernika")
            "193" -> LineMeta("Górczyn PKM", "Os. Sobieskiego")
            "211" -> LineMeta("Garbary PKM", "Starołęka PKM")
            "214" -> LineMeta("Radojewo", "Junikowo PKM")
            "215" -> LineMeta("Os. Sobieskiego", "Os. Dębina / Luboń")
            "218" -> LineMeta("Os. Sobieskiego (PST)", "Franowo")
            "222" -> LineMeta("Port Lotniczy Ławica", "Mogileńska")
            else -> LineMeta("Kierunek Centrum", "Pętla Miejska")
        }
    }
}
