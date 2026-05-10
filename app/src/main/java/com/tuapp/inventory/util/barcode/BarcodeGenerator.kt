package com.tuapp.inventory.util.barcode

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Generador de códigos QR y Barras optimizado para escaneo móvil.
 */
object BarcodeGenerator {

    /**
     * Genera un código QR compacto y de alta resolución.
     */
    fun generateQrCode(
        content: String,
        size: Int = 512
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content, BarcodeFormat.QR_CODE, size, size, hints
            )
            bitMatrix.toBitmap()
        } catch (e: Exception) {
            android.util.Log.e("BarcodeGenerator", "Error QR: ${e.message}")
            null
        }
    }

    fun generateCode128(
        content: String,
        width: Int = 600,
        height: Int = 150
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 4,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content, BarcodeFormat.CODE_128, width, height, hints
            )
            bitMatrix.toBitmap()
        } catch (e: Exception) {
            android.util.Log.e("BarcodeGenerator", "Error Barcode: ${e.message}")
            null
        }
    }

    private fun BitMatrix.toBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}
