package com.igrab.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DownloadRepository(context: Context) {

    private val db = AppDatabase.get(context)
    private val dao = db.downloadDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("igrab_prefs", Context.MODE_PRIVATE)

    val serverUrl: String
        get() = prefs.getString("server_url", "http://localhost:5000") ?: "http://localhost:5000"

    val client: GalleryDlClient
        get() = GalleryDlClient(serverUrl)

    // ── Room ─────────────────────────────────────────────────────────
    fun allJobs(): Flow<List<DownloadJob>> = dao.allJobs()

    suspend fun insertJob(job: DownloadJob): Long = dao.insert(job)
    suspend fun updateJob(job: DownloadJob) = dao.update(job)
    suspend fun deleteJob(job: DownloadJob) = dao.delete(job)

    // ── Fluxo completo de download ────────────────────────────────────
    suspend fun startAndPoll(
        url: String,
        cookies: String = "",
        onLog: (String) -> Unit,
        onDone: (List<FileInfo>) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val startResult = client.startDownload(url, cookies)
        if (startResult.isFailure) {
            onError(startResult.exceptionOrNull()?.message ?: "Erro ao iniciar")
            return@withContext
        }

        val jobId = startResult.getOrThrow()
        var lastLogSize = 0

        while (true) {
            delay(1200)
            val statusResult = client.getStatus(jobId)
            if (statusResult.isFailure) continue

            val status = statusResult.getOrThrow()

            // Emite novas linhas de log
            if (status.log.size > lastLogSize) {
                status.log.drop(lastLogSize).forEach { onLog(it) }
                lastLogSize = status.log.size
            }

            when (status.status) {
                "done" -> { onDone(status.files); break }
                "error" -> { onError(status.log.lastOrNull() ?: "Erro desconhecido"); break }
            }
        }
    }

    // ── Prefs ─────────────────────────────────────────────────────────
    fun saveServerUrl(url: String) {
        prefs.edit().putString("server_url", url).apply()
    }

    fun saveCookies(cookies: String) {
        prefs.edit().putString("cookies", cookies).apply()
    }

    fun getCookies(): String = prefs.getString("cookies", "") ?: ""
}
