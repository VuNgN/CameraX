package com.vungn.camerax.util

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.MediaActionSound
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.TorchState
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.vungn.camerax.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraXHelper {
    private lateinit var context: Context
    private lateinit var lifecycleOwner: LifecycleOwner
    private var _camera: Camera? = null
    private lateinit var _imageCapture: ImageCapture
    private val _message: MutableStateFlow<String?> = MutableStateFlow(null)
    private val _cameraCapturing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _torchState: MutableStateFlow<Int> = MutableStateFlow(TorchState.OFF)
    private val _meteringPoint: MutableStateFlow<MeteringPoint> =
        MutableStateFlow(MeteringPoint(0f, 0f, MeteringPointFactory.getDefaultPointSize()))
    private val _aspectRatio: MutableStateFlow<Int> = MutableStateFlow(AspectRatio.RATIO_4_3)
    private val _len: MutableStateFlow<Int> = MutableStateFlow(CameraSelector.LENS_FACING_BACK)

    val camera: Camera?
        get() = _camera
    val message: MutableStateFlow<String?>
        get() = _message
    val cameraCapturing: MutableStateFlow<Boolean>
        get() = _cameraCapturing
    val torchState: MutableStateFlow<Int>
        get() = _torchState
    val meteringPoint: MutableStateFlow<MeteringPoint>
        get() = _meteringPoint
    val aspectRatio: MutableStateFlow<Int>
        get() = _aspectRatio
    val len: MutableStateFlow<Int>
        get() = _len

    fun bindPreview(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView,
    ) {
        this.context = context
        this.lifecycleOwner = lifecycleOwner

        cameraProvider.unbindAll()

        // Preview
        val preview =
            androidx.camera.core.Preview.Builder().setTargetAspectRatio(_aspectRatio.value).build()
        val cameraSelector: CameraSelector =
            CameraSelector.Builder().requireLensFacing(_len.value).build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        // Image capture
        _imageCapture = ImageCapture.Builder().setTargetRotation(previewView.display.rotation)
            .setTargetAspectRatio(_aspectRatio.value).build()
        _imageCapture.enableOrientation(context)

        // Use case
        val useCaseGroup =
            UseCaseGroup.Builder().addUseCase(preview).addUseCase(_imageCapture).build()

        // Lifecycle binding
        _camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)

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

    private fun getOutputFileOption(): ImageCapture.OutputFileOptions {
        val fileName = SimpleDateFormat(
            FILENAME_FORMAT, Locale.US
        ).getFileName(PHOTO_EXTENSION)
        val outputStream = context.contentResolver.getImageOutputStream(fileName = fileName)
        return ImageCapture.OutputFileOptions.Builder(outputStream).build()
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

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpeg"
    }
}