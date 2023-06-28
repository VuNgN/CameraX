package com.vungn.camerax.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vungn.camerax.util.MeteringPoint
import com.vungn.camerax.util.offsetX
import com.vungn.camerax.util.offsetY

@Composable
fun FocusView(meteringPoint: MeteringPoint) {
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
