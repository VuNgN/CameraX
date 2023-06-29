package com.vungn.camerax.util

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.media.AudioManager
import android.media.MediaActionSound
import android.util.Log
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.TorchState
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.vungn.camerax.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SuppressLint("UnsafeOptInUsageError")
class CameraXHelper {
    private val stopWatch by lazy { StopWatch() }

    private lateinit var context: Context
    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var cameraProvider: CameraProvider
    private var _camera: Camera? = null
    private lateinit var _imageCapture: ImageCapture
    private lateinit var _videoCapture: VideoCapture<Recorder>
    private var _recording: Recording? = null
    private val _useCase: MutableStateFlow<UseCase> = MutableStateFlow(UseCase.PHOTO)
    private val _message: MutableStateFlow<String?> = MutableStateFlow(null)
    private val _cameraCapturing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _videoPause: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _videoTimer: MutableStateFlow<String> = MutableStateFlow("00:00")
    private val _torchState: MutableStateFlow<Int> = MutableStateFlow(TorchState.OFF)
    private val _meteringPoint: MutableStateFlow<MeteringPoint> =
        MutableStateFlow(MeteringPoint(0f, 0f, MeteringPointFactory.getDefaultPointSize()))
    private val _aspectRatio: MutableStateFlow<Int> = MutableStateFlow(AspectRatio.RATIO_4_3)
    private val _len: MutableStateFlow<Int> = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    private val _barcodes: MutableStateFlow<List<Barcode>> = MutableStateFlow(emptyList())
    private val _filteredQualities: MutableStateFlow<List<Quality>> = MutableStateFlow(emptyList())
    private val _videoQuality: MutableStateFlow<Quality> = MutableStateFlow(Quality.SD)
    private val _rotation: MutableStateFlow<Int> = MutableStateFlow(Surface.ROTATION_0)

    val camera: Camera?
        get() = _camera
    val useCase: MutableStateFlow<UseCase>
        get() = _useCase
    val message: MutableStateFlow<String?>
        get() = _message
    val cameraCapturing: MutableStateFlow<Boolean>
        get() = _cameraCapturing
    val videoPause: MutableStateFlow<Boolean>
        get() = _videoPause
    val videoTimer: MutableStateFlow<String>
        get() = _videoTimer
    val torchState: MutableStateFlow<Int>
        get() = _torchState
    val meteringPoint: MutableStateFlow<MeteringPoint>
        get() = _meteringPoint
    val aspectRatio: MutableStateFlow<Int>
        get() = _aspectRatio
    val len: MutableStateFlow<Int>
        get() = _len
    val barcodes: MutableStateFlow<List<Barcode>>
        get() = _barcodes
    val filteredQualities: MutableStateFlow<List<Quality>>
        get() = _filteredQualities
    val videoQuality: MutableStateFlow<Quality>
        get() = _videoQuality
    val rotation: MutableStateFlow<Int>
        get() = _rotation

    fun bindPreview(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView,
    ) {
        this.context = context
        this.lifecycleOwner = lifecycleOwner
        this.cameraProvider = cameraProvider
        this._filteredQualities.let {
            val cameraInfo = cameraProvider.availableCameraInfos.filter {
                Camera2CameraInfo.from(it)
                    .getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK
            }
            val supportedQualities = QualitySelector.getSupportedQualities(cameraInfo[0])
            supportedQualities.forEach {
                Log.d(TAG, "Supported quality: $it")
            }
            lifecycleOwner.lifecycleScope.launch {
                it.emit(listOf(
                    Quality.UHD, Quality.FHD, Quality.HD, Quality.SD
                ).filter { supportedQualities.contains(it) })
            }
        }

        cameraProvider.unbindAll()

        // Preview
        val preview = androidx.camera.core.Preview.Builder()
            .setTargetAspectRatio(if (_useCase.value == UseCase.PHOTO) _aspectRatio.value else AspectRatio.RATIO_4_3)
            .build()
        val cameraSelector: CameraSelector =
            CameraSelector.Builder().requireLensFacing(_len.value).build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        // Image capture
        _imageCapture = ImageCapture.Builder().setTargetRotation(previewView.display.rotation)
            .setTargetAspectRatio(_aspectRatio.value).build()
        _imageCapture.enableOrientation(context, this::changeRotation)

        // Image analysis
        val imageAnalysis =
            androidx.camera.core.ImageAnalysis.Builder().setTargetAspectRatio(_aspectRatio.value)
                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
        imageAnalysis.setupAnalyzer {
            lifecycleOwner.lifecycleScope.launch {
                _barcodes.emit(it)
            }
        }

        // Video capture
        val qualitySelector = QualitySelector.from(_videoQuality.value)
        val recorder = Recorder.Builder().setExecutor(Executors.newSingleThreadExecutor())
            .setQualitySelector(qualitySelector).build()
        _videoCapture = VideoCapture.withOutput(recorder)
        _videoCapture.enableOrientation(context, this::changeRotation)

        // Use case
        val useCaseGroup = UseCaseGroup.Builder().addUseCase(preview).addUseCase(
            when (_useCase.value) {
                UseCase.PHOTO -> _imageCapture
                UseCase.VIDEO -> _videoCapture
            }
        )
        if (_useCase.value == UseCase.PHOTO) {
            useCaseGroup.addUseCase(imageAnalysis).build()
        }

        // Lifecycle binding
        _camera =
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup.build())

        _camera?.cameraInfo?.torchState?.observe(lifecycleOwner) {
            lifecycleOwner.lifecycleScope.launch {
                _torchState.emit(it)
            }
        }
        configPreviewView(previewView)
    }

    fun imageCapturing() {
        _cameraCapturing.value = true
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                val sound = MediaActionSound()
                sound.play(MediaActionSound.SHUTTER_CLICK)
            }
        }
        val outputFileOptions = getOutputFileOption()
        _imageCapture.takePicture(
            outputFileOptions, Executors.newSingleThreadExecutor(), onImageSavedCallback
        )
    }

    @SuppressLint("MissingPermission")
    fun videoCapturing() {
        _cameraCapturing.value = true
        _videoPause.value = false
        val outputFileOptions = getVideoOutputFileOption()
        _recording =
            _videoCapture.output.prepareRecording(context, outputFileOptions).withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context), onVideoSaveCallback)
    }

    fun stopVideoCapturing() {
        _cameraCapturing.value = false
        _recording?.stop()
    }

    fun pauseVideoCapturing() {
        _videoPause.value = true
        _recording?.pause()
    }

    fun resumeVideoCapturing() {
        _videoPause.value = false
        _recording?.resume()
    }

    fun changeAspectRatio(newRatio: Int) {
        lifecycleOwner.lifecycleScope.launch {
            _aspectRatio.emit(newRatio)
        }
    }

    fun switchCamera(newLen: Int) {
        lifecycleOwner.lifecycleScope.launch {
            _len.emit(newLen)
        }
    }

    fun changeUseCase(newUseCase: UseCase) {
        lifecycleOwner.lifecycleScope.launch {
            _useCase.emit(newUseCase)
        }
    }

    fun changeVideoQuality(newQuality: Quality) {
        lifecycleOwner.lifecycleScope.launch {
            _videoQuality.emit(newQuality)
        }
    }

    private fun configPreviewView(previewView: PreviewView) {
        val meteringSize = MeteringPointFactory.getDefaultPointSize()
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
        previewView.setOnTouchListener { v, event ->
            _meteringPoint.value = MeteringPoint(event.x, event.y, meteringSize * 500f)
            val meteringPoint = previewView.meteringPointFactory.createPoint(
                event.x, event.y, meteringSize
            )
            val action = FocusMeteringAction.Builder(meteringPoint)
                .setAutoCancelDuration(3, TimeUnit.SECONDS).build()
            val result = _camera?.cameraControl?.startFocusAndMetering(action)
            result?.addListener({}, ContextCompat.getMainExecutor(context))
            v.performClick()
            true
        }
    }

    fun changeTorchState() {
        if (_camera?.cameraInfo?.hasFlashUnit() == true) {
            _camera?.cameraControl?.enableTorch(_camera?.cameraInfo?.torchState?.value != TorchState.ON)
        }
    }

    private val onImageSavedCallback = object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val savedUri = outputFileResults.savedUri
            Log.d(MainActivity.TAG, "onImageSaved in ${savedUri.toString()}")
            lifecycleOwner.lifecycleScope.launch {
                _cameraCapturing.emit(false)
            }
        }

        override fun onError(exception: ImageCaptureException) {
            Log.e(MainActivity.TAG, "onError: Save failure", exception)
            lifecycleOwner.lifecycleScope.launch {
                _cameraCapturing.emit(false)
                _message.emit("Save failure")
            }
        }
    }

    private val onVideoSaveCallback = Consumer<VideoRecordEvent> {
        when (it) {
            is VideoRecordEvent.Start -> {
                Log.d(MainActivity.TAG, "onVideoSaved: Start")
                stopWatch.reset()
                stopWatch.start()
            }

            is VideoRecordEvent.Pause -> {
                Log.d(MainActivity.TAG, "onVideoSaved: Pause")
                stopWatch.pause()
            }

            is VideoRecordEvent.Finalize -> {
                Log.d(MainActivity.TAG, "onVideoSaved: Finalize")
                stopWatch.stop()
                stopWatch.reset()
                lifecycleOwner.lifecycleScope.launch {
                    _videoTimer.emit(stopWatch.getTime())
                }
            }

            is VideoRecordEvent.Resume -> {
                Log.d(MainActivity.TAG, "onVideoSaved: Resume")
                stopWatch.resume()
            }

            is VideoRecordEvent.Status -> {
                Log.d(MainActivity.TAG, "onVideoSaved: Status")
                Log.d(TAG, "timer: ${stopWatch.getTime()}")
                lifecycleOwner.lifecycleScope.launch {
                    _videoTimer.emit(stopWatch.getTime())
                }
            }
        }
    }

    private fun getOutputFileOption(): ImageCapture.OutputFileOptions {
        val fileName = SimpleDateFormat(
            FILENAME_FORMAT, Locale.US
        ).getFileName(PHOTO_EXTENSION)
        val outputStream = context.contentResolver.getImageOutputStream(fileName = fileName)
        return ImageCapture.OutputFileOptions.Builder(outputStream).build()
    }

    private fun getVideoOutputFileOption(): MediaStoreOutputOptions {
        val fileName = SimpleDateFormat(
            FILENAME_FORMAT, Locale.US
        ).getFileName(VIDEO_EXTENSION)
        return context.contentResolver.getVideoOutputOptions(fileName = fileName)
    }

    private fun changeRotation(newRotation: Int) {
        lifecycleOwner.lifecycleScope.launch {
            _rotation.emit(newRotation)
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: CameraXHelper? = null
        fun getInstance(): CameraXHelper {
            if (INSTANCE == null) {
                INSTANCE = CameraXHelper()
            }
            return INSTANCE!!
        }

        private const val TAG = "CameraXHelper"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpeg"
        private const val VIDEO_EXTENSION = ".mp4"
    }

    enum class UseCase {
        PHOTO, VIDEO
    }
}