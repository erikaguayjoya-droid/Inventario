package com.tuapp.inventory.ui.scanner

import android.content.Intent
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.tuapp.inventory.ui.scanner.components.*
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalGetImage::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun ScannerScreen(
    onNavigateBack:     () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    // Hilo dedicado para el análisis (Evita trabar la UI)
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(uiState.pdfUri) {
        uiState.pdfUri?.let { uri ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Abrir etiqueta PDF"))
            viewModel.onPdfUriConsumed()
        }
    }

    // Configuración de ALTA SENSIBILIDAD para ML Kit
    val scannerOptions = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39) // Lee de todo (QR, CODE_128, EAN, etc)
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(scannerOptions) }

    val barcodeAnalyzer = remember {
        ImageAnalysis.Analyzer { imageProxy ->
            Log.d("PRUEBA_SCAN", "Detectando algo... (Frame recibido)")
            
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            // Loguea TODOS los que detecta
                            barcodes.forEach { barcode ->
                                Log.d("PRUEBA_SCAN", "Código encontrado: ${barcode.rawValue} | Formato: ${barcode.format}")
                            }

                            val rawValue = barcodes[0].rawValue
                            rawValue?.let { viewModel.onBarcodeDetected(it) }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("PRUEBA_SCAN", "Error en ML Kit: ${e.message}")
                    }
                    .addOnCompleteListener {
                        // CIERRE VITAL: Cerramos el proxy DESPUÉS de que ML Kit termine
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    // Limpieza al cerrar la pantalla
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Escanear activo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.3f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !cameraPermission.status.isGranted -> {
                    CameraPermissionRequest(
                        shouldShowRationale = cameraPermission.status.shouldShowRationale,
                        onRequestPermission = { cameraPermission.launchPermissionRequest() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    CameraPreview(
                        analyzer = barcodeAnalyzer, 
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    ScannerOverlay(modifier = Modifier.fillMaxSize())

                    if (uiState.foundItem == null && !uiState.isSearching) {
                        Text(
                            text  = "Apunta al código de barras del activo",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 48.dp)
                        )
                    }

                    if (uiState.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    if (uiState.notFound) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Activo no registrado en el inventario",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = viewModel::resumeScanning) { Text("OK") }
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.foundItem?.let { item ->
        ItemFoundBottomSheet(
            item          = item,
            onViewDetail  = { onNavigateToDetail(item.id) },
            onGeneratePdf = viewModel::generatePdf,
            onDismiss     = viewModel::resumeScanning
        )
    }
}

@Composable
private fun CameraPermissionRequest(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📷", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (shouldShowRationale)
                "La cámara es necesaria para escanear códigos de barras de los activos."
            else "Se necesita permiso de cámara para usar el escáner.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) { Text("Conceder permiso") }
    }
}
