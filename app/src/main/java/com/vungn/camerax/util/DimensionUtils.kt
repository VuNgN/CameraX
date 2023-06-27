package com.vungn.camerax.util

import android.content.res.Resources
import android.graphics.Rect
import android.graphics.RectF
import android.util.DisplayMetrics
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val displayMetrics: DisplayMetrics by lazy { Resources.getSystem().displayMetrics }

/**
 * Returns boundary of the screen in pixels (px).
 */
val screenRectPx: Rect
    get() = displayMetrics.run { Rect(0, 0, widthPixels, heightPixels) }

/**
 * Returns boundary of the screen in density independent pixels (dp).

 */
val screenRectDp: RectF
    get() = screenRectPx.run { RectF(0f, 0f, right.px2dp, bottom.px2dp) }

/**
 * Converts any given number from pixels (px) into density independent pixels (dp).
 */
val Number.px2dp: Float
    get() = this.toFloat() / displayMetrics.density

val paddingBottom: Dp =
    if ((screenRectDp.height() - (screenRectDp.width() / 9 * 16)).dp > ButtonDefaults.MinHeight) {
        (screenRectDp.height() - (screenRectDp.width() / 9 * 16)).dp - ButtonDefaults.MinHeight
    } else {
        (screenRectDp.height() - (screenRectDp.width() / 9 * 16)).dp
    }

val cameraWidth: Dp = screenRectDp.width().dp

val cameraHeightFull: Dp = screenRectDp.height().dp

val cameraHeight43: Dp = (screenRectDp.width() / 3 * 4).dp

val cameraHeight169: Dp = (screenRectDp.width() / 9 * 16).dp
