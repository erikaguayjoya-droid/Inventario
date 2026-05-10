package com.tuapp.inventory.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tuapp.inventory.ui.home.components.*
import com.tuapp.inventory.util.excel.ExcelManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScanner: () -> Unit,
    onNavigateToAudit:   () -> Unit,
    onNavigateToDetail:  (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val internalPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onExcelFileSelected(it, ExcelManager.ImportType.INTERNAL) }
    }

    val auditorPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onExcelFileSelected(it, ExcelManager.ImportType.AUDITOR) }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            scope.launch { snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long) }
            viewModel.onUserMessageShown()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Inventario Facultad", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear activo")
                    }
                    IconButton(onClick = { 
                        internalPicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") 
                    }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Inventario Interno")
                    }
                    IconButton(onClick = { 
                        auditorPicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") 
                    }) {
                        Icon(Icons.Default.Checklist, contentDescription = "Inventario Auditoría")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor      = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            AreaSelector(
                selectedArea   = uiState.selectedArea,
                availableAreas = uiState.availableAreas,
                onAreaSelected = viewModel::onAreaSelected
            )
            InventorySearchBar(
                query          = uiState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged,
                visible        = uiState.selectedArea == "General"
            )

            // --- SECCIÓN DE REPORTE DE AUDITORÍA (Rehabilitada para la demo) ---
            val canGenerate = uiState.hasInternalData && uiState.hasAuditorData
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (canGenerate) MaterialTheme.colorScheme.secondaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (canGenerate) "¡Inventarios Listos!" else "Faltan datos para comparar",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (canGenerate) MaterialTheme.colorScheme.onSecondaryContainer 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.generateAuditReport { uri ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir Reporte"))
                            }
                        },
                        enabled = canGenerate && !isGeneratingReport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGeneratingReport) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Assignment, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Generar Reporte de Auditoría")
                        }
                    }
                    if (!canGenerate) {
                        Text(
                            text = "Cargue ambos inventarios para comparar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (uiState.items.isNotEmpty()) {
                Text(
                    text = "${uiState.items.size} artículo(s) en Facultad",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.items.isEmpty() && !uiState.isImporting) {
                    EmptyState(
                        selectedArea = uiState.selectedArea,
                        searchQuery  = uiState.searchQuery,
                        modifier     = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            InventoryItemCard(
                                item = item,
                                onClick = { onNavigateToDetail(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    ImportProgressDialog(progress = uiState.importProgress, visible = uiState.isImporting)
}

@Composable
private fun EmptyState(selectedArea: String, searchQuery: String, modifier: Modifier = Modifier) {
    val (emoji, title, subtitle) = when {
        searchQuery.isNotBlank() -> Triple("🔍", "Sin resultados", "No se encontraron artículos con \"$searchQuery\"")
        selectedArea != "General" -> Triple("📦", "Área vacía", "No hay artículos en \"$selectedArea\"")
        else                     -> Triple("📋", "Inventario vacío", "Importa los archivos Excel usando los botones de la barra superior")
    }
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
