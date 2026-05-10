package com.tuapp.inventory.audit.model

data class AuditItemDiff(
    val noActivo:  String,
    val nombre:    String,
    val status:    DiffStatus,
    val dbArea:    String? = null,
    val auditArea: String? = null
) {
    fun descriptionLine(): String = when (status) {
        DiffStatus.MISSING    -> "FALTANTE | $noActivo | $nombre | DB: $dbArea"
        DiffStatus.SURPLUS    -> "SOBRANTE | $noActivo | $nombre | Excel: $auditArea"
        DiffStatus.WRONG_AREA -> "UBICACIÓN INCORRECTA | $noActivo | $nombre | DB: $dbArea | Excel: $auditArea"
    }
}

enum class DiffStatus {
    MISSING,
    SURPLUS,
    WRONG_AREA
}

data class AuditResult(
    val missing:    List<AuditItemDiff>,
    val surplus:    List<AuditItemDiff>,
    val wrongArea:  List<AuditItemDiff>,
    val totalDb:    Int,
    val totalAudit: Int,
    val auditedAt:  Long = System.currentTimeMillis()
) {
    val totalDiffs: Int     get() = missing.size + surplus.size + wrongArea.size
    val isClean:    Boolean get() = totalDiffs == 0
    val allDiffs:   List<AuditItemDiff> get() = missing + surplus + wrongArea
}
