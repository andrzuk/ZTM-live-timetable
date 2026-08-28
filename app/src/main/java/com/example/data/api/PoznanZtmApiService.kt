package com.example.data.api

import com.example.data.model.AlertSeverity
import com.example.data.model.LiveDeparture
import com.example.data.model.StopAlert
import com.example.data.model.VehicleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ApiLiveDeparturesResponse(
    val departures: List<LiveDeparture>,
    val alerts: List<StopAlert>,
    val bollardName: String? = null
)

class PoznanZtmApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchLiveDeparturesForStop(
        stopSymbol: String,
        stopName: String,
        stopCode: String = "",
        hasPst: Boolean = false
    ): Result<ApiLiveDeparturesResponse> = withContext(Dispatchers.IO) {
        // 1. Try official PEKA Virtual Monitor API (Poznań public transit real-time system)
        val pekaResult = fetchFromPekaVm(stopSymbol, stopName, stopCode, hasPst)
        if (pekaResult.isSuccess && pekaResult.getOrNull()?.departures?.isNotEmpty() == true) {
            return@withContext pekaResult
        }

        // 2. Try Poznań Open Data (Plan Poznania map_service) as secondary fallback
        val openDataResult = fetchFromPoznanOpenData(stopSymbol, stopName, stopCode, hasPst)
        if (openDataResult.isSuccess && openDataResult.getOrNull()?.departures?.isNotEmpty() == true) {
            return@withContext openDataResult
        }

        // Return PEKA result if succeeded (even if empty at night) or the openData result
        if (pekaResult.isSuccess) {
            pekaResult
        } else {
            openDataResult
        }
    }

    private fun fetchFromPekaVm(
        stopSymbol: String,
        stopName: String,
        stopCode: String,
        hasPst: Boolean
    ): Result<ApiLiveDeparturesResponse> {
        val urls = listOf(
            "https://www.peka.poznan.pl/vm/method.vm",
            "http://www.peka.poznan.pl/vm/method.vm"
        )
        var lastError: Exception? = null

        for (url in urls) {
            try {
                val formBody = FormBody.Builder()
                    .add("method", "getTimes")
                    .add("p0", "{\"symbol\":\"$stopSymbol\"}")
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .post(formBody)
                    .header("User-Agent", "PoznanTransitLive/1.0 (Android; OpenData)")
                    .header("Accept", "application/json, text/plain, */*")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastError = Exception("PEKA HTTP error: ${response.code}")
                        return@use
                    }
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.isBlank() || !bodyString.trimStart().startsWith("{")) {
                        lastError = Exception("Invalid JSON from PEKA")
                        return@use
                    }

                    val json = JSONObject(bodyString)
                    val success = json.optBoolean("success", true)
                    if (!success) {
                        lastError = Exception("PEKA returned success=false")
                        return@use
                    }

                    val resultObj = json.optJSONObject("result")
                    val timesArray = resultObj?.optJSONArray("times")
                    val bollardObj = resultObj?.optJSONObject("bollard")
                    val bollardName = bollardObj?.optString("name")

                    val departures = mutableListOf<LiveDeparture>()
                    val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val timeDisplayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    if (timesArray != null) {
                        for (i in 0 until timesArray.length()) {
                            val item = timesArray.optJSONObject(i) ?: continue
                            val line = item.optString("line", "").trim()
                            if (line.isBlank()) continue

                            val direction = item.optString("direction", "Kierunek").trim()
                            val isRealTime = item.optBoolean("realTime", false)
                            val minutes = item.optInt("minutes", 0)
                            val rawDeparture = item.optString("departure", "")
                            val onTime = item.optBoolean("onTime", true)

                            var timeStr = ""
                            if (rawDeparture.isNotBlank()) {
                                try {
                                    val cleanIso = if (rawDeparture.contains("+")) {
                                        rawDeparture.substringBefore("+")
                                    } else if (rawDeparture.contains("Z")) {
                                        rawDeparture.substringBefore("Z")
                                    } else {
                                        rawDeparture
                                    }
                                    val parsedDate = isoParser.parse(cleanIso)
                                    if (parsedDate != null) {
                                        timeStr = timeDisplayFormat.format(parsedDate)
                                    }
                                } catch (_: Exception) {
                                    timeStr = if (rawDeparture.length >= 5 && rawDeparture.contains(":")) {
                                        rawDeparture.substring(rawDeparture.indexOf(":") - 2, rawDeparture.indexOf(":") + 3)
                                    } else {
                                        rawDeparture
                                    }
                                }
                            }
                            if (timeStr.isBlank()) {
                                val cal = Calendar.getInstance().apply {
                                    add(Calendar.MINUTE, minutes)
                                }
                                timeStr = timeDisplayFormat.format(cal.time)
                            }

                            val vehicleType = getVehicleTypeForLine(line)
                            val delay = if (isRealTime && !onTime) 1 else 0
                            val platform = if (hasPst) "Peron PST" else if (stopCode.isNotBlank()) "Słupek $stopCode" else "Przystanek"
                            val vehicleModel = when (vehicleType) {
                                VehicleType.TRAM -> "Pojazd tramwajowy MPK Poznań"
                                VehicleType.BUS -> "Autobus miejski MPK Poznań"
                                VehicleType.NIGHT_BUS -> "Autobus nocny MPK Poznań"
                                VehicleType.SUBURBAN -> "Autobus podmiejski ZTM Poznań"
                            }

                            departures.add(
                                LiveDeparture(
                                    id = "peka_${stopSymbol}_${line}_${i}_${minutes}",
                                    line = line,
                                    direction = direction,
                                    departureTime = timeStr,
                                    minutesLeft = maxOf(0, minutes),
                                    isRealtime = isRealTime,
                                    delayMinutes = delay,
                                    platform = platform,
                                    vehicleType = vehicleType,
                                    isLowFloor = true,
                                    hasAirConditioning = true,
                                    vehicleModel = vehicleModel,
                                    stopsRemaining = maxOf(1, minutes / 2)
                                )
                            )
                        }
                    }

                    // Alerts and announcements
                    val alerts = mutableListOf<StopAlert>()
                    val messagesArray = resultObj?.optJSONArray("messages")
                    if (messagesArray != null) {
                        for (j in 0 until messagesArray.length()) {
                            val msg = messagesArray.optString(j)
                            if (!msg.isNullOrBlank()) {
                                alerts.add(
                                    StopAlert(
                                        id = "peka_msg_$j",
                                        title = "Komunikat ZTM Poznań",
                                        message = msg,
                                        severity = AlertSeverity.INFO
                                    )
                                )
                            }
                        }
                    }

                    return Result.success(
                        ApiLiveDeparturesResponse(
                            departures = departures.sortedBy { it.minutesLeft },
                            alerts = alerts,
                            bollardName = bollardName
                        )
                    )
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        return Result.failure(lastError ?: Exception("Nie udało się połączyć z API PEKA"))
    }

    private fun fetchFromPoznanOpenData(
        stopSymbol: String,
        stopName: String,
        stopCode: String,
        hasPst: Boolean
    ): Result<ApiLiveDeparturesResponse> {
        return try {
            val url = "https://www.poznan.pl/mim/plan/map_service.html?mtype=pub_transport&co=stoptrip&stop_id=$stopSymbol"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PoznanTransitLive/1.0 (Android; OpenData)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Błąd HTTP OpenData: ${response.code}"))
                }
                val bodyString = response.body?.string() ?: ""
                val departures = mutableListOf<LiveDeparture>()
                if (bodyString.isNotBlank() && bodyString.trimStart().startsWith("{")) {
                    val json = JSONObject(bodyString)
                    val features = json.optJSONArray("features")
                    if (features != null) {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        for (i in 0 until features.length()) {
                            val feature = features.optJSONObject(i) ?: continue
                            val props = feature.optJSONObject("properties") ?: continue
                            val line = props.optString("line", "").trim()
                            val direction = props.optString("direction", props.optString("headsign", "Kierunek")).trim()
                            val minutes = props.optInt("minutes", props.optInt("time", i * 3))
                            val isRealtime = props.optBoolean("realtime", props.optBoolean("real_time", true))

                            if (line.isNotBlank()) {
                                val cal = Calendar.getInstance().apply {
                                    add(Calendar.MINUTE, minutes)
                                }
                                val vehicleType = getVehicleTypeForLine(line)
                                departures.add(
                                    LiveDeparture(
                                        id = "opendata_${stopSymbol}_${line}_$i",
                                        line = line,
                                        direction = direction,
                                        departureTime = timeFormat.format(cal.time),
                                        minutesLeft = maxOf(0, minutes),
                                        isRealtime = isRealtime,
                                        delayMinutes = 0,
                                        platform = if (hasPst) "Peron PST" else "Słupek $stopCode",
                                        vehicleType = vehicleType,
                                        isLowFloor = true,
                                        hasAirConditioning = true,
                                        vehicleModel = if (vehicleType == VehicleType.TRAM) "Tramwaj MPK" else "Autobus MPK",
                                        stopsRemaining = maxOf(1, minutes / 2)
                                    )
                                )
                            }
                        }
                    }
                }
                Result.success(
                    ApiLiveDeparturesResponse(
                        departures = departures.sortedBy { it.minutesLeft },
                        alerts = emptyList()
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
}
