package com.tuapp.inventory.di

import android.content.Context
import com.tuapp.inventory.audit.AuditExporter
import com.tuapp.inventory.audit.AuditManager
import com.tuapp.inventory.data.local.dao.AuditorDao
import com.tuapp.inventory.data.local.dao.InventoryDao
import com.tuapp.inventory.data.local.database.InventoryDatabase
import com.tuapp.inventory.data.repository.InventoryRepository
import com.tuapp.inventory.domain.usecase.CompareInventoryUseCase
import com.tuapp.inventory.util.excel.ExcelManager
import com.tuapp.inventory.util.pdf.PdfGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): InventoryDatabase =
        InventoryDatabase.getInstance(ctx)

    @Provides @Singleton
    fun provideInventoryDao(db: InventoryDatabase): InventoryDao = db.inventoryDao()

    @Provides @Singleton
    fun provideAuditorDao(db: InventoryDatabase): AuditorDao = db.auditorDao()

    @Provides @Singleton
    fun provideRepository(
        inventoryDao: InventoryDao,
        auditorDao: AuditorDao
    ): InventoryRepository = InventoryRepository(inventoryDao, auditorDao)

    @Provides @Singleton
    fun provideExcelManager(
        @ApplicationContext ctx: Context,
        repository: InventoryRepository
    ): ExcelManager = ExcelManager(ctx, repository)

    @Provides @Singleton
    fun providePdfGenerator(@ApplicationContext ctx: Context): PdfGenerator = PdfGenerator(ctx)

    @Provides @Singleton
    fun provideAuditManager(
        @ApplicationContext ctx: Context,
        repository: InventoryRepository
    ): AuditManager = AuditManager(ctx, repository)

    @Provides @Singleton
    fun provideAuditExporter(@ApplicationContext ctx: Context): AuditExporter = AuditExporter(ctx)

    @Provides @Singleton
    fun provideCompareInventoryUseCase(repository: InventoryRepository): CompareInventoryUseCase =
        CompareInventoryUseCase(repository)
}
