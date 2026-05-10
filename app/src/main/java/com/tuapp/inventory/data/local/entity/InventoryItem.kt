package com.tuapp.inventory.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad principal del inventario.
 * @param id          Clave primaria autogenerada.
 * @param area        Área o laboratorio al que pertenece el activo.
 * @param noActivo    Número de activo único; se usa como código de barras.
 * @param nombre      Nombre descriptivo del activo.
 * @param atributosJson JSON con columnas dinámicas del Excel. Default: "{}".
 */
@Entity(
    tableName = "inventory_items",
    indices = [Index(value = ["no_activo"], unique = true)]
)
data class InventoryItem(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "area")
    val area: String,

    @ColumnInfo(name = "no_activo")
    val noActivo: String,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "atributos_json")
    val atributosJson: String = "{}"
)
