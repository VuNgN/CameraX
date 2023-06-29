@file:OptIn(ExperimentalMaterial3Api::class)

package com.vungn.camerax

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.vungn.camerax.ui.CameraX
import com.vungn.camerax.ui.theme.CameraXTheme
import com.vungn.camerax.util.CameraXHelper

class MainActivity : ComponentActivity() {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private val cameraXHelper: CameraXHelper by lazy {
        CameraXHelper.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val previewView = remember { PreviewView(context) }
            val isGranted = remember { mutableStateOf(false) }
            val snackBarHostState = remember { SnackbarHostState() }
            val useCase = cameraXHelper.useCase.collectAsState()
            val message = cameraXHelper.message.collectAsState()
            val cameraCapturing = cameraXHelper.cameraCapturing.collectAsState()
            val videoPause = cameraXHelper.videoPause.collectAsState()
            val torchState = cameraXHelper.torchState.collectAsState()
            val meteringPoint = cameraXHelper.meteringPoint.collectAsState()
            val aspectRatio = cameraXHelper.aspectRatio.collectAsState()
            val cameraLen = cameraXHelper.len.collectAsState()
            val barcodes = cameraXHelper.barcodes.collectAsState()
            val videoTimer = cameraXHelper.videoTimer.collectAsState()
            val filteredQualities = cameraXHelper.filteredQualities.collectAsState()
            val videoQuality = cameraXHelper.videoQuality.collectAsState()
            val rotation = cameraXHelper.rotation.collectAsState()
            val state = rememberTransformableState { zoomChange, _, _ ->
                cameraXHelper.camera?.cameraControl?.setZoomRatio(cameraXHelper.camera?.cameraInfo?.zoomState?.value?.zoomRatio!! * zoomChange)
            }
            val requestMultiplePermissions =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        Log.d(TAG, "is granted: ${permissions.all { true }}")
                        isGranted.value = permissions.all { true }
                    })

            LaunchedEffect(key1 = true, block = {
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            })
            LaunchedEffect(keys = arrayOf(
                isGranted.value,
                aspectRatio.value,
                cameraLen.value,
                useCase.value,
                videoQuality.value
            ), block = {
                if (isGranted.value) {
                    cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        cameraXHelper.bindPreview(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            cameraProvider = cameraProvider,
                            previewView = previewView
                        )
                    }, ContextCompat.getMainExecutor(context))
                }
            })
            LaunchedEffect(key1 = message.value, block = {
                if (message.value != null) {
                    snackBarHostState.showSnackbar(message = message.value!!)
                }
            })
            CameraXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(modifier = Modifier,
                        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }) { padding ->
                        CameraX(
                            previewView = previewView,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .transformable(state = state),
                            useCase = useCase.value,
                            capturing = cameraCapturing.value,
                            videoPause = videoPause.value,
                            torchState = torchState.value,
                            meteringPoint = meteringPoint.value,
                            aspectRatio = aspectRatio.value,
                            cameraLen = cameraLen.value,
                            barcodes = barcodes.value,
                            videoTimer = videoTimer.value,
                            filteredQualities = filteredQualities.value,
                            videoQuality = videoQuality.value,
                            rotation = rotation.value,
                            imageCapture = cameraXHelper::imageCapturing,
                            changeTorchState = cameraXHelper::changeTorchState,
                            changeRatio = cameraXHelper::changeAspectRatio,
                            switchCamera = cameraXHelper::switchCamera,
                            videoCapture = cameraXHelper::videoCapturing,
                            stopVideoCapturing = cameraXHelper::stopVideoCapturing,
                            pauseVideoCapturing = cameraXHelper::pauseVideoCapturing,
                            resumeVideoCapturing = cameraXHelper::resumeVideoCapturing,
                            changeUseCase = cameraXHelper::changeUseCase,
                            changeVideoQuality = cameraXHelper::changeVideoQuality,
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}