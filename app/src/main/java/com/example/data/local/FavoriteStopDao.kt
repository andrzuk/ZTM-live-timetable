package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteStopDao {
    @Query("SELECT * FROM favorite_stops ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteStopEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteStopEntity)

    @Query("DELETE FROM favorite_stops WHERE stopId = :stopId")
    suspend fun deleteFavoriteById(stopId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stops WHERE stopId = :stopId)")
    fun isFavorite(stopId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stops WHERE stopId = :stopId)")
    suspend fun isFavoriteDirect(stopId: String): Boolean
}
