package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_stops")
data class FavoriteStopEntity(
    @PrimaryKey val stopId: String,
    val name: String,
    val symbol: String,
    val code: String,
    val latitude: Double,
    val longitude: Double,
    val linesCsv: String,
    val isTram: Boolean,
    val isBus: Boolean,
    val hasPst: Boolean,
    val zone: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)
