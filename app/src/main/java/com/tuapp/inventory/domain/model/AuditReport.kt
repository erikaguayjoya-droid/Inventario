package com.tuapp.inventory.domain.model

import com.tuapp.inventory.data.local.entity.AuditorItem
import com.tuapp.inventory.data.local.entity.InventoryItem

data class AuditReport(
    val matches: List<AuditMatch>,
    val missing: List<AuditorItem>, // En lista del Auditor, pero no en Inventario Facultad
    val extras: List<InventoryItem> // En Inventario Facultad, pero no en lista del Auditor
)

data class AuditMatch(
    val inventoryItem: InventoryItem,
    val auditorItem: AuditorItem
)
