package com.vungn.camerax.ui

import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.TorchState
import androidx.camera.video.Quality
import androidx.camera.view.PreviewView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.barcode.common.Barcode
import com.vungn.camerax.MainActivity.Companion.TAG
import com.vungn.camerax.util.CameraXHelper
import com.vungn.camerax.util.MeteringPoint
import com.vungn.camerax.util.cameraHeight169
import com.vungn.camerax.util.cameraHeight43
import com.vungn.camerax.util.cameraHeightFull
import com.vungn.camerax.util.cameraWidth
import kotlinx.coroutines.delay


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CameraX(
    modifier: Modifier = Modifier,
    previewView: PreviewView,
    filteredQualities: List<Quality>,
    capturing: Boolean,
    torchState: Int = TorchState.OFF,
    meteringPoint: MeteringPoint = MeteringPoint(
        0f, 0f, MeteringPointFactory.getDefaultPointSize()
    ),
    aspectRatio: Int = AspectRatio.RATIO_4_3,
    videoPause: Boolean,
    cameraLen: Int = CameraSelector.LENS_FACING_BACK,
    barcodes: List<Barcode>,
    useCase: CameraXHelper.UseCase,
    videoTimer: String,
    videoQuality: Quality,
    rotation: Int,
    imageCapture: () -> Unit = {},
    changeTorchState: () -> Unit = {},
    changeRatio: (Int) -> Unit = {},
    switchCamera: (Int) -> Unit = {},
    videoCapture: () -> Unit,
    stopVideoCapturing: () -> Unit,
    pauseVideoCapturing: () -> Unit,
    resumeVideoCapturing: () -> Unit,
    changeUseCase: (CameraXHelper.UseCase) -> Unit,
    changeVideoQuality: (Quality) -> Unit
) {
    var showMeteringView by remember { mutableStateOf(false) }
    var targetHeight by remember { mutableStateOf(0.dp) }
    val cameraHeight by animateDpAsState(
        targetValue = targetHeight, animationSpec = tween(easing = FastOutLinearInEasing)
    )
    LaunchedEffect(key1 = meteringPoint, block = {
        Log.d(TAG, "CameraX: tab (${meteringPoint.x}, ${meteringPoint.y})")
        showMeteringView = true
        delay(5000)
        showMeteringView = false
    })
    LaunchedEffect(keys = arrayOf(aspectRatio, useCase), block = {
        targetHeight = if (useCase == CameraXHelper.UseCase.VIDEO) cameraHeight43
        else when (aspectRatio) {
            AspectRatio.RATIO_4_3 -> cameraHeight43
            AspectRatio.RATIO_16_9 -> cameraHeight169
            else -> cameraHeightFull
        }
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
            useCase = useCase,
            filteredQualities = filteredQualities,
            torchState = torchState,
            changeTorchState = changeTorchState,
            aspectRatio = aspectRatio,
            changeRatio = changeRatio,
            capturing = capturing,
            videoQuality = videoQuality,
            rotation = rotation,
            changeVideoQuality = changeVideoQuality
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            AndroidView(
                factory = { previewView }, modifier = Modifier.size(
                    width = cameraWidth, height = cameraHeight
                )
            )
            if (capturing && useCase == CameraXHelper.UseCase.PHOTO) {
                Surface(
                    modifier = Modifier.size(
                        width = cameraWidth, height = cameraHeight
                    ), color = Color.Black
                ) {}
            }
            BottomController(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Transparent),
                imageCapture = imageCapture,
                capturing = capturing,
                cameraLen = cameraLen,
                switchCamera = switchCamera,
                useCase = useCase,
                videoTimer = videoTimer,
                rotation = rotation,
                switchUseCase = { changeUseCase(it) },
                videoCapture = videoCapture,
                videoPause = videoPause,
                stopVideoCapturing = stopVideoCapturing,
                pauseVideoCapturing = pauseVideoCapturing,
                resumeVideoCapturing = resumeVideoCapturing
            )
            if (showMeteringView) {
                FocusView(meteringPoint)
            }
            if (barcodes.isNotEmpty() && useCase == CameraXHelper.UseCase.PHOTO) {
                barcodes.forEach { barcode ->
                    Button(modifier = Modifier
                        .graphicsLayer(
                            translationX = barcode.boundingBox?.exactCenterX()!!,
                            translationY = barcode.boundingBox?.exactCenterY()!!
                        )
                        .clip(RoundedCornerShape(10.dp)), colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ), onClick = {}) {
                        Text(
                            text = barcode.rawValue.toString(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}