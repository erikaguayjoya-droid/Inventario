package com.tuapp.inventory.util.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.content.FileProvider
import com.tuapp.inventory.data.local.entity.InventoryItem
import com.tuapp.inventory.domain.model.AuditReport
import com.tuapp.inventory.util.barcode.BarcodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfGenerator @Inject constructor(private val context: Context) {

    private val TAG = "PdfGenerator"
    private val PAGE_WIDTH  = 595 // A4 width in points
    private val PAGE_HEIGHT = 842 // A4 height in points
    private val MARGIN      = 40f
    private val CONTENT_W   = PAGE_WIDTH - MARGIN * 2

    suspend fun generateLabel(item: InventoryItem): Uri? = withContext(Dispatchers.IO) {
        try {
            // Reemplazamos QR por Código de Barras (CODE_128)
            val barcodeBitmap = BarcodeGenerator.generateCode128(item.noActivo, width = 600, height = 200)
                ?: return@withContext null

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(408, 528, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            drawLabel(page.canvas, item, barcodeBitmap)
            pdfDocument.finishPage(page)

            val file = savePdfToCache(pdfDocument, "etiqueta_${item.noActivo.replace("/", "-")}")
            pdfDocument.close()

            return@withContext FileProvider.getUriForFile(
                context,
                "com.tuapp.inventory.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar etiqueta para ${item.noActivo}", e)
            return@withContext null
        }
    }

    /**
     * Genera el reporte de auditoría.
     */
    suspend fun generateAuditReportPdf(report: AuditReport): Uri? = withContext(Dispatchers.IO) {
        Log.d("DEBUG_AUDIT", "Iniciando PDF de reporte con StaticLayout...")

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()

        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = TextPaint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }
        val boldPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.BLACK
            typeface = Typeface.DEFAULT_BOLD
        }

        var y = MARGIN

        // 1. Encabezado
        boldPaint.textSize = 18f
        canvas.drawText("Reporte de Conciliación de Inventario", MARGIN, y, boldPaint)
        y += 30f

        paint.textSize = 10f
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Fecha de generación: $timestamp", MARGIN, y, paint)
        y += 40f

        // 2. Resumen Numérico
        boldPaint.textSize = 14f
        canvas.drawText("Resumen Ejecutivo", MARGIN, y, boldPaint)
        y += 25f

        paint.textSize = 12f
        canvas.drawText("Total Coincidencias: ${report.matches.size}", MARGIN + 20, y, paint)
        y += 20f
        canvas.drawText("Total Faltantes: ${report.missing.size}", MARGIN + 20, y, paint)
        y += 20f
        canvas.drawText("Total Extras (Sobrantes): ${report.extras.size}", MARGIN + 20, y, paint)
        y += 45f

        // Helper para dibujar bloques de texto con StaticLayout
        fun drawWrappedItem(text: String, currentY: Float): Float {
            val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, CONTENT_W.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()

            var finalY = currentY

            if (finalY + staticLayout.height > PAGE_HEIGHT - MARGIN) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                finalY = MARGIN
            }

            canvas.save()
            canvas.translate(MARGIN, finalY)
            staticLayout.draw(canvas)
            canvas.restore()

            return finalY + staticLayout.height + 6f
        }

        // 3. Sección de Faltantes
        if (report.missing.isNotEmpty()) {
            boldPaint.textSize = 14f
            boldPaint.color = Color.RED

            if (y > PAGE_HEIGHT - 100) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
            }

            canvas.drawText("SECCIÓN: FALTANTES (En Auditoría, no en Facultad)", MARGIN, y, boldPaint)
            y += 25f
            boldPaint.color = Color.BLACK

            paint.textSize = 10f
            report.missing.forEach { item ->
                val text = "• [${item.activo ?: "S/N"}] ${item.descripcion ?: "Sin descripción"}"
                y = drawWrappedItem(text, y)
            }
            y += 25f
        }

        // 4. Sección de Extras (Sobrantes)
        if (report.extras.isNotEmpty()) {
            boldPaint.textSize = 14f
            boldPaint.color = Color.parseColor("#1B5E20")

            if (y > PAGE_HEIGHT - 100) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
            }

            canvas.drawText("SECCIÓN: EXTRAS (En Facultad, no en Auditoría)", MARGIN, y, boldPaint)
            y += 25f
            boldPaint.color = Color.BLACK

            paint.textSize = 10f

            report.extras.forEach { item ->
                val activoStr = item.noActivo ?: "S/N"
                val nombreStr = item.nombre ?: "Sin descripción"

                val text = "• [$activoStr] $nombreStr"
                y = drawWrappedItem(text, y)
            }
        }

        pdfDocument.finishPage(page)
        val file = savePdfToCache(pdfDocument, "reporte_auditoria")
        pdfDocument.close()

        return@withContext FileProvider.getUriForFile(
            context,
            "com.tuapp.inventory.fileprovider",
            file
        )
    }

    private fun drawLabel(canvas: Canvas, item: InventoryItem, barcodeBitmap: Bitmap) {
        canvas.drawColor(Color.WHITE)
        val headerPaint = Paint().apply { color = Color.parseColor("#1A237E"); textSize = 14f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val valuePaint = Paint().apply { color = Color.BLACK; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val labelPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f; isAntiAlias = true }
        val dividerPaint = Paint().apply { color = Color.parseColor("#1A237E"); strokeWidth = 1.5f; style = Paint.Style.STROKE }

        var y = 50f
        canvas.drawText("FACULTAD DE INGENIERÍA", 30f, y, headerPaint); y += 20f
        canvas.drawLine(30f, y, 378f, y, dividerPaint); y += 25f

        canvas.drawText("NOMBRE DEL ACTIVO:", 30f, y, labelPaint); y += 15f
        y = drawWrappedText(canvas, item.nombre.uppercase(), 30f, y, 348f, valuePaint); y += 20f

        canvas.drawText("UBICACIÓN / ÁREA:", 30f, y, labelPaint); y += 15f
        canvas.drawText(item.area, 30f, y, valuePaint); y += 25f

        canvas.drawLine(30f, y, 378f, y, dividerPaint); y += 30f

        // Ajuste para Código de Barras (Rectangular 5:2 aprox)
        val barcodeWidth = 250f
        val barcodeHeight = 100f
        val scaledBarcode = Bitmap.createScaledBitmap(barcodeBitmap, barcodeWidth.toInt(), barcodeHeight.toInt(), true)
        canvas.drawBitmap(scaledBarcode, (408f - barcodeWidth) / 2f, y, null); y += barcodeHeight + 15f

        val idPaint = Paint().apply { color = Color.BLACK; textSize = 10f; typeface = Typeface.MONOSPACE; isAntiAlias = true }
        val idTextW = idPaint.measureText(item.noActivo)
        canvas.drawText(item.noActivo, (408f - idTextW) / 2f, y, idPaint)
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, startY: Float, maxWidth: Float, paint: Paint): Float {
        val words = text.split(" ")
        var line = ""; var y = startY
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) > maxWidth) {
                if (line.isNotEmpty()) { canvas.drawText(line, x, y, paint); y += paint.textSize + 5f }
                line = word
            } else { line = testLine }
        }
        if (line.isNotEmpty()) { canvas.drawText(line, x, y, paint); y += paint.textSize + 5f }
        return y
    }

    @Throws(IOException::class)
    private fun savePdfToCache(doc: PdfDocument, prefix: String): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            doc.writeTo(out)
            out.flush()
        }
        return file
    }
}
