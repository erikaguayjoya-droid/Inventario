package com.tuapp.inventory.audit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuapp.inventory.audit.model.AuditItemDiff
import com.tuapp.inventory.audit.model.DiffStatus

@Composable
fun AuditDiffCard(diff: AuditItemDiff, modifier: Modifier = Modifier) {
    val accentColor = when (diff.status) {
        DiffStatus.MISSING    -> Color(0xFFB71C1C)
        DiffStatus.SURPLUS    -> Color(0xFF1B5E20)
        DiffStatus.WRONG_AREA -> Color(0xFFE65100)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Surface(
                modifier = Modifier.width(4.dp).fillMaxHeight(),
                color = accentColor, shape = MaterialTheme.shapes.small
            ) {}
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(diff.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Spacer(Modifier.height(2.dp))
                Text(diff.noActivo, style = MaterialTheme.typography.labelMedium, color = accentColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                when (diff.status) {
                    DiffStatus.MISSING    -> SmallLabel("Área registrada: ${diff.dbArea}")
                    DiffStatus.SURPLUS    -> SmallLabel("Área en Excel: ${diff.auditArea}")
                    DiffStatus.WRONG_AREA -> {
                        SmallLabel("DB: ${diff.dbArea}")
                        SmallLabel("Excel: ${diff.auditArea}", color = accentColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallLabel(text: String, color: Color = Color.Unspecified) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color
    )
}
