package com.igrab.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// ──────────────────────────────────────────────
// Download Job — representa uma URL sendo baixada
// ──────────────────────────────────────────────
@Entity(tableName = "downloads")
data class DownloadJob(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val mediaCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val outputDir: String = ""
)

enum class DownloadStatus {
    PENDING, RUNNING, DONE, ERROR
}

// ──────────────────────────────────────────────
// Media Item — arquivo baixado
// ──────────────────────────────────────────────
data class MediaItem(
    val path: String,
    val name: String,
    val type: MediaType,
    val sizeBytes: Long,
    val jobId: Long
)

enum class MediaType { IMAGE, VIDEO }

// ──────────────────────────────────────────────
// API response models (do servidor gallery-dl)
// ──────────────────────────────────────────────
data class StartResponse(val job_id: String?, val error: String?)

data class StatusResponse(
    val status: String,
    val url: String,
    val log: List<String>,
    val files: List<FileInfo>
)

data class FileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val type: String   // "image" | "video"
)

data class JobSummary(
    val id: String,
    val url: String,
    val status: String,
    val files_count: Int
)
