package ru.eflarus.shibacam.fragments

import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import ru.eflarus.shibacam.R
import ru.eflarus.shibacam.databinding.FragmentCapturePhotoBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CapturePhotoFragment : Fragment() {

    private lateinit var binding: FragmentCapturePhotoBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imageCaptureExecutor: ExecutorService

    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCapturePhotoBinding.inflate(layoutInflater)


        cameraProviderFuture = ProcessCameraProvider.getInstance(binding.root.context)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        imageCaptureExecutor = Executors.newSingleThreadExecutor()
        startCamera()

        binding.imgCaptureBtn.setOnClickListener {
            takePhoto()
            animateFlash()
        }

        binding.switchToVideoBtn.setOnClickListener {

            Log.d("TAG", "modeSwitchBtn")
            lifecycleScope.launch {
                navController.navigate(
                    CapturePhotoFragmentDirections.actionCapturePhotoToCaptureVideoFragment()
                )
            }
        }
        binding.switchBtn.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }
        binding.galleryBtn.setOnClickListener {
            lifecycleScope.launch {
                navController.navigate(
                    CapturePhotoFragmentDirections.actionCapturePhotoToGalleryFragment()
                )
            }
        }

        return binding.root
    }

    private fun startCamera() {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.preview.surfaceProvider)
        }
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            if (imageCapture == null) {
                imageCapture = ImageCapture.Builder().build()
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.d("TAG", "Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(binding.root.context))
    }

    private fun takePhoto() {
        imageCapture?.let {
            val fileName = "JPEG_${System.currentTimeMillis()}.jpg"
//            val file = File(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, fileName)
//            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
            // Create time stamped name and MediaStore entry.

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    val appName = requireContext().resources.getString(R.string.app_name)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
                }
            }

            // Create output options object which contains file + metadata
            val outputFileOptions = ImageCapture.OutputFileOptions
                .Builder(requireContext().contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues)
                .build()
            it.takePicture(outputFileOptions,
                imageCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.i("TAG", "The image has been saved in ${outputFileResults.savedUri}")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            binding.root.context, "Error taking photo", Toast.LENGTH_LONG
                        ).show()
                        Log.d("TAG", "Error taking photo:$exception")
                    }

                })
        }
    }

    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

    override fun onDestroy() {
        super.onDestroy()
        imageCaptureExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val PHOTO_TYPE = "image/jpeg"
    }
}