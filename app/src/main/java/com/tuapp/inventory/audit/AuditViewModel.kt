package com.tuapp.inventory.audit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.inventory.audit.model.AuditResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuditUiState {
    data object Idle : AuditUiState()
    data class Loading(val step: String) : AuditUiState()
    data class Done(val result: AuditResult) : AuditUiState()
    data class Error(val message: String) : AuditUiState()
}

data class AuditActionState(
    val isExporting: Boolean = false,
    val exportedUri: Uri?    = null,
    val exportError: String? = null
)

@HiltViewModel
class AuditViewModel @Inject constructor(
    private val auditManager:  AuditManager,
    private val auditExporter: AuditExporter
) : ViewModel() {

    private val _uiState     = MutableStateFlow<AuditUiState>(AuditUiState.Idle)
    private val _actionState = MutableStateFlow(AuditActionState())

    val uiState:     StateFlow<AuditUiState>     = _uiState.asStateFlow()
    val actionState: StateFlow<AuditActionState> = _actionState.asStateFlow()

    fun startAudit(uri: Uri) {
        viewModelScope.launch {
            auditManager.runAudit(uri).collect { state ->
                _uiState.value = when (state) {
                    is AuditState.ReadingExcel  -> AuditUiState.Loading("Leyendo Excel de auditoría…")
                    is AuditState.ComparingData -> AuditUiState.Loading("Comparando con la base de datos…")
                    is AuditState.Success       -> AuditUiState.Done(state.result)
                    is AuditState.Error         -> AuditUiState.Error(state.message)
                }
            }
        }
    }

    fun resetAudit() { _uiState.value = AuditUiState.Idle }

    fun exportCsv() = exportWith { auditExporter.exportCsv(it) }
    fun exportPdf() = exportWith { auditExporter.exportPdf(it) }

    private fun exportWith(exporter: suspend (AuditResult) -> Uri?) {
        val result = (_uiState.value as? AuditUiState.Done)?.result ?: return
        viewModelScope.launch {
            _actionState.value = _actionState.value.copy(isExporting = true)
            val uri = exporter(result)
            _actionState.value = _actionState.value.copy(
                isExporting = false,
                exportedUri = uri,
                exportError = if (uri == null) "No se pudo generar el archivo" else null
            )
        }
    }

    fun onExportedUriConsumed() { _actionState.value = _actionState.value.copy(exportedUri = null) }
    fun onExportErrorShown()    { _actionState.value = _actionState.value.copy(exportError = null) }
}
