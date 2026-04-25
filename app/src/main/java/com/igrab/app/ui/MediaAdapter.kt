package com.igrab.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.igrab.app.data.FileInfo
import com.igrab.app.databinding.ItemMediaBinding
import java.io.File

class MediaAdapter(
    private val onClick: (FileInfo) -> Unit
) : ListAdapter<FileInfo, MediaAdapter.VH>(DIFF) {

    inner class VH(val b: ItemMediaBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.b

        if (item.type == "video") {
            b.mediaThumb.setImageResource(android.R.drawable.ic_media_play)
            b.mediaOverlay.alpha = 1f
            b.videoIcon.visibility = android.view.View.VISIBLE
        } else {
            b.videoIcon.visibility = android.view.View.GONE
            b.mediaOverlay.alpha = 0f
            Glide.with(b.mediaThumb)
                .load(File(item.path))
                .centerCrop()
                .placeholder(android.R.color.darker_gray)
                .into(b.mediaThumb)
        }

        // Tamanho formatado
        b.mediaSize.text = formatBytes(item.size)
        b.mediaName.text = item.name.take(18)

        b.root.setOnClickListener { onClick(item) }
    }

    private fun formatBytes(b: Long): String {
        return when {
            b < 1024 -> "$b B"
            b < 1_048_576 -> String.format("%.1f KB", b / 1024f)
            else -> String.format("%.1f MB", b / 1_048_576f)
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FileInfo>() {
            override fun areItemsTheSame(a: FileInfo, b: FileInfo) = a.path == b.path
            override fun areContentsTheSame(a: FileInfo, b: FileInfo) = a == b
        }
    }
}
