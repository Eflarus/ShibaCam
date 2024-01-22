package ru.eflarus.shibacam.fragments

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.concurrent.futures.await
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import ru.eflarus.shibacam.R
import ru.eflarus.shibacam.databinding.FragmentCaptureVideoBinding
import ru.eflarus.shibacam.utills.GenericListAdapter
import java.text.SimpleDateFormat
import java.util.*

class CaptureVideoFragment : Fragment() {

    private var _captureViewBinding: FragmentCaptureVideoBinding? = null
    private val captureViewBinding get() = _captureViewBinding!!
    private val captureLiveStatus = MutableLiveData<String>()


    private val cameraCapabilities = mutableListOf<CameraCapability>()

    private lateinit var videoCapture: VideoCapture<Recorder>
    private var currentRecording: Recording? = null
    private lateinit var recordingState: VideoRecordEvent

    enum class UiState {
        IDLE,
        RECORDING,
        FINALIZED
    }

    private var cameraIndex = 0
    private var qualityIndex = DEFAULT_QUALITY_IDX
    private var audioEnabled = true

    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(requireContext()) }
    private var enumerationDeferred: Deferred<Unit>? = null


    private suspend fun setupCepture() {
        val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).await()

        val cameraSelector = getCameraSelector(cameraIndex)

        val quality = cameraCapabilities[cameraIndex].qualities[qualityIndex]
        val qualitySelector = QualitySelector.from(quality)

        captureViewBinding.previewView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            val orientation = this@CaptureVideoFragment.resources.configuration.orientation
            dimensionRatio = quality.getAspectRatioString(
                quality, (orientation == Configuration.ORIENTATION_PORTRAIT)
            )
        }

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()


        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .apply {
                setSurfaceProvider(captureViewBinding.previewView.surfaceProvider)
            }

        val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner, cameraSelector, videoCapture, preview
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)

        }
        enableUI(true)
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        val name = "shiba_cam_rec_" + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                val appName = requireContext().resources.getString(R.string.app_name)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
            }
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            requireActivity().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        currentRecording = videoCapture.output.prepareRecording(requireActivity(), mediaStoreOutput)
            .apply { if (audioEnabled) withAudioEnabled() }
            .start(mainThreadExecutor, captureListener)
    }

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        if (event !is VideoRecordEvent.Status) recordingState = event

        updateUI(event)

        if (event is VideoRecordEvent.Finalize) {
            updateUI(event)
            initCameraFragment()


        }
    }

    private fun Quality.getAspectRatioString(quality: Quality, portraitMode: Boolean): String {
        val hdQualities = arrayOf(Quality.UHD, Quality.FHD, Quality.HD)
        val ratio =
            when {
                hdQualities.contains(quality) -> Pair(16, 9)
                quality == Quality.SD -> Pair(4, 3)
                else -> throw UnsupportedOperationException()
            }

        return if (portraitMode) "V,${ratio.second}:${ratio.first}"
        else "H,${ratio.first}:${ratio.second}"
    }


    private fun getCameraSelector(idx: Int): CameraSelector {
        if (cameraCapabilities.size == 0) {
            Log.i(TAG, "Error: This device does not have any camera, bailing out")
            requireActivity().finish()
        }
        return (cameraCapabilities[idx % cameraCapabilities.size].camSelector)
    }

    data class CameraCapability(val camSelector: CameraSelector, val qualities: List<Quality>)

    init {
        enumerationDeferred = lifecycleScope.async {
            whenCreated {
                val provider = ProcessCameraProvider.getInstance(requireContext()).await()

                provider.unbindAll()
                for (camSelector in arrayOf(
                    CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA
                )) {
                    try {
                        if (provider.hasCamera(camSelector)) {
                            val camera = provider.bindToLifecycle(requireActivity(), camSelector)

                            QualitySelector.getSupportedQualities(camera.cameraInfo)
                                .filter { quality ->
                                    listOf(
                                        Quality.UHD,
                                        Quality.FHD,
                                        Quality.HD,
                                        Quality.SD
                                    ).contains(quality)
                                }.also {
                                    cameraCapabilities.add(CameraCapability(camSelector, it))
                                }
                        }
                    } catch (exc: java.lang.Exception) {
                        Log.e(TAG, "Camera Face $camSelector is not supported")
                    }
                }
            }
        }
    }


    private fun initCameraFragment() {
        initializeUI()
        viewLifecycleOwner.lifecycleScope.launch {
            if (enumerationDeferred != null) {
                enumerationDeferred!!.await()
                enumerationDeferred = null
            }
            initializeQualitySectionsUI()

            setupCepture()
        }
    }


    private fun initializeUI() {
        captureViewBinding.camSwitchBtn.apply {
            setOnClickListener {
                cameraIndex = (cameraIndex + 1) % cameraCapabilities.size
                qualityIndex = DEFAULT_QUALITY_IDX
                initializeQualitySectionsUI()
                enableUI(false)
                viewLifecycleOwner.lifecycleScope.launch {
                    setupCepture()
                }
            }
            isEnabled = false
        }

        captureViewBinding.switchToPhotoBtn.setOnClickListener {
            lifecycleScope.launch {
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    CaptureVideoFragmentDirections.actionCaptureFragmentToCapturePhotoFragment()
                )
            }
        }
        captureViewBinding.galleryBtn.setOnClickListener {
            lifecycleScope.launch {
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    CaptureVideoFragmentDirections.actionCaptureFragmentToGalleryFragment()
                )
            }
        }


        captureViewBinding.recordButton.apply {
            setOnClickListener {
                if (!this@CaptureVideoFragment::recordingState.isInitialized || recordingState is VideoRecordEvent.Finalize) {
                    enableUI(false)
                    startRecording()
                } else {
                    when (recordingState) {
                        is VideoRecordEvent.Start -> {
                            currentRecording?.pause()
                            captureViewBinding.stopButton.visibility = View.VISIBLE
                        }

                        is VideoRecordEvent.Pause -> currentRecording?.resume()
                        is VideoRecordEvent.Resume -> currentRecording?.pause()
                        else -> throw IllegalStateException("recordingState in unknown state")
                    }
                }
            }
            isEnabled = false
        }

        captureViewBinding.stopButton.apply {
            setOnClickListener {
                captureViewBinding.stopButton.visibility = View.INVISIBLE
                if (currentRecording == null || recordingState is VideoRecordEvent.Finalize) {
                    return@setOnClickListener
                }

                val recording = currentRecording
                if (recording != null) {
                    recording.stop()
                    currentRecording = null
                }
                captureViewBinding.recordButton.setImageResource(R.drawable.ic_start)
            }
            visibility = View.INVISIBLE
            isEnabled = false
        }

        captureLiveStatus.observe(viewLifecycleOwner) {
            captureViewBinding.captureStatus.apply {
                post { text = it }
            }
        }
        captureLiveStatus.value =""
//        captureLiveStatus.value = getString(R.string.Idle)
    }


    private fun updateUI(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Status -> {}

            is VideoRecordEvent.Start -> {
                showUI(UiState.RECORDING)
            }

            is VideoRecordEvent.Finalize -> {
                showUI(UiState.FINALIZED)
            }

            is VideoRecordEvent.Pause -> {
                captureViewBinding.recordButton.setImageResource(R.drawable.ic_resume)
            }

            is VideoRecordEvent.Resume -> {
                captureViewBinding.recordButton.setImageResource(R.drawable.ic_pause)
            }
        }

        val stats = event.recordingStats
        val size = stats.numBytesRecorded / 1000_000
        val time = java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(stats.recordedDurationNanos)
        var text = "$time s / $size Mb"
        if (event is VideoRecordEvent.Finalize) text =
            "${text}\nFile saved!"

        captureLiveStatus.value = text
    }

    private fun Quality.getNameString(): String {
        return when (this) {
            Quality.UHD -> "2K"
            Quality.FHD -> "FHD"
            Quality.HD -> "HD"
            Quality.SD -> "SD"
            else -> throw IllegalArgumentException("Quality $this is NOT supported")
        }
    }


    private fun enableUI(enable: Boolean) {
        arrayOf(
            captureViewBinding.camSwitchBtn,
            captureViewBinding.recordButton,
            captureViewBinding.stopButton,
            captureViewBinding.qualitySelection,
            captureViewBinding.switchToPhotoBtn,
            captureViewBinding.galleryBtn
        ).forEach {
            it.isEnabled = enable
        }
        if (cameraCapabilities.size <= 1) {
            captureViewBinding.camSwitchBtn.isEnabled = false
        }
        if (cameraCapabilities[cameraIndex].qualities.size <= 1) {
            captureViewBinding.qualitySelection.apply { isEnabled = false }
        }
    }


    private fun showUI(state: UiState) {
        captureViewBinding.let {
            when (state) {
                UiState.IDLE -> {
                    it.recordButton.setImageResource(R.drawable.ic_start)
                    it.stopButton.visibility = View.INVISIBLE

                    it.camSwitchBtn.visibility = View.VISIBLE
                    it.qualitySelection.visibility = View.VISIBLE
                    it.switchToPhotoBtn.visibility = View.VISIBLE
                    it.galleryBtn.visibility = View.VISIBLE
                }

                UiState.RECORDING -> {
                    it.camSwitchBtn.visibility = View.INVISIBLE
                    it.qualitySelection.visibility = View.INVISIBLE
                    it.switchToPhotoBtn.visibility = View.INVISIBLE
                    it.galleryBtn.visibility = View.INVISIBLE


                    it.recordButton.setImageResource(R.drawable.ic_pause)
                    it.recordButton.isEnabled = true
                    it.stopButton.visibility = View.VISIBLE
                    it.stopButton.isEnabled = true
                }

                UiState.FINALIZED -> {
                    it.recordButton.setImageResource(R.drawable.ic_start)
                    it.stopButton.visibility = View.INVISIBLE


                    it.recordButton.setImageResource(R.drawable.ic_start)
                    it.stopButton.visibility = View.INVISIBLE

                    it.camSwitchBtn.visibility = View.VISIBLE
                    it.qualitySelection.visibility = View.VISIBLE
                    it.switchToPhotoBtn.visibility = View.VISIBLE
                    it.galleryBtn.visibility = View.VISIBLE

                }

            }
        }
    }


    private fun initializeQualitySectionsUI() {
        val selectorStrings = cameraCapabilities[cameraIndex].qualities.map {
            it.getNameString()
        }
        captureViewBinding.qualitySelection.apply {
            layoutManager = LinearLayoutManager(
                context, LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = GenericListAdapter(
                selectorStrings, itemLayoutId = R.layout.video_quality_item
            ) { holderView, qcString, position ->

                holderView.apply {
                    findViewById<TextView>(R.id.qualityTextView)?.text = qcString
                    isSelected = (position == qualityIndex)
                }

                holderView.setOnClickListener { view ->
                    if (qualityIndex == position) return@setOnClickListener

                    captureViewBinding.qualitySelection.let {
                        it.findViewHolderForAdapterPosition(qualityIndex)?.itemView?.isSelected =
                            false
                    }
                    view.isSelected = true
                    qualityIndex = position

                    enableUI(false)
                    viewLifecycleOwner.lifecycleScope.launch {
                        setupCepture()
                    }
                }
            }
            isEnabled = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _captureViewBinding = FragmentCaptureVideoBinding.inflate(inflater, container, false)
        return captureViewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCameraFragment()
    }

    override fun onDestroyView() {
        _captureViewBinding = null
        super.onDestroyView()
    }

    companion object {
        const val DEFAULT_QUALITY_IDX = 0
        val TAG: String = CaptureVideoFragment::class.java.simpleName
    }
}