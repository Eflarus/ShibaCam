package ru.eflarus.shibacam.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import ru.eflarus.shibacam.R


class FullScreenMediaFragment : Fragment() {

    private val args: FullScreenMediaFragmentArgs by navArgs()
    private lateinit var backButton: ImageButton
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("ho", args.mediaPath)
        val view: View = if (args.mediaPath.substringAfterLast('.', "") == "mp4") {
            inflater.inflate(R.layout.media_fullscreen_video, container, false)
        } else {
            inflater.inflate(R.layout.media_fullscreen_photo, container, false)
        }

        if (args.mediaPath.substringAfterLast('.', "") == "mp4") {
            val videoView: VideoView = view.findViewById(R.id.fullscreen_video)
            val mediaController = MediaController(activity)
            backButton = view.findViewById(R.id.back_button)
            videoView.setMediaController(mediaController)
            videoView.setVideoURI(Uri.parse(args.mediaPath))
            videoView.requestFocus()
            videoView.start()
        } else {
            backButton = view.findViewById(R.id.back_button)
            val imageView: ImageView = view.findViewById(R.id.fullscreen_image)
            Glide.with(this).load(args.mediaPath).into(imageView)
        }

        backButton.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigateUp()
        }
        return view
    }

}
