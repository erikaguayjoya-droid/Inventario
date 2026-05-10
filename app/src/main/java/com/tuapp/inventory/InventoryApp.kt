package com.tuapp.inventory

import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application principal.
 * Extiende MultiDexApplication porque Apache POI supera el límite de 64k métodos.
 * @HiltAndroidApp dispara la generación de código de Hilt.
 */
@HiltAndroidApp
class InventoryApp : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
    }
}
