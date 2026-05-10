package com.tuapp.inventory.util.barcode

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Analizador de CameraX optimizado para códigos de barras lineales (1D).
 * Descarta QR para mejorar la velocidad en dispositivos de gama media.
 */
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Configuración optimizada: Solo formatos lineales comunes en inventarios
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_ITF
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { value ->
                            // El escaneo fue exitoso, devolvemos el valor al callback
                            onBarcodeDetected(value)
                        }
                    }
                }
                .addOnFailureListener {
                    // Manejar error si es necesario
                }
                .addOnCompleteListener {
                    // CRÍTICO: Cerrar el proxy para permitir que el siguiente frame sea procesado
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
