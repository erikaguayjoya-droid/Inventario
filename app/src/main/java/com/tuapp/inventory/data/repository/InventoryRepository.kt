package com.tuapp.inventory.data.repository

import android.util.Log
import com.tuapp.inventory.data.local.dao.AuditorDao
import com.tuapp.inventory.data.local.dao.InventoryDao
import com.tuapp.inventory.data.local.entity.AuditorItem
import com.tuapp.inventory.data.local.entity.InventoryItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val auditorDao: AuditorDao
) {
    // --- Lógica de Inventario Interno ---
    suspend fun insertItems(items: List<InventoryItem>): List<Long> = inventoryDao.insertAll(items)
    suspend fun clearAllItems() = inventoryDao.deleteAll()
    fun getAllItems(): Flow<List<InventoryItem>> = inventoryDao.getAllItems()
    suspend fun getAllItemsList(): List<InventoryItem> = inventoryDao.getAllItemsList()
    fun getItemsByArea(area: String): Flow<List<InventoryItem>> = inventoryDao.getItemsByArea(area)
    fun searchByNombre(query: String): Flow<List<InventoryItem>> = inventoryDao.searchByNombre("%$query%")
    
    fun getItemByNoActivo(noActivo: String): Flow<InventoryItem?> {
        val cleanCode = noActivo.trim()
            .replace("—", "-")
            .replace("–", "-")
            .replace(" ", "")
        return inventoryDao.getItemByNoActivo(cleanCode)
    }

    fun getDistinctAreas(): Flow<List<String>> = inventoryDao.getDistinctAreas()

    // --- Lógica de Auditoría ---
    suspend fun insertAuditorItems(items: List<AuditorItem>) = auditorDao.insertAllAuditorItems(items)
    suspend fun clearAuditorTable() = auditorDao.clearAuditorTable()
    suspend fun getAllAuditorItems(): List<AuditorItem> = auditorDao.getAllAuditorItems()
    fun getAllAuditorItemsFlow(): Flow<List<AuditorItem>> = auditorDao.getAllAuditorItemsFlow()
}
