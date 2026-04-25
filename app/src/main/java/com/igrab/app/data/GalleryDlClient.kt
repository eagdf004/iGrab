package com.igrab.app.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP para o servidor Flask (gallery-dl) rodando localmente.
 * Por padrão usa localhost:5000, mas o usuário pode mudar nas configurações.
 */
class GalleryDlClient(private val baseUrl: String = "http://localhost:5000") {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    // ── Inicia download ──────────────────────────────────────────────
    fun startDownload(url: String, cookies: String = ""): Result<String> {
        val body = gson.toJson(mapOf("url" to url, "cookies" to cookies))
            .toRequestBody(JSON)
        val request = Request.Builder()
            .url("$baseUrl/download")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val json = response.body?.string() ?: return Result.failure(IOException("Empty body"))
                val resp = gson.fromJson(json, StartResponse::class.java)
                if (resp.error != null) Result.failure(IOException(resp.error))
                else Result.success(resp.job_id ?: "")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Consulta status de um job ────────────────────────────────────
    fun getStatus(jobId: String): Result<StatusResponse> {
        val request = Request.Builder().url("$baseUrl/status/$jobId").get().build()
        return try {
            client.newCall(request).execute().use { response ->
                val json = response.body?.string() ?: return Result.failure(IOException("Empty body"))
                Result.success(gson.fromJson(json, StatusResponse::class.java))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Lista todos os jobs ──────────────────────────────────────────
    fun listJobs(): Result<List<JobSummary>> {
        val request = Request.Builder().url("$baseUrl/jobs").get().build()
        return try {
            client.newCall(request).execute().use { response ->
                val json = response.body?.string() ?: return Result.failure(IOException("Empty body"))
                val type = object : TypeToken<List<JobSummary>>() {}.type
                Result.success(gson.fromJson(json, type))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Verifica se o servidor está online ──────────────────────────
    fun isServerOnline(): Boolean {
        return try {
            val request = Request.Builder().url("$baseUrl/jobs").get().build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    // ── URL de um arquivo para download direto ───────────────────────
    fun fileUrl(path: String) = "$baseUrl/file/${path.trimStart('/')}"
}
