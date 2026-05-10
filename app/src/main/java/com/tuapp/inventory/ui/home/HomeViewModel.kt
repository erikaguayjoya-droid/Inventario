package com.tuapp.inventory.ui.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.inventory.data.repository.InventoryRepository
import com.tuapp.inventory.domain.model.AuditReport
import com.tuapp.inventory.domain.usecase.CompareInventoryUseCase
import com.tuapp.inventory.util.excel.ExcelImportResult
import com.tuapp.inventory.util.excel.ExcelManager
import com.tuapp.inventory.util.pdf.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val excelManager: ExcelManager,
    private val compareUseCase: CompareInventoryUseCase,
    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    private val _selectedArea   = MutableStateFlow<String?>(null)
    private val _searchQuery    = MutableStateFlow("")
    private val _isImporting    = MutableStateFlow(false)
    private val _importProgress = MutableStateFlow<ExcelImportResult.Progress?>(null)
    private val _userMessage    = MutableStateFlow<String?>(null)
    private val _isGeneratingReport = MutableStateFlow(false)

    // Lógica de filtrado reactiva
    private val _items = combine(_selectedArea, _searchQuery.debounce(300)) { area, query ->
        Pair(area, query)
    }
        .distinctUntilChanged()
        .flatMapLatest { (area, query) ->
            when {
                // Si hay área seleccionada (y no es "General"), filtramos por área
                area != null && area != "General" -> repository.getItemsByArea(area)
                // Si estamos en "General" y hay búsqueda, buscamos por nombre
                query.isNotBlank() -> repository.searchByNombre(query)
                // Caso por defecto: todo el inventario
                else -> repository.getAllItems()
            }
        }

    val uiState: StateFlow<HomeUiState> = combine(
        _items,
        _selectedArea,
        repository.getDistinctAreas(), // Obtenemos áreas únicas desde el DAO
        _searchQuery,
        _isImporting,
        _importProgress,
        _userMessage,
        repository.getAllItems().map { it.isNotEmpty() },
        repository.getAllAuditorItemsFlow().map { it.isNotEmpty() }
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val dbAreas = values[2] as List<String>
        
        // TAREA: Construir lista de pestañas dinámicamente: "General" + áreas de la DB
        val allTabs = listOf("General") + dbAreas.filter { it.trim().uppercase() != "GENERAL" }

        HomeUiState(
            items           = values[0] as List<com.tuapp.inventory.data.local.entity.InventoryItem>,
            selectedArea    = values[1] as String? ?: "General", // Default a "General"
            availableAreas  = allTabs,
            searchQuery     = values[3] as String,
            isImporting     = values[4] as Boolean,
            importProgress  = values[5] as ExcelImportResult.Progress?,
            userMessage     = values[6] as String?,
            hasInternalData = values[7] as Boolean,
            hasAuditorData  = values[8] as Boolean
        )
    }
        .catch { e -> _userMessage.value = "Error al cargar datos: ${e.localizedMessage}" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val isGeneratingReport = _isGeneratingReport.asStateFlow()

    fun onAreaSelected(area: String?) {
        // Si seleccionan "General", seteamos null para habilitar búsqueda global
        _selectedArea.value = if (area == "General") null else area
        _searchQuery.value  = ""
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    fun onExcelFileSelected(uri: Uri, type: ExcelManager.ImportType, clearFirst: Boolean = true) {
        if (_isImporting.value) return
        viewModelScope.launch {
            excelManager.importFromUri(uri, type, clearFirst).collect { result ->
                when (result) {
                    is ExcelImportResult.Progress -> {
                        _isImporting.value = true; _importProgress.value = result
                    }
                    is ExcelImportResult.Success -> {
                        _isImporting.value = false; _importProgress.value = null
                        val base = "✅ ${result.totalItems} ítems importados"
                        _userMessage.value = if (result.unmappedSheets.isEmpty()) base
                        else "$base\n⚠️ No reconocidas: ${result.unmappedSheets.joinToString(", ")}"
                    }
                    is ExcelImportResult.Error -> {
                        _isImporting.value = false; _importProgress.value = null
                        _userMessage.value = "❌ ${result.message}"
                    }
                }
            }
        }
    }

    fun generateAuditReport(onComplete: (Uri) -> Unit) {
        if (_isGeneratingReport.value) return
        viewModelScope.launch {
            _isGeneratingReport.value = true
            try {
                val report = compareUseCase.execute()
                val uri = pdfGenerator.generateAuditReportPdf(report)
                if (uri != null) {
                    onComplete(uri)
                } else {
                    _userMessage.value = "❌ Error inesperado al generar el PDF"
                }
            } catch (e: Exception) {
                Log.e("DEBUG_AUDIT", "Error en generación de reporte", e)
                _userMessage.value = when (e) {
                    is IllegalArgumentException -> "❌ Error de permisos al compartir (FileProvider)"
                    is NullPointerException -> "❌ Faltan datos críticos para realizar la comparación"
                    is OutOfMemoryError -> "❌ El reporte es demasiado grande para la memoria del dispositivo"
                    else -> "❌ Error crítico: ${e.localizedMessage ?: "Consulte los logs"}"
                }
            } finally {
                _isGeneratingReport.value = false
            }
        }
    }

    fun onUserMessageShown() { _userMessage.value = null }
}
