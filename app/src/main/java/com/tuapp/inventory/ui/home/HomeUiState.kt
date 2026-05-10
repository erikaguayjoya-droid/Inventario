package com.tuapp.inventory.ui.home

import com.tuapp.inventory.data.local.entity.InventoryItem
import com.tuapp.inventory.util.excel.ExcelImportResult

data class HomeUiState(
    val items: List<InventoryItem> = emptyList(),
    val selectedArea: String = "General",
    val availableAreas: List<String> = emptyList(),
    val searchQuery: String = "",
    val isImporting: Boolean = false,
    val importProgress: ExcelImportResult.Progress? = null,
    val userMessage: String? = null,
    val hasInternalData: Boolean = false,
    val hasAuditorData: Boolean = false
)
