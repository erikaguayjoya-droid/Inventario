package com.tuapp.inventory.audit.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tuapp.inventory.audit.AuditUiState
import com.tuapp.inventory.audit.AuditViewModel
import com.tuapp.inventory.audit.model.AuditItemDiff
import com.tuapp.inventory.audit.model.AuditResult
import com.tuapp.inventory.audit.model.DiffStatus
import com.tuapp.inventory.audit.ui.components.AuditDiffCard
import com.tuapp.inventory.audit.ui.components.AuditSummaryHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditReportScreen(
    initialUri:     Uri?,
    onNavigateBack: () -> Unit,
    viewModel: AuditViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val context     = LocalContext.current
    val snackbar    = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(initialUri) { initialUri?.let { viewModel.startAudit(it) } }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.startAudit(it) }
    }

    LaunchedEffect(actionState.exportedUri) {
        actionState.exportedUri?.let { uri ->
            val mime = if (uri.toString().endsWith(".pdf")) "application/pdf" else "text/csv"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime; putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
            viewModel.onExportedUriConsumed()
        }
    }

    LaunchedEffect(actionState.exportError) {
        actionState.exportError?.let { snackbar.showSnackbar(it); viewModel.onExportErrorShown() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Auditoría de Inventario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (uiState is AuditUiState.Done) {
                        IconButton(onClick = viewModel::resetAudit) {
                            Icon(Icons.Default.Refresh, "Nueva auditoría")
                        }
                    }
                }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "audit_state",
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { state ->
            when (state) {
                AuditUiState.Idle -> IdleContent(onSelectFile = {
                    filePicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                })
                is AuditUiState.Loading -> LoadingContent(state.step)
                is AuditUiState.Error   -> ErrorContent(state.message) {
                    viewModel.resetAudit()
                    filePicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                }
                is AuditUiState.Done -> ReportContent(
                    result        = state.result,
                    selectedTab   = selectedTab,
                    onTabSelected = { selectedTab = it },
                    isExporting   = actionState.isExporting,
                    onExportCsv   = viewModel::exportCsv,
                    onExportPdf   = viewModel::exportPdf
                )
            }
        }
    }
}

@Composable
private fun IdleContent(onSelectFile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📋", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(24.dp))
        Text("Motor de Auditoría", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Selecciona el Excel de conteo físico. Lo compararemos con el inventario sin modificar la base de datos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onSelectFile, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp))
            Text("Seleccionar Excel de auditoría")
        }
    }
}

@Composable
private fun LoadingContent(step: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text(step, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⚠️", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

@Composable
private fun ReportContent(
    result: AuditResult, selectedTab: Int, onTabSelected: (Int) -> Unit,
    isExporting: Boolean, onExportCsv: () -> Unit, onExportPdf: () -> Unit
) {
    data class TabData(val title: String, val items: List<AuditItemDiff>, val color: Color, val emoji: String)
    val tabs = listOf(
        TabData("Faltantes",   result.missing,   Color(0xFFB71C1C), "📭"),
        TabData("Sobrantes",   result.surplus,   Color(0xFF1B5E20), "📦"),
        TabData("Incorrectos", result.wrongArea, Color(0xFFE65100), "📍")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        AuditSummaryHeader(result = result, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExportCsv, enabled = !isExporting, modifier = Modifier.weight(1f)) {
                if (isExporting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.TableChart, null)
                Spacer(Modifier.width(6.dp)); Text("CSV")
            }
            Button(onClick = onExportPdf, enabled = !isExporting, modifier = Modifier.weight(1f)) {
                if (isExporting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Icon(Icons.Default.PictureAsPdf, null)
                Spacer(Modifier.width(6.dp)); Text("PDF")
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
            tabs.forEachIndexed { index, tab ->
                Tab(selected = selectedTab == index, onClick = { onTabSelected(index) }, text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tab.emoji); Text(tab.title)
                        if (tab.items.isNotEmpty()) Badge { Text(tab.items.size.toString()) }
                    }
                })
            }
        }

        val currentDiffs = tabs[selectedTab].items
        if (currentDiffs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("Sin diferencias en esta categoría", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(currentDiffs, key = { it.noActivo }) { diff -> AuditDiffCard(diff = diff) }
            }
        }
    }
}
