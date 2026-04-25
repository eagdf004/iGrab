package com.igrab.app.ui

import android.app.Application
import androidx.lifecycle.*
import com.igrab.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repo = DownloadRepository(app)

    // ── Estado da UI ─────────────────────────────────────────────────
    val downloadState = MutableLiveData<DownloadState>(DownloadState.Idle)
    val logLines = MutableLiveData<List<String>>(emptyList())
    val serverOnline = MutableLiveData<Boolean?>(null)
    val jobHistory: LiveData<List<DownloadJob>> = repo.allJobs().asLiveData()

    // ── Checa se o servidor está rodando ─────────────────────────────
    fun checkServer() = viewModelScope.launch(Dispatchers.IO) {
        serverOnline.postValue(repo.client.isServerOnline())
    }

    // ── Inicia download ───────────────────────────────────────────────
    fun download(url: String, cookies: String) {
        if (downloadState.value is DownloadState.Running) return

        viewModelScope.launch {
            val jobId = repo.insertJob(DownloadJob(url = url, status = DownloadStatus.RUNNING))
            val logs = mutableListOf<String>()
            logLines.postValue(emptyList())
            downloadState.postValue(DownloadState.Running(url))

            repo.startAndPoll(
                url = url,
                cookies = cookies,
                onLog = { line ->
                    logs.add(line)
                    logLines.postValue(logs.toList())
                },
                onDone = { files ->
                    downloadState.postValue(DownloadState.Done(files))
                    viewModelScope.launch {
                        repo.updateJob(
                            DownloadJob(
                                id = jobId, url = url,
                                status = DownloadStatus.DONE,
                                mediaCount = files.size,
                                finishedAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                onError = { msg ->
                    downloadState.postValue(DownloadState.Error(msg))
                    viewModelScope.launch {
                        repo.updateJob(
                            DownloadJob(
                                id = jobId, url = url,
                                status = DownloadStatus.ERROR,
                                errorMessage = msg,
                                finishedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            )
        }
    }

    fun resetState() = downloadState.postValue(DownloadState.Idle)
    fun saveCookies(c: String) = repo.saveCookies(c)
    fun getCookies() = repo.getCookies()
    fun saveServerUrl(u: String) { repo.saveServerUrl(u); checkServer() }
    fun getServerUrl() = repo.serverUrl
}

// ── Estados da UI ─────────────────────────────────────────────────────
sealed class DownloadState {
    object Idle : DownloadState()
    data class Running(val url: String) : DownloadState()
    data class Done(val files: List<FileInfo>) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
