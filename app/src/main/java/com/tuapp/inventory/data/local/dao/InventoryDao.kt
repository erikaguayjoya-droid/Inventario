package com.tuapp.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tuapp.inventory.data.local.entity.InventoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InventoryItem>): List<Long>

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM inventory_items ORDER BY area ASC, nombre ASC")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items")
    suspend fun getAllItemsList(): List<InventoryItem>

    @Query("SELECT * FROM inventory_items WHERE area = :area ORDER BY nombre ASC")
    fun getItemsByArea(area: String): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE nombre LIKE :query ORDER BY nombre ASC")
    fun searchByNombre(query: String): Flow<List<InventoryItem>>

    /**
     * Búsqueda Ultra-Robusta:
     * Compara los códigos eliminando espacios en ambos lados de la moneda (DB y Parámetro).
     */
    @Query("SELECT * FROM inventory_items WHERE REPLACE(no_activo, ' ', '') = REPLACE(:noActivo, ' ', '') LIMIT 1")
    fun getItemByNoActivo(noActivo: String): Flow<InventoryItem?>

    /**
     * TAREA: Obtener categorías únicas filtrando nulos y vacíos.
     */
    @Query("SELECT DISTINCT area FROM inventory_items WHERE area IS NOT NULL AND area != '' ORDER BY area ASC")
    fun getDistinctAreas(): Flow<List<String>>
}
