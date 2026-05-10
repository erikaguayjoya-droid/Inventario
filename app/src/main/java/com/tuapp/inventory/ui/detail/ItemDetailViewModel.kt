package com.tuapp.inventory.ui.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.inventory.data.local.entity.InventoryItem
import com.tuapp.inventory.data.repository.InventoryRepository
import com.tuapp.inventory.util.pdf.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class ItemDetailUiState(
    val item:         InventoryItem?      = null,
    val atributos:    Map<String, String> = emptyMap(),
    val isLoading:    Boolean             = true,
    val isGenerating: Boolean             = false,
    val pdfUri:       Uri?                = null,
    val errorMessage: String?             = null
)

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val repository:   InventoryRepository,
    private val pdfGenerator: PdfGenerator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: Long = checkNotNull(savedStateHandle["itemId"])
    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    init { loadItem() }

    private fun loadItem() {
        viewModelScope.launch {
            repository.getAllItems()
                .map { list -> list.firstOrNull { it.id == itemId } }
                .distinctUntilChanged()
                .collect { item ->
                    _uiState.value = _uiState.value.copy(
                        item      = item,
                        atributos = item?.parseAtributos() ?: emptyMap(),
                        isLoading = false
                    )
                }
        }
    }

    fun generatePdf() {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            val uri = pdfGenerator.generateLabel(item)
            _uiState.value = _uiState.value.copy(
                isGenerating = false,
                pdfUri       = uri,
                errorMessage = if (uri == null) "No se pudo generar el PDF" else null
            )
        }
    }

    fun onPdfUriConsumed() { _uiState.value = _uiState.value.copy(pdfUri = null) }
    fun onErrorShown()     { _uiState.value = _uiState.value.copy(errorMessage = null) }

    private fun InventoryItem.parseAtributos(): Map<String, String> =
        try {
            Json.parseToJsonElement(atributosJson)
                .jsonObject
                .mapValues { it.value.jsonPrimitive.content }
        } catch (e: Exception) { emptyMap() }
}
