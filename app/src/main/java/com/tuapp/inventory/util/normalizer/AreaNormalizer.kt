package com.tuapp.inventory.util.normalizer

import java.text.Normalizer
import kotlin.math.min

object AreaNormalizer {

    val OFFICIAL_AREAS = listOf(
        "General", "Hidráulica", "B1", "Impresión 3D", "Pasillos",
        "Motores eléctricos", "PLC", "CIM", "Eléctrica", "Dinámica",
        "Taller de prototipos", "Taller pesado", "Electrónica",
        "Aire acondicionado", "Metrología"
    )

    /**
     * Encuentra el área oficial más cercana al nombre de la hoja usando Distancia de Levenshtein.
     */
    fun getClosestArea(sheetName: String): String {
        val normalizedInput = normalize(sheetName)
        
        // 1. Intento de coincidencia exacta tras normalización
        OFFICIAL_AREAS.find { normalize(it) == normalizedInput }?.let { return it }

        // 2. Fuzzy Matching usando Levenshtein
        return OFFICIAL_AREAS.minByOrNull { levenshtein(normalizedInput, normalize(it)) } ?: "General"
    }

    fun matchArea(sheetName: String): String? {
        val normalizedInput = normalize(sheetName)
        return OFFICIAL_AREAS.find { normalize(it) == normalizedInput }
    }

    fun normalize(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "") // Elimina puntos, guiones, espacios
            .trim()
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }
}
