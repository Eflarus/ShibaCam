//package ru.eflarus.shibacam.fragments
//
//import ru.eflarus.shibacam.databinding.FragmentVideoViewerBinding
//
//
//import android.content.ContentResolver
//import android.database.Cursor
//import android.database.CursorIndexOutOfBoundsException
//import android.media.MediaScannerConnection
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.provider.MediaStore
//import android.provider.OpenableColumns
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.MediaController
//import androidx.navigation.fragment.navArgs
//import android.util.TypedValue
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.Navigation
//import kotlinx.coroutines.launch
//import ru.eflarus.shibacam.R
//import java.lang.RuntimeException
//
//
//class VideoViewerFragment : androidx.fragment.app.Fragment() {
//    private val args: VideoViewerFragmentArgs by navArgs()
//
//    private var _binding: FragmentVideoViewerBinding? = null
//    private val binding get() = _binding!!
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentVideoViewerBinding.inflate(inflater, container, false)
//
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//            showVideo(args.uri)
//        } else {
//            // force MediaScanner to re-scan the media file.
//            val path = getAbsolutePathFromUri(args.uri) ?: return
//            MediaScannerConnection.scanFile(
//                context, arrayOf(path), null
//            ) { _, uri ->
//                // playback video on main thread with VideoView
//                if (uri != null) {
//                    lifecycleScope.launch {
//                        showVideo(uri)
//                    }
//                }
//            }
//        }
//
//        // Handle back button press
//        binding.backButton.setOnClickListener {
//            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp()
//        }
//    }
//
//    override fun onDestroyView() {
//        _binding = null
//        super.onDestroyView()
//    }
//
//
//    private fun showVideo(uri: Uri) {
//        val fileSize = getFileSizeFromUri(uri)
//        if (fileSize == null || fileSize <= 0) {
//            Log.e("VideoViewerFragment", "Failed to get recorded file size, could not be played!")
//            return
//        }
//
//        val filePath = getAbsolutePathFromUri(uri) ?: return
//        val fileInfo = "FileSize: $fileSize\n $filePath"
//        Log.i("VideoViewerFragment", fileInfo)
//        binding.videoViewerTips.text = fileInfo
//        val mc = MediaController(requireContext())
//        binding.videoViewer.apply {
//            setVideoURI(uri)
//            setMediaController(mc)
//            requestFocus()
//        }.start()
//        mc.show(0)
//    }
//
//
//
//
//
//}