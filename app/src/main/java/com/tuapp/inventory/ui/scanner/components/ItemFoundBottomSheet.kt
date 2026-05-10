package com.tuapp.inventory.ui.scanner.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuapp.inventory.data.local.entity.InventoryItem

// Flag temporal para ocultar funciones de PDF durante la demo
private const val SHOW_PDF_FEATURE = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFoundBottomSheet(
    item: InventoryItem,
    onViewDetail:  () -> Unit,
    onGeneratePdf: () -> Unit,
    onDismiss:     () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Activo encontrado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))

            LabeledValue("Nombre", item.nombre)
            LabeledValue("Área", item.area)
            LabeledValue("No. Activo", item.noActivo)
            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // El botón de "Ver detalle" permanece intacto
                OutlinedButton(
                    onClick = onViewDetail, 
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Info, null); Spacer(Modifier.width(6.dp)); Text("Ver detalle")
                }
                
                // Ocultamos el botón de PDF para la demo
                if (SHOW_PDF_FEATURE) {
                    Button(
                        onClick = onGeneratePdf, 
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(6.dp)); Text("PDF")
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
