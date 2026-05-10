package com.tuapp.inventory.audit

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tuapp.inventory.audit.model.AuditItemDiff
import com.tuapp.inventory.audit.model.AuditResult
import com.tuapp.inventory.audit.model.DiffStatus
import com.tuapp.inventory.data.local.entity.InventoryItem
import com.tuapp.inventory.data.repository.InventoryRepository
import com.tuapp.inventory.util.excel.ColumnMapper
import com.tuapp.inventory.util.normalizer.AreaNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuditManager"

sealed class AuditState {
    data object ReadingExcel  : AuditState()
    data object ComparingData : AuditState()
    data class  Success(val result: AuditResult) : AuditState()
    data class  Error(val message: String, val cause: Throwable? = null) : AuditState()
}

@Singleton
class AuditManager @Inject constructor(
    private val context:    Context,
    private val repository: InventoryRepository
) {
    fun runAudit(uri: Uri): Flow<AuditState> = flow {
        try {
            emit(AuditState.ReadingExcel)
            val auditItems: Map<String, InventoryItem> = withContext(Dispatchers.IO) {
                readExcelToMap(uri)
            }

            emit(AuditState.ComparingData)
            val dbItems: Map<String, InventoryItem> = withContext(Dispatchers.IO) {
                repository.getAllItems().first().associateBy { it.noActivo.trim() }
            }

            val result = withContext(Dispatchers.Default) { compare(dbItems, auditItems) }
            emit(AuditState.Success(result))

        } catch (e: Exception) {
            Log.e(TAG, "Error en auditoría", e)
            emit(AuditState.Error("Error al procesar la auditoría: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)

    private fun compare(
        dbItems:    Map<String, InventoryItem>,
        auditItems: Map<String, InventoryItem>
    ): AuditResult {
        val missing   = mutableListOf<AuditItemDiff>()
        val surplus   = mutableListOf<AuditItemDiff>()
        val wrongArea = mutableListOf<AuditItemDiff>()

        for ((noActivo, dbItem) in dbItems) {
            val auditItem = auditItems[noActivo]
            when {
                auditItem == null -> missing.add(
                    AuditItemDiff(noActivo, dbItem.nombre, DiffStatus.MISSING, dbItem.area, null)
                )
                !areasMatch(dbItem.area, auditItem.area) -> wrongArea.add(
                    AuditItemDiff(noActivo, dbItem.nombre, DiffStatus.WRONG_AREA, dbItem.area, auditItem.area)
                )
            }
        }

        for ((noActivo, auditItem) in auditItems) {
            if (!dbItems.containsKey(noActivo)) {
                surplus.add(AuditItemDiff(noActivo, auditItem.nombre, DiffStatus.SURPLUS, null, auditItem.area))
            }
        }

        return AuditResult(
            missing    = missing.sortedBy { it.nombre },
            surplus    = surplus.sortedBy { it.nombre },
            wrongArea  = wrongArea.sortedBy { it.nombre },
            totalDb    = dbItems.size,
            totalAudit = auditItems.size
        )
    }

    private fun areasMatch(dbArea: String, auditArea: String): Boolean =
        AreaNormalizer.normalize(dbArea) == AreaNormalizer.normalize(auditArea)

    private fun readExcelToMap(uri: Uri): Map<String, InventoryItem> {
        val result = mutableMapOf<String, InventoryItem>()
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("No se pudo abrir el archivo de auditoría.")

        inputStream.use { stream ->
            val workbook = WorkbookFactory.create(stream)
            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                val area  = AreaNormalizer.matchArea(sheet.sheetName.trim()) ?: sheet.sheetName.trim()

                val headerRow = findHeaderRow(sheet) ?: continue

                val headers = (0 until headerRow.lastCellNum).map { cellIdx ->
                    headerRow.getCell(cellIdx)?.getCellValueAsString() ?: ""
                }

                // --- CAMBIO CLAVE AQUÍ: Usamos mapColumns ---
                val colResult = ColumnMapper.mapColumns(headers)
                if (colResult.activoIdx == -1) continue

                for (rowIdx in (headerRow.rowNum + 1)..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIdx) ?: continue
                    val noActivo = row.getCell(colResult.activoIdx)
                        ?.getCellValueAsString()?.trim() ?: continue
                    if (noActivo.isBlank()) continue

                    val nombre = if (colResult.nombreIdx != -1)
                        row.getCell(colResult.nombreIdx)?.getCellValueAsString()?.trim() ?: noActivo
                    else noActivo

                    result[noActivo] = InventoryItem(area = area, noActivo = noActivo, nombre = nombre)
                }
            }
            workbook.close()
        }
        return result
    }

    private fun findHeaderRow(sheet: org.apache.poi.ss.usermodel.Sheet): org.apache.poi.ss.usermodel.Row? {
        for (rowIndex in 0..minOf(15, sheet.lastRowNum)) {
            val row = sheet.getRow(rowIndex) ?: continue
            if (!isRowEmpty(row)) return row
        }
        return null
    }

    private fun isRowEmpty(row: org.apache.poi.ss.usermodel.Row): Boolean {
        for (cellIndex in 0 until row.lastCellNum) {
            val value = row.getCell(cellIndex)?.getCellValueAsString()?.trim()
            if (!value.isNullOrBlank()) return false
        }
        return true
    }

    private fun Cell.getCellValueAsString(): String = when (cellType) {
        CellType.STRING  -> stringCellValue.trim()
        CellType.NUMERIC -> {
            val n = numericCellValue
            if (n == kotlin.math.floor(n)) n.toLong().toString() else n.toString()
        }
        CellType.BOOLEAN -> booleanCellValue.toString()
        CellType.FORMULA -> try {
            when(cachedFormulaResultType) {
                CellType.NUMERIC -> numericCellValue.toLong().toString()
                else -> stringCellValue
            }
        } catch (e: Exception) { "" }
        else -> ""
    }
}