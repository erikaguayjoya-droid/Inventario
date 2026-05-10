package com.tuapp.inventory.audit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuapp.inventory.audit.model.AuditResult

@Composable
fun AuditSummaryHeader(result: AuditResult, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Resumen de Auditoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (result.isClean) {
                    SuggestionChip(onClick = {}, label = { Text("✅ Sin diferencias") })
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "DB: ${result.totalDb} ítems  ·  Excel: ${result.totalAudit} ítems",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CounterBadge(result.missing.size,   "Faltantes",   Color(0xFFB71C1C))
                CounterBadge(result.surplus.size,   "Sobrantes",   Color(0xFF1B5E20))
                CounterBadge(result.wrongArea.size, "Incorrectos", Color(0xFFE65100))
            }
        }
    }
}

@Composable
private fun CounterBadge(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = MaterialTheme.shapes.medium, color = color.copy(alpha = 0.12f)) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
