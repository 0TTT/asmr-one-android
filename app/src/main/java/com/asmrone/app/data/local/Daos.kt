package com.asmrone.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_works ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorite_works WHERE workId = :workId LIMIT 1")
    suspend fun getById(workId: Int): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Delete
    suspend fun delete(entity: FavoriteEntity)

    @Query("DELETE FROM favorite_works WHERE workId = :workId")
    suspend fun deleteById(workId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_works WHERE workId = :workId)")
    fun observeIsFavorite(workId: Int): Flow<Boolean>
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM play_history ORDER BY listenedAt DESC LIMIT 200")
    fun observeAll(): Flow<List<PlayHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlayHistoryEntity)

    @Query("DELETE FROM play_history")
    suspend fun clearAll()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads")
    suspend fun getAll(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE trackUrl = :trackUrl LIMIT 1")
    suspend fun getByUrl(trackUrl: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE trackUrl = :trackUrl")
    suspend fun deleteByUrl(trackUrl: String)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
