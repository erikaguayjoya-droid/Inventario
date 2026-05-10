package com.tuapp.inventory.util.excel

import com.tuapp.inventory.data.local.entity.InventoryItem
import com.tuapp.inventory.util.normalizer.AreaNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.poi.ss.usermodel.*
import java.io.InputStream
import java.text.Normalizer

/**
 * Lector de Excel optimizado para el inventario de la facultad.
 * Implementa detección de encabezados y normalización de áreas.
 */
class ExcelParser {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    suspend fun parseInventory(inputStream: InputStream): List<InventoryItem> = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<InventoryItem>()
        
        try {
            // WorkbookFactory.create es seguro para .xls y .xlsx
            val workbook = WorkbookFactory.create(inputStream)
            
            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                if (sheet.lastRowNum < 0) continue // Hoja vacía

                // 1. Normalización del área (Fuzzy Matching)
                val areaName = AreaNormalizer.getClosestArea(sheet.sheetName)

                // 2. Localizar fila de encabezados
                val headerRowIndex = findHeaderRow(sheet, "no.activo") ?: continue
                val headerRow = sheet.getRow(headerRowIndex)
                
                // 3. Mapear columnas
                val headers = getHeaders(headerRow)
                val columnMap = ColumnMapper.mapColumns(headers)
                
                if (columnMap.activoIdx == -1) continue // No se encontró la columna ID

                // 4. Procesar datos
                for (rowIndex in (headerRowIndex + 1)..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    if (isRowEmpty(row)) continue

                    val noActivo = getCellValue(row.getCell(columnMap.activoIdx))?.trim() ?: ""
                    if (noActivo.isBlank() || noActivo.uppercase() == "S/E") continue

                    // Extraer atributos extra (columnas no mapeadas a campos fijos)
                    val extras = mutableMapOf<String, String>()
                    columnMap.extraColumns.forEach { (name, idx) ->
                        getCellValue(row.getCell(idx))?.trim()?.let { 
                            if (it.isNotBlank()) extras[name] = it 
                        }
                    }

                    allItems.add(InventoryItem(
                        area = areaName,
                        noActivo = noActivo,
                        nombre = getCellValue(row.getCell(columnMap.nombreIdx))?.trim() ?: "Item $noActivo",
                        atributosJson = json.encodeToString(extras)
                    ))
                }
            }
            workbook.close()
        } catch (e: Exception) {
            android.util.Log.e("ExcelParser", "Error fatal parseando Excel", e)
            throw Exception("No se pudo leer el archivo Excel. Asegúrese de que no esté protegido por contraseña o corrupto.")
        }
        
        return@withContext allItems
    }

    private fun findHeaderRow(sheet: Sheet, target: String): Int? {
        // Escaneamos las primeras 20 filas buscando la palabra clave
        for (i in 0..19) {
            val row = sheet.getRow(i) ?: continue
            for (j in 0 until row.lastCellNum) {
                val value = simplify(getCellValue(row.getCell(j)) ?: "")
                if (value.contains(target)) return i
            }
        }
        return null
    }

    private fun getHeaders(row: Row): List<String> =
        (0 until row.lastCellNum).map { getCellValue(row.getCell(it)) ?: "" }

    private fun isRowEmpty(row: Row): Boolean {
        for (i in 0 until row.lastCellNum) {
            if (!getCellValue(row.getCell(i))?.trim().isNullOrBlank()) return false
        }
        return true
    }

    private fun getCellValue(cell: Cell?): String? {
        if (cell == null) return null
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(cell.dateCellValue)
                } else {
                    val num = cell.numericCellValue
                    if (num == Math.floor(num)) num.toLong().toString() else num.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    cell.stringCellValue
                } catch (e: Exception) {
                    cell.numericCellValue.toString()
                }
            }
            else -> null
        }
    }

    private fun simplify(text: String): String {
        val temp = Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
        return temp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}
