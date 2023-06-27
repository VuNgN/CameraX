package com.vungn.camerax.util

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.TimeUnit

class QrCodeAnalyzer(private val qrCodeScanned: (List<Barcode>) -> Unit) : ImageAnalysis.Analyzer {
    private val lastAnalyzedTimeStamp = 0L

    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimeStamp >= TimeUnit.SECONDS.toMillis(1)) {
            image.image?.let { imageToAnalyze ->
                val option =
                    BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                val barcodeScanner = BarcodeScanning.getClient(option)
                val imageToProcess =
                    InputImage.fromMediaImage(imageToAnalyze, image.imageInfo.rotationDegrees)

                barcodeScanner.process(imageToProcess).addOnSuccessListener { barcodes ->
                    barcodes.forEach { barcode ->
                        barcode.rawValue?.let { barcodeValue ->
                            Log.d(TAG, "analyze QR code: $barcodeValue")
                        }
                    }
                    qrCodeScanned(barcodes)
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "BarcodeAnalyzer: somethings wrong -> $exception")
                }.addOnCompleteListener {
                    image.close()
                }
            }
        }
    }

    companion object {
        private val TAG = QrCodeAnalyzer::class.simpleName
    }
}