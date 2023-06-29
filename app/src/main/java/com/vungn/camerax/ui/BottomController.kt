package com.vungn.camerax.ui

import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vungn.camerax.util.CameraXHelper
import com.vungn.camerax.util.paddingBottom
import com.vungn.camerax.util.toRotationFloat
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BottomController(
    modifier: Modifier = Modifier,
    cameraLen: Int,
    imageCapture: () -> Unit = {},
    switchCamera: (Int) -> Unit,
    useCase: CameraXHelper.UseCase,
    switchUseCase: (CameraXHelper.UseCase) -> Unit,
    capturing: Boolean,
    videoPause: Boolean,
    videoTimer: String,
    rotation: Int,
    videoCapture: () -> Unit,
    stopVideoCapturing: () -> Unit,
    pauseVideoCapturing: () -> Unit,
    resumeVideoCapturing: () -> Unit
) {
    var state by rememberSaveable { mutableStateOf(0) }
    val titles = listOf(CameraXHelper.UseCase.PHOTO, CameraXHelper.UseCase.VIDEO)
    val myRotation by animateFloatAsState(targetValue = rotation.toRotationFloat())
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(targetState = capturing && useCase == CameraXHelper.UseCase.VIDEO,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                // Compare the incoming number with the previous number.
                if (targetState > initialState) {
                    // If the target number is larger, it slides up and fades in
                    // while the initial (smaller) number slides up and fades out.
                    slideInVertically { height -> height } + fadeIn() with slideOutVertically { height -> -height } + fadeOut()
                } else {
                    // If the target number is smaller, it slides down and fades in
                    // while the initial number slides down and fades out.
                    slideInVertically { height -> -height } + fadeIn() with slideOutVertically { height -> height } + fadeOut()
                }.using(
                    // Disable clipping since the faded slide-in/out should
                    // be displayed out of bounds.
                    SizeTransform(clip = false)
                )
            }) { capturing ->
            if (!capturing) {
                TabRow(selectedTabIndex = state,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    divider = { Divider(color = Color.Transparent) }) {
                    titles.forEachIndexed { index, title ->
                        Tab(selected = state == index, onClick = {
                            state = index
                            switchUseCase(title)
                        }, text = {
                            if (state == index) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp, vertical = 5.dp
                                        )
                                    )
                                }
                            } else {
                                Text(
                                    text = title.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        })
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(4.dp)
                        )
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var fiberShowing by remember { mutableStateOf(true) }
                    LaunchedEffect(key1 = true, block = {
                        while (true) {
                            fiberShowing = !fiberShowing
                            delay(500)
                        }
                    })
                    AnimatedContent(
                        modifier = Modifier.padding(start = 10.dp, end = 0.dp),
                        targetState = fiberShowing,
                        contentAlignment = Alignment.Center
                    ) { showing ->
                        Icon(
                            modifier = Modifier.size(10.dp),
                            imageVector = Icons.Rounded.FiberManualRecord,
                            contentDescription = null,
                            tint = if (showing && !videoPause) Color.Red else Color.Transparent
                        )
                    }
                    Text(
                        text = videoTimer,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(
                            start = 5.dp, end = 10.dp, top = 10.dp, bottom = 10.dp
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = paddingBottom / 2, bottom = paddingBottom),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraButton(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(50.dp),
                onClick = { /*TODO*/ },
                imageVector = Icons.Rounded.Image,
                contentDescription = "Select image",
                containerColor = MaterialTheme.colorScheme.background.copy(
                    0.5f
                ),
                contentColor = MaterialTheme.colorScheme.onBackground,
                type = CameraButtonType.FILLED,
                rotation = myRotation
            )

            if (useCase == CameraXHelper.UseCase.PHOTO) {
                Surface(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(4.dp, Color.Gray, CircleShape)
                        .clickable(enabled = !capturing) { imageCapture() },
                    color = Color.Gray.copy(alpha = 0.5f)
                ) {}
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(4.dp, Color.Gray, CircleShape)
                        .clickable {
                            if (capturing) stopVideoCapturing()
                            else videoCapture()
                        }, contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(targetState = capturing,
                        contentAlignment = Alignment.Center,
                        transitionSpec = {
                            (scaleIn() with scaleOut()).using(
                                SizeTransform(clip = false)
                            )
                        }) { capturing ->
                        if (capturing) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = "Pause video",
                                tint = Color.Red
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.FiberManualRecord,
                                contentDescription = "Capture video",
                                tint = Color.Red,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(1.dp)
                            )
                        }
                    }
                }
            }

            FilledIconButton(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(50.dp), onClick = {
                    if (capturing && useCase == CameraXHelper.UseCase.VIDEO) {
                        if (videoPause) resumeVideoCapturing() else pauseVideoCapturing()
                    } else {
                        switchCamera(if (cameraLen == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK)
                    }
                }, colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(
                        0.5f
                    ), contentColor = MaterialTheme.colorScheme.onBackground
                )
            ) {
                AnimatedContent(targetState = capturing,
                    contentAlignment = Alignment.Center,
                    transitionSpec = {
                        (scaleIn() with scaleOut()).using(
                            SizeTransform(clip = false)
                        )
                    }) { capturing ->
                    AnimatedContent(targetState = videoPause,
                        contentAlignment = Alignment.Center,
                        transitionSpec = {
                            (scaleIn() with scaleOut()).using(
                                SizeTransform(clip = false)
                            )
                        }) { videoPause ->
                        Icon(
                            modifier = Modifier.rotate(myRotation),
                            imageVector = if (capturing && useCase == CameraXHelper.UseCase.VIDEO) {
                                if (videoPause) Icons.Rounded.PlayArrow else Icons.Rounded.Pause
                            } else Icons.Rounded.Cameraswitch,
                            contentDescription = "Switch camera"
                        )
                    }
                }
            }
        }
    }
}