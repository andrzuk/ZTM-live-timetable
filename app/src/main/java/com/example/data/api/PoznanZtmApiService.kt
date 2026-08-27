package com.example.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PoznanZtmApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLiveDeparturesForStop(stopSymbol: String, stopName: String): Result<List<String>> {
        return try {
            // Query Poznań Open Data API / ZTM endpoint
            val url = "https://www.poznan.pl/mim/plan/map_service.html?mtype=pub_transport&co=stoptrip&stop_id=${stopSymbol}"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PoznanTransitLive/1.0 (Android; OpenData)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val results = mutableListOf<String>()
                if (bodyString.isNotBlank() && bodyString.startsWith("{")) {
                    val json = JSONObject(bodyString)
                    val features = json.optJSONArray("features")
                    if (features != null) {
                        for (i in 0 until features.length()) {
                            val feature = features.optJSONObject(i)
                            val props = feature?.optJSONObject("properties")
                            val line = props?.optString("line") ?: ""
                            if (line.isNotBlank()) {
                                results.add(line)
                            }
                        }
                    }
                }
                Result.success(results)
            } else {
                Result.failure(Exception("HTTP error ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
