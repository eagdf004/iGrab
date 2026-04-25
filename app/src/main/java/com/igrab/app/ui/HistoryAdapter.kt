package com.igrab.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.igrab.app.data.DownloadJob
import com.igrab.app.data.DownloadStatus
import com.igrab.app.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onClick: (DownloadJob) -> Unit
) : ListAdapter<DownloadJob, HistoryAdapter.VH>(DIFF) {

    inner class VH(val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val job = getItem(position)
        val b = holder.b
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        b.historyUrl.text = job.url.removePrefix("https://www.instagram.com/")
            .removePrefix("https://instagram.com/").take(40)
        b.historyDate.text = sdf.format(Date(job.createdAt))

        val (icon, color) = when (job.status) {
            DownloadStatus.DONE -> "✅" to 0xFF4ADE80.toInt()
            DownloadStatus.ERROR -> "❌" to 0xFFF87171.toInt()
            DownloadStatus.RUNNING -> "⏳" to 0xFFFCAF45.toInt()
            DownloadStatus.PENDING -> "⏸" to 0xFF7070A0.toInt()
        }
        b.historyStatus.text = if (job.status == DownloadStatus.DONE)
            "$icon ${job.mediaCount} arquivo(s)" else icon

        b.root.setOnClickListener { onClick(job) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<DownloadJob>() {
            override fun areItemsTheSame(a: DownloadJob, b: DownloadJob) = a.id == b.id
            override fun areContentsTheSame(a: DownloadJob, b: DownloadJob) = a == b
        }
    }
}
