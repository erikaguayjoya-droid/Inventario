package com.tuapp.inventory.ui.scanner

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.inventory.data.local.entity.InventoryItem
import com.tuapp.inventory.data.repository.InventoryRepository
import com.tuapp.inventory.util.pdf.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannerUiState(
    val foundItem:    InventoryItem? = null,
    val isSearching:  Boolean        = false,
    val notFound:     Boolean        = false,
    val pdfUri:       Uri?           = null,
    val errorMessage: String?        = null
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository:   InventoryRepository,
    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var lastScannedCode = ""

    fun onBarcodeDetected(rawValue: String) {
        if (rawValue == lastScannedCode || _uiState.value.isSearching) return
        lastScannedCode = rawValue

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, notFound = false)
            val item = repository.getItemByNoActivo(rawValue).first()
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                foundItem   = item,
                notFound    = item == null
            )
        }
    }

    fun generatePdf() {
        val item = _uiState.value.foundItem ?: return
        viewModelScope.launch {
            val uri = pdfGenerator.generateLabel(item)
            _uiState.value = _uiState.value.copy(
                pdfUri       = uri,
                errorMessage = if (uri == null) "No se pudo generar el PDF" else null
            )
        }
    }

    fun resumeScanning() {
        lastScannedCode = ""
        _uiState.value  = ScannerUiState()
    }

    fun onErrorShown()      { _uiState.value = _uiState.value.copy(errorMessage = null) }
    fun onPdfUriConsumed()  { _uiState.value = _uiState.value.copy(pdfUri = null) }
}
