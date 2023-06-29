package com.vungn.camerax.ui

import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun CameraButton(
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    painter: Painter,
    contentDescription: String? = null,
    onClick: () -> Unit,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    type: CameraButtonType = CameraButtonType.DEFAULT
) {
    if (type == CameraButtonType.FILLED) {
        FilledIconButton(
            modifier = modifier.rotate(rotation),
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = containerColor, contentColor = contentColor
            )
        ) {
            Icon(
                modifier = Modifier, painter = painter, contentDescription = contentDescription
            )
        }
    } else {
        IconButton(
            modifier = modifier.rotate(rotation),
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = containerColor, contentColor = contentColor
            )
        ) {
            Icon(
                modifier = Modifier, painter = painter, contentDescription = contentDescription
            )
        }
    }
}


@Composable
fun CameraButton(
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    imageVector: ImageVector,
    contentDescription: String? = null,
    onClick: () -> Unit,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    type: CameraButtonType = CameraButtonType.DEFAULT
) {
    if (type == CameraButtonType.FILLED) {
        FilledIconButton(
            modifier = modifier.rotate(rotation),
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = containerColor, contentColor = contentColor
            )
        ) {
            Icon(
                modifier = Modifier,
                imageVector = imageVector,
                contentDescription = contentDescription
            )
        }
    } else {
        IconButton(
            modifier = modifier.rotate(rotation),
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = containerColor, contentColor = contentColor
            )
        ) {
            Icon(
                modifier = Modifier,
                imageVector = imageVector,
                contentDescription = contentDescription
            )
        }
    }
}

@Composable
fun CameraButton(
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    text: String,
    onClick: () -> Unit,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    type: CameraButtonType = CameraButtonType.DEFAULT
) {
    if (type == CameraButtonType.FILLED) {
        FilledIconButton(
            modifier = modifier.rotate(rotation),
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = containerColor, contentColor = contentColor
            )
        ) {
            Text(text = text)
        }
    } else {
        IconButton(
            modifier = modifier.rotate(rotation),
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = containerColor, contentColor = contentColor
            )
        ) {
            Text(text = text)
        }
    }
}

enum class CameraButtonType {
    FILLED, DEFAULT,
}