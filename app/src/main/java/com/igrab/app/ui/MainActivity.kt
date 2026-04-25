package com.igrab.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.igrab.app.R
import com.igrab.app.data.DownloadJob
import com.igrab.app.data.DownloadStatus
import com.igrab.app.data.FileInfo
import com.igrab.app.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        handleSharedIntent(intent)
        vm.checkServer()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
    }

    // ── Intent de compartilhamento (share de outros apps) ────────────
    private fun handleSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val url = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            if (url.contains("instagram.com")) {
                binding.urlInput.setText(url.trim())
                Toast.makeText(this, "URL do Instagram detectada!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────
    private fun setupUI() {
        // RecyclerView de mídias baixadas
        mediaAdapter = MediaAdapter { file -> openOrShareFile(file) }
        binding.mediaGrid.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = mediaAdapter
        }

        // RecyclerView de histórico
        historyAdapter = HistoryAdapter { job -> showJobDetail(job) }
        binding.historyList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }

        // Botão baixar
        binding.btnDownload.setOnClickListener {
            val url = binding.urlInput.text.toString().trim()
            if (url.isEmpty()) {
                binding.urlInputLayout.error = "Cole uma URL do Instagram"
                return@setOnClickListener
            }
            if (!url.contains("instagram.com")) {
                binding.urlInputLayout.error = "URL inválida"
                return@setOnClickListener
            }
            binding.urlInputLayout.error = null
            val cookies = vm.getCookies()
            vm.download(url, cookies)
        }

        // Botão nova URL (após download)
        binding.btnNewDownload.setOnClickListener {
            vm.resetState()
            binding.urlInput.text?.clear()
        }

        // Botão configurações
        binding.btnSettings.setOnClickListener { showSettingsDialog() }

        // Chip de status do servidor
        binding.serverChip.setOnClickListener { vm.checkServer() }

        // Expandir/recolher log
        binding.logHeader.setOnClickListener {
            binding.logBox.isVisible = !binding.logBox.isVisible
            binding.logArrow.rotation = if (binding.logBox.isVisible) 180f else 0f
        }
    }

    // ── Observadores ─────────────────────────────────────────────────
    private fun setupObservers() {
        vm.downloadState.observe(this) { state ->
            when (state) {
                is DownloadState.Idle -> showIdle()
                is DownloadState.Running -> showRunning(state.url)
                is DownloadState.Done -> showDone(state.files)
                is DownloadState.Error -> showError(state.message)
            }
        }

        vm.logLines.observe(this) { lines ->
            binding.logBox.text = lines.joinToString("\n")
            if (lines.isNotEmpty()) {
                val scrollAmount = binding.logBox.layout?.getLineBottom(lines.size - 1) ?: 0
                binding.logScrollView.smoothScrollTo(0, scrollAmount)
            }
        }

        vm.serverOnline.observe(this) { online ->
            when (online) {
                true -> {
                    binding.serverChip.text = "Servidor online ✓"
                    binding.serverChip.chipBackgroundColor =
                        getColorStateList(R.color.chip_online)
                    binding.serverBanner.isVisible = false
                }
                false -> {
                    binding.serverChip.text = "Servidor offline"
                    binding.serverChip.chipBackgroundColor =
                        getColorStateList(R.color.chip_offline)
                    binding.serverBanner.isVisible = true
                }
                null -> {
                    binding.serverChip.text = "Verificando..."
                    binding.serverChip.chipBackgroundColor =
                        getColorStateList(R.color.chip_checking)
                }
            }
        }

        vm.jobHistory.observe(this) { jobs ->
            historyAdapter.submitList(jobs)
            binding.historySection.isVisible = jobs.isNotEmpty()
        }
    }

    // ── Estados visuais ───────────────────────────────────────────────
    private fun showIdle() {
        binding.inputCard.isVisible = true
        binding.progressCard.isVisible = false
        binding.resultCard.isVisible = false
        binding.btnDownload.isEnabled = true
        binding.btnDownload.text = "Baixar"
    }

    private fun showRunning(url: String) {
        binding.inputCard.isVisible = false
        binding.progressCard.isVisible = true
        binding.resultCard.isVisible = false
        binding.progressUrl.text = shortenUrl(url)
        binding.statusBadge.text = "⏳ Baixando..."
        binding.statusBadge.setBackgroundResource(R.drawable.badge_running)
        binding.logBox.isVisible = true
        binding.logArrow.rotation = 180f
    }

    private fun showDone(files: List<FileInfo>) {
        binding.progressCard.isVisible = true
        binding.resultCard.isVisible = true
        binding.statusBadge.text = "✅ Concluído · ${files.size} arquivo(s)"
        binding.statusBadge.setBackgroundResource(R.drawable.badge_done)
        mediaAdapter.submitList(files)
        binding.btnDownload.isEnabled = true
    }

    private fun showError(msg: String) {
        binding.progressCard.isVisible = true
        binding.statusBadge.text = "❌ Erro"
        binding.statusBadge.setBackgroundResource(R.drawable.badge_error)
        binding.btnDownload.isEnabled = true
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    // ── Abrir / compartilhar arquivo ──────────────────────────────────
    private fun openOrShareFile(file: FileInfo) {
        val f = File(file.path)
        if (!f.exists()) { Toast.makeText(this, "Arquivo não encontrado", Toast.LENGTH_SHORT).show(); return }

        val uri = FileProvider.getUriForFile(this, "$packageName.provider", f)
        val mime = if (file.type == "video") "video/*" else "image/*"

        AlertDialog.Builder(this, R.style.Theme_IGrab_Dialog)
            .setTitle(file.name)
            .setItems(arrayOf("Abrir", "Compartilhar")) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                    1 -> startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = mime
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "Compartilhar via"
                    ))
                }
            }.show()
    }

    // ── Detalhe de job histórico ──────────────────────────────────────
    private fun showJobDetail(job: DownloadJob) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = sdf.format(Date(job.createdAt))
        val status = when (job.status) {
            DownloadStatus.DONE -> "✅ Concluído · ${job.mediaCount} arquivo(s)"
            DownloadStatus.ERROR -> "❌ Erro: ${job.errorMessage}"
            DownloadStatus.RUNNING -> "⏳ Rodando"
            DownloadStatus.PENDING -> "⏸ Pendente"
        }

        AlertDialog.Builder(this, R.style.Theme_IGrab_Dialog)
            .setTitle("Download · $date")
            .setMessage("URL: ${job.url}\n\nStatus: $status")
            .setPositiveButton("OK", null)
            .setNegativeButton("Remover") { _, _ ->
                androidx.lifecycle.lifecycleScope.launchWhenStarted {
                    vm.repo.deleteJob(job)
                }
            }
            .show()
    }

    // ── Configurações ─────────────────────────────────────────────────
    private fun showSettingsDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_settings, null)
        val serverInput = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.settingsServerUrl)
        val cookiesInput = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.settingsCookies)

        serverInput.setText(vm.getServerUrl())
        cookiesInput.setText(vm.getCookies())

        AlertDialog.Builder(this, R.style.Theme_IGrab_Dialog)
            .setTitle("⚙️ Configurações")
            .setView(view)
            .setPositiveButton("Salvar") { _, _ ->
                val url = serverInput.text.toString().trim().ifEmpty { "http://localhost:5000" }
                val cookies = cookiesInput.text.toString().trim()
                vm.saveServerUrl(url)
                vm.saveCookies(cookies)
                Toast.makeText(this, "Configurações salvas!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun shortenUrl(url: String) = if (url.length > 50) url.take(47) + "..." else url
}
