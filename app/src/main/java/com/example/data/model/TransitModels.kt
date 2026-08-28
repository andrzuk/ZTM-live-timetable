package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PoznanAmber
import com.example.ui.theme.PoznanBlue
import com.example.ui.theme.PoznanGreen
import com.example.ui.theme.PoznanOrange
import com.example.ui.theme.PoznanPurple

enum class VehicleType(
    val displayName: String,
    val color: Color,
    val iconName: String
) {
    TRAM("Tramwaj", PoznanGreen, "tram"),
    BUS("Autobus", PoznanBlue, "directions_bus"),
    NIGHT_BUS("Linia nocna", PoznanPurple, "nights_stay"),
    SUBURBAN("Podmiejski", PoznanOrange, "airport_shuttle")
}

data class TransitStop(
    val id: String,
    val name: String,
    val symbol: String, // e.g. "MTEA01", "KAPO02"
    val code: String,   // e.g. "01", "02"
    val zone: String = "Strefa A (Poznań)",
    val latitude: Double,
    val longitude: Double,
    val lines: List<String>,
    val isTram: Boolean = true,
    val isBus: Boolean = false,
    val hasPst: Boolean = false,
    val description: String = "",
    val isFavorite: Boolean = false,
    val distanceMeters: Float? = null
)

data class LiveDeparture(
    val id: String,
    val line: String,
    val direction: String,
    val departureTime: String,     // e.g. "14:35"
    val minutesLeft: Int,          // e.g. 2 min (0 for ">>>" / "teraz")
    val isRealtime: Boolean = true, // GPS tracking active
    val delayMinutes: Int = 0,     // 0 = punctual, >0 = late, <0 = early
    val platform: String = "",     // e.g. "Peron 1", "Słupek 02"
    val vehicleType: VehicleType,
    val isLowFloor: Boolean = true,
    val hasAirConditioning: Boolean = true,
    val vehicleModel: String = "", // e.g. "Moderus Gamma #921", "Solaris Urbino 18 #1405"
    val stopsRemaining: Int = 0
)

data class StopAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity = AlertSeverity.INFO,
    val affectedLines: List<String> = emptyList()
)

enum class AlertSeverity {
    INFO, WARNING, CRITICAL
}

enum class StopDataSource {
    LIVE_API,
    ONLINE_NO_DEPARTURES,
    FALLBACK_GENERATED
}

data class StopDetails(
    val stop: TransitStop,
    val departures: List<LiveDeparture>,
    val alerts: List<StopAlert> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isOnlineData: Boolean = true,
    val dataSource: StopDataSource = StopDataSource.LIVE_API
)
