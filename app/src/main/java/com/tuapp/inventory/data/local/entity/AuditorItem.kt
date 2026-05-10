package com.tuapp.inventory.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para los items provenientes del listado del Auditor.
 * Aislada del inventario interno para permitir comparaciones.
 */
@Entity(tableName = "auditor_items")
data class AuditorItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "descripcion")
    val descripcion: String,

    @ColumnInfo(name = "resguardante")
    val resguardante: String,

    @ColumnInfo(name = "activo")
    val activo: String, // ID de barras / No. Inventario

    @ColumnInfo(name = "ubicacion")
    val ubicacion: String
)
