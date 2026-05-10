package com.tuapp.inventory.ui.scanner.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Componente que dibuja el marco de escaneo.
 * Utiliza CompositingStrategy.Offscreen para permitir que BlendMode.Clear 
 * perfore el overlay y muestre la cámara debajo.
 */
@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    val overlayColor = Color.Black.copy(alpha = 0.6f)
    val borderColor  = Color(0xFF2196F3)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val canvasW = size.width
        val canvasH = size.height
        
        // Definimos el tamaño del cuadro de escaneo (window)
        val windowW = canvasW * 0.75f
        val windowH = canvasH * 0.25f
        val windowX = (canvasW - windowW) / 2f
        val windowY = (canvasH - windowH) / 2f

        // 1. Dibujamos el fondo oscuro en toda la pantalla
        drawRect(color = overlayColor, size = Size(canvasW, canvasH))

        // 2. "Perforamos" el centro usando BlendMode.Clear
        // Gracias a graphicsLayer Offscreen, esto borrará el overlayColor
        // revelando lo que hay detrás (la cámara).
        drawRoundRect(
            color = Color.Transparent, 
            topLeft = Offset(windowX, windowY),
            size = Size(windowW, windowH), 
            cornerRadius = CornerRadius(12.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // 3. Dibujamos el borde azul del cuadro
        drawRoundRect(
            color = borderColor, 
            topLeft = Offset(windowX, windowY),
            size = Size(windowW, windowH), 
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )

        // 4. Dibujamos las esquinas decorativas blancas
        val cornerLen = 24.dp.toPx()
        listOf(
            Offset(windowX, windowY), 
            Offset(windowX + windowW, windowY),
            Offset(windowX, windowY + windowH), 
            Offset(windowX + windowW, windowY + windowH)
        ).forEach { corner ->
            val signX = if (corner.x == windowX) 1f else -1f
            val signY = if (corner.y == windowY) 1f else -1f
            
            drawLine(
                color = Color.White, 
                start = corner, 
                end = Offset(corner.x + signX * cornerLen, corner.y), 
                strokeWidth = 4.dp.toPx()
            )
            drawLine(
                color = Color.White, 
                start = corner, 
                end = Offset(corner.x, corner.y + signY * cornerLen), 
                strokeWidth = 4.dp.toPx()
            )
        }
    }
}
