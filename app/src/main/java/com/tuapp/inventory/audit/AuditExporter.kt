package com.tuapp.inventory.audit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.tuapp.inventory.audit.model.AuditResult
import com.tuapp.inventory.audit.model.DiffStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditExporter @Inject constructor(private val context: Context) {

    private val dateStr: String
        get() = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())

    suspend fun exportCsv(result: AuditResult): Uri? = withContext(Dispatchers.IO) {
        try {
            val file = File(File(context.cacheDir, "audits").also { it.mkdirs() }, "auditoria_$dateStr.csv")
            FileOutputStream(file).bufferedWriter(Charsets.UTF_8).use { w ->
                w.write("\uFEFF")
                w.writeLine("REPORTE DE AUDITORÍA DE INVENTARIO")
                w.writeLine("Generado: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
                w.writeLine("Ítems en DB: ${result.totalDb} | Ítems en Excel: ${result.totalAudit}")
                w.writeLine("Total diferencias: ${result.totalDiffs}")
                w.writeLine("")
                w.writeLine("ESTADO;NO. ACTIVO;NOMBRE;ÁREA EN DB;ÁREA EN EXCEL")

                if (result.missing.isNotEmpty()) {
                    w.writeLine(";--- FALTANTES (${result.missing.size}) ---;;;;")
                    result.missing.forEach { d ->
                        w.writeLine("FALTANTE;${d.noActivo};${d.nombre.escapeCsv()};${d.dbArea ?: ""};")
                    }
                    w.writeLine("")
                }
                if (result.surplus.isNotEmpty()) {
                    w.writeLine(";--- SOBRANTES (${result.surplus.size}) ---;;;;")
                    result.surplus.forEach { d ->
                        w.writeLine("SOBRANTE;${d.noActivo};${d.nombre.escapeCsv()};;${d.auditArea ?: ""}")
                    }
                    w.writeLine("")
                }
                if (result.wrongArea.isNotEmpty()) {
                    w.writeLine(";--- UBICACIÓN INCORRECTA (${result.wrongArea.size}) ---;;;;")
                    result.wrongArea.forEach { d ->
                        w.writeLine("UBICACIÓN INCORRECTA;${d.noActivo};${d.nombre.escapeCsv()};${d.dbArea ?: ""};${d.auditArea ?: ""}")
                    }
                }
            }
            file.toShareUri()
        } catch (e: Exception) {
            android.util.Log.e("AuditExporter", "Error CSV", e); null
        }
    }

    suspend fun exportPdf(result: AuditResult): Uri? = withContext(Dispatchers.IO) {
        try {
            val pageW = 612; val pageH = 792; val margin = 40f; val contentW = pageW - margin * 2
            val pdfDoc = PdfDocument()
            var pageNumber = 1
            var page   = pdfDoc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create())
            var canvas = page.canvas
            var y = margin

            val titlePaint  = Paint().apply { color = Color.parseColor("#1A237E"); textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
            val headerPaint = Paint().apply { color = Color.parseColor("#283593"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
            val bodyPaint   = Paint().apply { color = Color.DKGRAY; textSize = 9f; isAntiAlias = true }
            val smallPaint  = Paint().apply { color = Color.GRAY; textSize = 8f; isAntiAlias = true }

            val sectionColors = mapOf(
                DiffStatus.MISSING    to Color.parseColor("#B71C1C"),
                DiffStatus.SURPLUS    to Color.parseColor("#1B5E20"),
                DiffStatus.WRONG_AREA to Color.parseColor("#E65100")
            )

            fun newPageIfNeeded(h: Float) {
                if (y + h > pageH - margin) {
                    pdfDoc.finishPage(page); pageNumber++
                    page = pdfDoc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create())
                    canvas = page.canvas; y = margin
                }
            }

            canvas.drawText("Reporte de Auditoría de Inventario", margin, y, titlePaint); y += 24f
            canvas.drawText(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()), margin, y, smallPaint); y += 14f
            canvas.drawText("DB: ${result.totalDb}  |  Excel: ${result.totalAudit}  |  Diferencias: ${result.totalDiffs}", margin, y, smallPaint); y += 6f
            canvas.drawLine(margin, y, margin + contentW, y, Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }); y += 18f

            listOf(
                Triple("FALTANTES",             result.missing,   DiffStatus.MISSING),
                Triple("SOBRANTES",             result.surplus,   DiffStatus.SURPLUS),
                Triple("UBICACIÓN INCORRECTA",  result.wrongArea, DiffStatus.WRONG_AREA)
            ).forEach { (title, diffs, status) ->
                if (diffs.isEmpty()) return@forEach
                newPageIfNeeded(40f)
                val sColor = sectionColors[status] ?: Color.BLACK
                canvas.drawRect(margin, y - 12f, margin + contentW, y + 6f, Paint().apply { color = sColor; alpha = 30 })
                headerPaint.color = sColor
                canvas.drawText("$title (${diffs.size})", margin + 4f, y, headerPaint); y += 16f

                val c1 = margin; val c2 = margin + 100f; val c3 = margin + contentW * 0.62f; val c4 = margin + contentW * 0.81f
                canvas.drawText("No. Activo", c1, y, smallPaint)
                canvas.drawText("Nombre", c2, y, smallPaint)
                if (status == DiffStatus.WRONG_AREA) { canvas.drawText("Área DB", c3, y, smallPaint); canvas.drawText("Área Excel", c4, y, smallPaint) }
                else canvas.drawText("Área", c3, y, smallPaint)
                y += 4f
                canvas.drawLine(margin, y, margin + contentW, y, Paint().apply { color = Color.LTGRAY }); y += 12f

                diffs.forEach { diff ->
                    newPageIfNeeded(16f)
                    canvas.drawText(diff.noActivo, c1, y, bodyPaint)
                    canvas.drawText(diff.nombre.take(28) + if (diff.nombre.length > 28) "…" else "", c2, y, bodyPaint)
                    when (status) {
                        DiffStatus.MISSING    -> canvas.drawText(diff.dbArea    ?: "", c3, y, bodyPaint)
                        DiffStatus.SURPLUS    -> canvas.drawText(diff.auditArea ?: "", c3, y, bodyPaint)
                        DiffStatus.WRONG_AREA -> {
                            canvas.drawText(diff.dbArea ?: "", c3, y, bodyPaint)
                            canvas.drawText(diff.auditArea ?: "", c4, y, bodyPaint.apply { color = sColor })
                            bodyPaint.color = Color.DKGRAY
                        }
                    }
                    y += 16f
                }
                y += 10f
            }

            canvas.drawText("Generado por App Inventario Facultad — Pág. $pageNumber", margin, pageH - margin / 2, smallPaint)
            pdfDoc.finishPage(page)

            val file = File(File(context.cacheDir, "audits").also { it.mkdirs() }, "reporte_auditoria_$dateStr.pdf")
            FileOutputStream(file).use { pdfDoc.writeTo(it) }
            pdfDoc.close()
            file.toShareUri()
        } catch (e: Exception) {
            android.util.Log.e("AuditExporter", "Error PDF", e); null
        }
    }

    private fun File.toShareUri(): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)

    private fun java.io.BufferedWriter.writeLine(line: String) { write(line); newLine() }

    private fun String.escapeCsv(): String =
        if (contains(";") || contains("\"") || contains("\n"))
            "\"${replace("\"", "\"\"")}\""
        else this
}
