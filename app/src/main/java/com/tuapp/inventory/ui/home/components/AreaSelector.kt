package com.tuapp.inventory.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AreaSelector(
    selectedArea: String, // Cambiado de String? a String para consistencia con el ViewModel
    availableAreas: List<String>,
    onAreaSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedArea,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir selector de área")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            availableAreas.forEachIndexed { index, area ->
                DropdownMenuItem(
                    text = {
                        Text(
                            area,
                            color = if (selectedArea == area) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = { 
                        onAreaSelected(area)
                        expanded = false 
                    }
                )
                // Poner una línea divisoria solo después de "General" para separar de las subcategorías
                if (area == "General" && availableAreas.size > 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}
