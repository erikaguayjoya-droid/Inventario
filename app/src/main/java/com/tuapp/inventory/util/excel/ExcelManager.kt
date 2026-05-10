package com.tuapp.inventory.util.excel

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tuapp.inventory.data.local.entity.AuditorItem
import com.tuapp.inventory.data.local.entity.InventoryItem
import com.tuapp.inventory.data.repository.InventoryRepository
import com.tuapp.inventory.util.normalizer.AreaNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.poi.ss.usermodel.*
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ExcelManager"

@Singleton
class ExcelManager @Inject constructor(
    private val context: Context,
    private val repository: InventoryRepository
) {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    enum class ImportType {
        INTERNAL,
        AUDITOR
    }

    fun importFromUri(uri: Uri, type: ImportType, clearFirst: Boolean = true): Flow<ExcelImportResult> = flow {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("No se pudo abrir el archivo.")

            val workbook = WorkbookFactory.create(inputStream)

            if (clearFirst) {
                when (type) {
                    ImportType.INTERNAL -> repository.clearAllItems()
                    ImportType.AUDITOR -> repository.clearAuditorTable()
                }
            }

            val result = when (type) {
                ImportType.INTERNAL -> readInternalFormat(workbook)
                ImportType.AUDITOR -> readAuditorFormat(workbook)
            }

            workbook.close()
            emit(result)

        } catch (e: Exception) {
            Log.e(TAG, "Error crítico en importación ${type.name}", e)
            emit(ExcelImportResult.Error(e.message ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * CARRIL 1: FORMATO INTERNO
     */
    private suspend fun readInternalFormat(workbook: Workbook): ExcelImportResult {
        val targetSheetName = "GENERAL"
        var sheet: Sheet? = null

        for (i in 0 until workbook.numberOfSheets) {
            val currentSheet = workbook.getSheetAt(i)
            if (currentSheet.sheetName.trim().uppercase() == targetSheetName) {
                sheet = currentSheet
                break
            }
        }

        if (sheet == null) return ExcelImportResult.Error("No se encontró la hoja '$targetSheetName'")

        val headerRow = findHeaderRow(sheet) ?: return ExcelImportResult.Error("No hay encabezados")
        val headers = getHeaders(headerRow)
        val columnMap = ColumnMapper.mapColumns(headers)

        val items = mutableListOf<InventoryItem>()
        for (rowIndex in (headerRow.rowNum + 1)..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            if (isRowEmpty(row)) continue

            val noActivo = row.getCell(columnMap.activoIdx)?.getCellValueAsString()?.trim() ?: ""
            if (noActivo.isBlank() || noActivo == "S/E") continue

            val extras = mutableMapOf<String, String>()
            columnMap.extraColumns.forEach { (name, idx) ->
                row.getCell(idx)?.getCellValueAsString()?.trim()?.let { if (it.isNotBlank()) extras[name] = it }
            }

            // Extraer el área/ubicación de la columna si existe, de lo contrario usar el nombre de la hoja
            val areaVal = if (columnMap.areaIdx != -1) {
                row.getCell(columnMap.areaIdx)?.getCellValueAsString()?.trim()
            } else null

            items.add(InventoryItem(
                area = areaVal ?: AreaNormalizer.matchArea(sheet.sheetName) ?: sheet.sheetName,
                noActivo = noActivo,
                nombre = row.getCell(columnMap.nombreIdx)?.getCellValueAsString()?.trim() ?: "Item $noActivo",
                atributosJson = json.encodeToString(extras)
            ))
        }

        repository.insertItems(items)
        return ExcelImportResult.Success(items.size, mapOf(sheet.sheetName to items.size), emptyList())
    }

    /**
     * CARRIL 2: FORMATO AUDITORÍA - FILTRADO POR MAESTRA IRMA
     */
    private suspend fun readAuditorFormat(workbook: Workbook): ExcelImportResult {
        if (workbook.numberOfSheets == 0) return ExcelImportResult.Error("Archivo vacío")

        val sheet = workbook.getSheetAt(0)
        var idIdx = -1; var descIdx = -1; var resIdx = -1; var ubiIdx = -1
        var headerRow: Row? = null

        for (rowIndex in 0..15) {
            val row = sheet.getRow(rowIndex) ?: continue
            val headers = getHeaders(row)
            idIdx = headers.indexOfFirst { simplify(it) in listOf("activo", "no. activo", "no.activo", "id") }
            descIdx = headers.indexOfFirst { simplify(it) in listOf("descripcion", "nombre", "articulo") }
            resIdx = headers.indexOfFirst { simplify(it) in listOf("resguardante", "responsable") }
            ubiIdx = headers.indexOfFirst { simplify(it) in listOf("ubicacion", "area") }

            if (idIdx != -1 && descIdx != -1) { headerRow = row; break }
        }

        if (headerRow == null) return ExcelImportResult.Error("Formato no reconocido")

        val items = mutableListOf<AuditorItem>()
        val targetResguardante = "IRMA ADRIANA CANTU MUNGUIA"

        for (rowIndex in (headerRow.rowNum + 1)..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            if (row.zeroHeight || isRowEmpty(row)) continue

            val currentRes = if (resIdx != -1) row.getCell(resIdx)?.getCellValueAsString()?.trim() ?: "" else ""

            // Filtro de resguardante
            if (!currentRes.equals(targetResguardante, ignoreCase = true)) continue

            val idValue = row.getCell(idIdx)?.getCellValueAsString()?.trim() ?: ""
            if (idValue.isBlank()) continue

            items.add(AuditorItem(
                activo = idValue,
                descripcion = if (descIdx != -1) row.getCell(descIdx)?.getCellValueAsString()?.trim() ?: "" else "Sin descripción",
                resguardante = currentRes,
                ubicacion = if (ubiIdx != -1) row.getCell(ubiIdx)?.getCellValueAsString()?.trim() ?: "" else "Sin ubicación"
            ))
        }

        if (items.isNotEmpty()) {
            repository.insertAuditorItems(items)
            return ExcelImportResult.Success(items.size, mapOf("Electromecánica" to items.size), emptyList())
        }
        return ExcelImportResult.Error("No se encontraron artículos para $targetResguardante")
    }

    private fun simplify(text: String): String {
        val temp = Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
        return temp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    private fun getHeaders(row: Row): List<String> =
        (0 until row.lastCellNum).map { row.getCell(it)?.getCellValueAsString() ?: "" }

    private fun findHeaderRow(sheet: Sheet): Row? {
        for (i in 0..15) {
            val row = sheet.getRow(i) ?: continue
            if (!isRowEmpty(row)) return row
        }
        return null
    }

    private fun isRowEmpty(row: Row): Boolean {
        for (i in 0 until row.lastCellNum) {
            if (!row.getCell(i)?.getCellValueAsString()?.trim().isNullOrBlank()) return false
        }
        return true
    }

    private fun Cell.getCellValueAsString(): String = when (cellType) {
        CellType.STRING -> stringCellValue
        CellType.NUMERIC -> {
            if (DateUtil.isCellDateFormatted(this)) {
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(dateCellValue)
            } else {
                val num = numericCellValue
                if (num == Math.floor(num)) num.toLong().toString() else num.toString()
            }
        }
        CellType.BOOLEAN -> booleanCellValue.toString()
        else -> ""
    }
}
