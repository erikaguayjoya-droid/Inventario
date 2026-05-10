package com.tuapp.inventory.util.excel

sealed class ExcelImportResult {

    data class Success(
        val totalItems: Int,
        val sheetsSummary: Map<String, Int>,
        val unmappedSheets: List<String> = emptyList()
    ) : ExcelImportResult()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ExcelImportResult()

    data class Progress(
        val currentSheet: String,
        val processedSheets: Int,
        val totalSheets: Int
    ) : ExcelImportResult()
}
