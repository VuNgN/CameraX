@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)

package com.vungn.camerax

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.vungn.camerax.MainActivity.Companion.TAG
import com.vungn.camerax.ui.theme.CameraXTheme
import com.vungn.camerax.util.CameraXHelper
import com.vungn.camerax.util.MeteringPoint
import com.vungn.camerax.util.cameraHeight169
import com.vungn.camerax.util.cameraHeight43
import com.vungn.camerax.util.cameraHeightFull
import com.vungn.camerax.util.cameraWidth
import com.vungn.camerax.util.offsetX
import com.vungn.camerax.util.offsetY
import com.vungn.camerax.util.paddingBottom
import kotlinx.coroutines.delay

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
            val message = cameraXHelper.message.collectAsState()
            val cameraCapturing = cameraXHelper.cameraCapturing.collectAsState()
            val state = rememberTransformableState { zoomChange, _, _ ->
                cameraXHelper.camera?.cameraControl?.setZoomRatio(cameraXHelper.camera?.cameraInfo?.zoomState?.value?.zoomRatio!! * zoomChange)
            }
            val torchState = cameraXHelper.torchState.collectAsState()
            val meteringPoint = cameraXHelper.meteringPoint.collectAsState()
            val aspectRatio = cameraXHelper.aspectRatio.collectAsState()
            val cameraLen = cameraXHelper.len.collectAsState()
            val requestMultiplePermissions =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        Log.d(TAG, "is granted: ${permissions.all { true }}")
                        isGranted.value = permissions.all { true }
                    })

            LaunchedEffect(key1 = true, block = {
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            })
            LaunchedEffect(key1 = isGranted.value,
                key2 = aspectRatio.value,
                key3 = cameraLen.value,
                block = {
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
                            cameraCapturing = cameraCapturing.value,
                            torchState = torchState.value,
                            meteringPoint = meteringPoint.value,
                            aspectRatio = aspectRatio.value,
                            cameraLen = cameraLen.value,
                            imageCapturing = cameraXHelper::imageCapturing,
                            changeTorchState = cameraXHelper::changeTorchState,
                            changeRatio = cameraXHelper::changeAspectRatio,
                            switchCamera = cameraXHelper::switchCamera
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

@Composable
fun CameraX(
    previewView: PreviewView,
    modifier: Modifier = Modifier,
    cameraCapturing: Boolean,
    torchState: Int = TorchState.OFF,
    meteringPoint: MeteringPoint = MeteringPoint(
        0f, 0f, MeteringPointFactory.getDefaultPointSize()
    ),
    aspectRatio: Int = AspectRatio.RATIO_4_3,
    cameraLen: Int = CameraSelector.LENS_FACING_BACK,
    imageCapturing: () -> Unit = {},
    changeTorchState: () -> Unit = {},
    changeRatio: (Int) -> Unit = {},
    switchCamera: (Int) -> Unit = {}
) {
    var showMeteringView by remember { mutableStateOf(false) }
    val cameraHeight by animateDpAsState(
        targetValue = if (cameraCapturing) 0.dp else when (aspectRatio) {
            AspectRatio.RATIO_4_3 -> cameraHeight43
            AspectRatio.RATIO_16_9 -> cameraHeight169
            else -> cameraHeightFull
        }, animationSpec = tween(easing = FastOutLinearInEasing)
    )
    LaunchedEffect(key1 = meteringPoint, block = {
        Log.d(TAG, "CameraX: tab (${meteringPoint.x}, ${meteringPoint.y})")
        showMeteringView = true
        delay(5000)
        showMeteringView = false
    })
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopController(
            modifier = Modifier
                .background(Color.Transparent)
                .fillMaxWidth(),
            torchState = torchState,
            changeTorchState = changeTorchState,
            aspectRatio = aspectRatio,
            changeRatio = changeRatio
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            AndroidView(
                factory = { previewView }, modifier = Modifier.size(
                    width = cameraWidth, height = cameraHeight
                )
            )
            BottomController(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Transparent),
                imageCapturing = imageCapturing,
                cameraLen = cameraLen,
                switchCamera = switchCamera
            )
            if (showMeteringView) {
                FocusView(meteringPoint)
            }
        }
    }
}

@Composable
fun TopController(
    modifier: Modifier,
    torchState: Int = TorchState.OFF,
    changeTorchState: () -> Unit = {},
    aspectRatio: Int = AspectRatio.RATIO_4_3,
    changeRatio: (Int) -> Unit = {}
) {
    var ratioChoosing by remember { mutableStateOf(false) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(targetState = ratioChoosing) { isChosen ->
            Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isChosen) {
                    IconButton(colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (aspectRatio == AspectRatio.RATIO_4_3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    ), onClick = {
                        changeRatio(AspectRatio.RATIO_4_3)
                        ratioChoosing = !ratioChoosing
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.aspect_ratio_4_3),
                            contentDescription = "Ratio 4:3"
                        )
                    }
                    IconButton(colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (aspectRatio == AspectRatio.RATIO_16_9) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    ), onClick = {
                        changeRatio(AspectRatio.RATIO_16_9)
                        ratioChoosing = !ratioChoosing
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.aspect_ratio_16_9),
                            contentDescription = "Ratio 16:9"
                        )
                    }
                } else {
                    IconButton(colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ), onClick = { ratioChoosing = !ratioChoosing }) {
                        Icon(
                            imageVector = Icons.Rounded.AspectRatio,
                            contentDescription = "Flash button"
                        )
                    }

                }

            }
        }
        IconButton(
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = if (torchState == TorchState.ON) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            ), onClick = changeTorchState
        ) {
            Icon(
                imageVector = if (torchState == TorchState.ON) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                contentDescription = "Flash button"
            )
        }
    }
}

@Composable
fun BottomController(
    modifier: Modifier = Modifier,
    cameraLen: Int,
    imageCapturing: () -> Unit = {},
    switchCamera: (Int) -> Unit
) {
    Row(
        modifier = modifier.padding(bottom = paddingBottom),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            modifier = Modifier
                .clip(CircleShape)
                .size(50.dp),
            onClick = { /*TODO*/ },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.background.copy(
                    0.5f
                ), contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(imageVector = Icons.Rounded.Image, contentDescription = "Select image")
        }

        Surface(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(4.dp, Color.Gray, CircleShape)
                .clickable { imageCapturing() },
            color = Color.Gray.copy(alpha = 0.5f)
        ) {}

        FilledIconButton(
            modifier = Modifier
                .clip(CircleShape)
                .size(50.dp),
            onClick = { switchCamera(if (cameraLen == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK) },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.background.copy(
                    0.5f
                ), contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(imageVector = Icons.Rounded.Cameraswitch, contentDescription = "Switch camera")
        }
    }
}

@Composable
private fun FocusView(meteringPoint: MeteringPoint) {
    Box(
        modifier = Modifier
            .offset(offset = {
                IntOffset(
                    meteringPoint.offsetX(), meteringPoint.offsetY()
                )
            })
            .size(meteringPoint.size.dp)
            .background(Color.Transparent)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size((meteringPoint.size / 5).dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            shape = CircleShape,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {}
    }
}
