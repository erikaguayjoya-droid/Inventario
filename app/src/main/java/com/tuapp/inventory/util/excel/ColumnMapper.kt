package com.tuapp.inventory.util.excel

import java.text.Normalizer

/**
 * Utilidad "Todoterreno" para la Facultad.
 * Detecta columnas principales y agrupa las demás como atributos extra.
 */
object ColumnMapper {

    private val ACTIVO_ALIASES = listOf("activo", "no. activo", "no.activo", "no activo", "inventario", "id")
    private val NOMBRE_ALIASES = listOf("descripción", "descripcion", "nombre", "articulo", "equipo", "objeto")
    private val AREA_ALIASES   = listOf("ubicacion", "ubicación", "area", "área", "departamento", "laboratorio")

    /**
     * Mapea las columnas necesarias y devuelve los índices de las columnas principales y extras.
     */
    fun mapColumns(headers: List<String>): ColumnMapResult {
        var activoIdx = -1
        var nombreIdx = -1
        var areaIdx   = -1
        val extraColumns = mutableMapOf<String, Int>()

        headers.forEachIndexed { index, header ->
            val cleanHeader = simplify(header)

            when {
                ACTIVO_ALIASES.contains(cleanHeader) && activoIdx == -1 -> activoIdx = index
                NOMBRE_ALIASES.contains(cleanHeader) && nombreIdx == -1 -> nombreIdx = index
                AREA_ALIASES.contains(cleanHeader)   && areaIdx == -1   -> areaIdx   = index
                else -> {
                    // Cualquier otra columna se guarda como extra
                    extraColumns[header] = index
                }
            }
        }

        return ColumnMapResult(activoIdx, nombreIdx, areaIdx, extraColumns)
    }

    private fun simplify(text: String): String {
        val temp = Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
        return temp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}

data class ColumnMapResult(
    val activoIdx: Int,
    val nombreIdx: Int,
    val areaIdx: Int,
    val extraColumns: Map<String, Int>
)
