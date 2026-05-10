package com.tuapp.inventory.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tuapp.inventory.util.excel.ExcelImportResult

@Composable
fun ImportProgressDialog(
    progress: ExcelImportResult.Progress?,
    visible: Boolean
) {
    if (!visible) return

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(8.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(20.dp))
                Text("Importando inventario…", style = MaterialTheme.typography.titleMedium)

                if (progress != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Procesando: ${progress.currentSheet}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    val fraction = if (progress.totalSheets > 0)
                        progress.processedSheets.toFloat() / progress.totalSheets else 0f
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Hoja ${progress.processedSheets + 1} de ${progress.totalSheets}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Preparando archivo…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
