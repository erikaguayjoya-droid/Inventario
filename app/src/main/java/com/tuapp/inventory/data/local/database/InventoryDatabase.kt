package com.tuapp.inventory.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tuapp.inventory.data.local.dao.AuditorDao
import com.tuapp.inventory.data.local.dao.InventoryDao
import com.tuapp.inventory.data.local.entity.AuditorItem
import com.tuapp.inventory.data.local.entity.InventoryItem

@Database(
    entities = [InventoryItem::class, AuditorItem::class],
    version = 2,
    exportSchema = true
)
abstract class InventoryDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao
    abstract fun auditorDao(): AuditorDao

    companion object {
        private const val DATABASE_NAME = "inventory_database"

        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        fun getInstance(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): InventoryDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                InventoryDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}
