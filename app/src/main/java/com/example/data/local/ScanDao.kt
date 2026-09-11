package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scan_records WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteScans(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scan_records WHERE source = :sourceType ORDER BY timestamp DESC")
    fun getScansBySource(sourceType: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scan_records WHERE title LIKE '%' || :query || '%' OR rawText LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchScans(query: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scan_records WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: Long): ScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity): Long

    @Update
    suspend fun updateScan(scan: ScanEntity)

    @Delete
    suspend fun deleteScan(scan: ScanEntity)

    @Query("DELETE FROM scan_records WHERE id = :id")
    suspend fun deleteScanById(id: Long)

    @Query("DELETE FROM scan_records")
    suspend fun deleteAllScans()
}
