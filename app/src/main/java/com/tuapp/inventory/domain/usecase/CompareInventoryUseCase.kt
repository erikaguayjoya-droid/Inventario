package com.tuapp.inventory.domain.usecase

import android.util.Log
import com.tuapp.inventory.data.repository.InventoryRepository
import com.tuapp.inventory.domain.model.AuditMatch
import com.tuapp.inventory.domain.model.AuditReport
import javax.inject.Inject

/**
 * UseCase para cruzar los datos del Inventario Interno vs Listado del Auditor.
 */
class CompareInventoryUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend fun execute(): AuditReport {
        val internalItems = repository.getAllItemsList()
        val auditorItems = repository.getAllAuditorItems()

        Log.d("DEBUG_AUDIT", "Iniciando comparación con normalización robusta...")
        Log.d("DEBUG_AUDIT", "Items Internos (Facultad): ${internalItems.size}")
        Log.d("DEBUG_AUDIT", "Items Auditoría (Externo): ${auditorItems.size}")

        val internalMap = internalItems.associateBy { it.noActivo.normalizeActivoId() }
        val auditorMap = auditorItems.associateBy { it.activo.normalizeActivoId() }

        val matches = mutableListOf<AuditMatch>()
        val missing = mutableListOf<com.tuapp.inventory.data.local.entity.AuditorItem>()
        val extras = mutableListOf<com.tuapp.inventory.data.local.entity.InventoryItem>()

        // 1. Buscar Matches y Faltantes (Lo que el auditor dice que debe estar)
        auditorItems.forEach { auditorItem ->
            val key = auditorItem.activo.normalizeActivoId()
            if (key.isBlank()) return@forEach // Ignorar si el ID normalizado queda vacío

            val match = internalMap[key]
            if (match != null) {
                matches.add(AuditMatch(match, auditorItem))
            } else {
                missing.add(auditorItem)
            }
        }

        // 2. Buscar Sobrantes (Lo que nosotros tenemos pero el auditor no listó)
        internalItems.forEach { internalItem ->
            val key = internalItem.noActivo.normalizeActivoId()
            if (key.isBlank()) return@forEach

            if (!auditorMap.containsKey(key)) {
                extras.add(internalItem)
            }
        }

        Log.d("DEBUG_AUDIT", "Comparación finalizada: Matches=${matches.size}, Faltantes=${missing.size}, Extras=${extras.size}")
        return AuditReport(matches, missing, extras)
    }

    /**
     * Limpieza robusta de IDs para evitar falsos positivos por caracteres basura o etiquetas S/E.
     */
    private fun String.normalizeActivoId(): String {
        return this.uppercase()
            .replace(Regex("\\s+"), "") // Elimina espacios, tabs y saltos de línea
            .replace(Regex("[\\(\\)\\[\\]\\\$]*S/E[\\(\\)\\[\\]\\\$]*"), "") // Elimina variants de S/E y símbolos adyacentes
            .replace("-", "") // Mantenemos la eliminación de guiones para consistencia
    }
}
