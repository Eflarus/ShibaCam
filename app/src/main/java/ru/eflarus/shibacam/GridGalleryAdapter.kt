package ru.eflarus.shibacam

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.eflarus.shibacam.databinding.GalleryCardBinding
import ru.eflarus.shibacam.fragments.GalleryFragmentDirections
import java.io.File


class GridGalleryAdapter(private val files: Array<File>) :
    RecyclerView.Adapter<GridGalleryAdapter.ViewHolder>() {


    class ViewHolder(private val binding: GalleryCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(file: File) {
            Glide.with(binding.root)
                .load(file)
                .placeholder(R.drawable.ic_launcher_background)
                .into(binding.galleryCardImg)
            if (file.absolutePath.substringAfterLast('.', "") == "mp4") {
                binding.isVideoCard.visibility = View.VISIBLE
            }
        }

    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(
            GalleryCardBinding.inflate(
                layoutInflater,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        val filePath = file.absolutePath
        holder.bind(file)
        holder.itemView.setOnClickListener {
            holder.itemView.findNavController().navigate(
                GalleryFragmentDirections.actionGalleryFragmentToFullScreenDialogFragment(
                    filePath
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }
}