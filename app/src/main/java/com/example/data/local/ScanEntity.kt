package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val rawText: String,
    val summary: String = "",
    val language: String = "English",
    val source: String = "HANDWRITING", // HANDWRITING, PDF, CAMERA, GALLERY, SAMPLE
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val pageCount: Int = 1,
    val confidenceNote: String = "High Accuracy"
)
