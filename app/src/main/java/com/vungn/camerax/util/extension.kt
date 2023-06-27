package com.vungn.camerax.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.CountDownTimer
import android.provider.MediaStore
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import com.google.mlkit.vision.barcode.common.Barcode
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.concurrent.Executors

fun SimpleDateFormat.getFileName(extension: String): String =
    this.format(System.currentTimeMillis()) + extension

fun ContentResolver.getImageOutputStream(
    fileName: String
): OutputStream {
    val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val contentValues = ContentValues()
    contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
    contentValues.put(MediaStore.Images.Media.MIME_TYPE, "images/*")
    val uri = this.insert(filePath, contentValues)

    return this.openOutputStream(uri!!)!!
}

fun ImageCapture.enableOrientation(context: Context) {
    val orientationEventListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation: Int = when (orientation) {
                in 45..134 -> Surface.ROTATION_270
                in 135..224 -> Surface.ROTATION_180
                in 225..314 -> Surface.ROTATION_90
                else -> Surface.ROTATION_0
            }
            this@enableOrientation.targetRotation = rotation
        }
    }
    orientationEventListener.enable()
}

fun ImageAnalysis.setupAnalyzer(onFinished: (List<Barcode>) -> Unit) {
    val timer = object : CountDownTimer(500, 1000) {
        override fun onTick(millisUntilFinished: Long) {}

        override fun onFinish() {
            onFinished(emptyList())
        }
    }
    this.setAnalyzer(Executors.newSingleThreadExecutor(), QrCodeAnalyzer { barcodes ->
        if (barcodes.isNotEmpty()) {
            timer.start()
            onFinished(barcodes)
        }
    })
}