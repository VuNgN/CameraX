package com.vungn.camerax.ui


import androidx.camera.core.AspectRatio
import androidx.camera.core.TorchState
import androidx.camera.video.Quality
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vungn.camerax.R
import com.vungn.camerax.util.CameraXHelper
import com.vungn.camerax.util.qualityToString

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TopController(
    modifier: Modifier,
    filteredQualities: List<Quality>,
    torchState: Int = TorchState.OFF,
    aspectRatio: Int = AspectRatio.RATIO_4_3,
    useCase: CameraXHelper.UseCase,
    capturing: Boolean,
    videoQuality: Quality,
    changeTorchState: () -> Unit = {},
    changeRatio: (Int) -> Unit = {},
    changeVideoQuality: (Quality) -> Unit
) {
    var ratioChoosing by remember { mutableStateOf(false) }
    var qualityChoosing by remember { mutableStateOf(false) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!capturing) {
            if (useCase == CameraXHelper.UseCase.PHOTO) {
                AnimatedContent(
                    targetState = ratioChoosing, contentAlignment = Alignment.Center
                ) { isChosen ->
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
                                    painter = when (aspectRatio) {
                                        AspectRatio.RATIO_4_3 -> painterResource(id = R.drawable.aspect_ratio_4_3)
                                        AspectRatio.RATIO_16_9 -> painterResource(id = R.drawable.aspect_ratio_16_9)
                                        else -> painterResource(id = R.drawable.aspect_ratio_4_3)
                                    }, contentDescription = "Flash button"
                                )
                            }
                        }
                    }
                }
            }
            if (useCase == CameraXHelper.UseCase.VIDEO) {
                AnimatedContent(
                    targetState = qualityChoosing, contentAlignment = Alignment.Center
                ) { choosing ->
                    if (choosing) {
                        Row(
                            modifier = Modifier,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            filteredQualities.forEach { quality ->
                                IconButton(colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = if (videoQuality == quality) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                ), onClick = {
                                    changeVideoQuality(quality)
                                    qualityChoosing = !qualityChoosing
                                }) {
                                    Text(text = quality.qualityToString())
                                }
                            }
                        }
                    } else {
                        IconButton(onClick = { qualityChoosing = !qualityChoosing }) {
                            Text(text = videoQuality.qualityToString())
                        }
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
