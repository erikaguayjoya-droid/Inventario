package com.tuapp.inventory.data.local.dao

import androidx.room.*
import com.tuapp.inventory.data.local.entity.AuditorItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAuditorItems(items: List<AuditorItem>)

    @Query("DELETE FROM auditor_items")
    suspend fun clearAuditorTable()

    @Query("SELECT * FROM auditor_items")
    suspend fun getAllAuditorItems(): List<AuditorItem>

    @Query("SELECT * FROM auditor_items")
    fun getAllAuditorItemsFlow(): Flow<List<AuditorItem>>
}
