package com.example.golfdistancetracker.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.golfdistancetracker.data.entity.Shot
import com.example.golfdistancetracker.data.entity.ShotType

@Composable
fun ShotHeatmap(shots: List<Shot>) {
    Box(
        modifier = Modifier.fillMaxWidth().height(160.dp).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            
            // Draw background grid/circles
            drawCircle(color = Color.LightGray.copy(alpha = 0.3f), radius = radius, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            drawCircle(color = Color.LightGray.copy(alpha = 0.3f), radius = radius * 0.66f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            drawCircle(color = Color.LightGray.copy(alpha = 0.3f), radius = radius * 0.33f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            
            // Divide into 7x7 grid
            val gridSize = 7
            val cellSize = size.width / gridSize
            val density = Array(gridSize) { IntArray(gridSize) }

            shots.forEach { shot ->
                val xNorm = if (shot.shotType == ShotType.FIELD) {
                    val latDev = shot.lateralDeviation ?: 0.0
                    (latDev.toFloat() / 20f).coerceIn(-1f, 1f)
                } else {
                    val dev = shot.deviation ?: 0f
                    (dev / 2f).coerceIn(-1f, 1f)
                }
                
                val yNorm = if (shot.shotType == ShotType.FIELD) {
                    0f // Field shots only have horizontal dev for now in this view
                } else {
                    val qual = shot.quality ?: 1
                    (1 - qual).toFloat() // -1, 0, 1
                }

                // Map norm (-1 to 1) to grid index (0 to 6)
                val gridX = ((xNorm + 1f) / 2f * (gridSize - 1)).toInt().coerceIn(0, gridSize - 1)
                val gridY = ((yNorm + 1f) / 2f * (gridSize - 1)).toInt().coerceIn(0, gridSize - 1)
                density[gridX][gridY]++
            }

            val maxDensity = density.maxOfOrNull { it.maxOrNull() ?: 1 } ?: 1

            // Draw heatmap cells
            for (i in 0 until gridSize) {
                for (j in 0 until gridSize) {
                    val count = density[i][j]
                    if (count > 0) {
                        val alpha = (count.toFloat() / maxDensity).coerceIn(0.1f, 0.8f)
                        val color = lerpColor(Color.Blue.copy(alpha = 0.1f), Color.Red, count.toFloat() / maxDensity)
                        
                        drawRect(
                            color = color.copy(alpha = alpha),
                            topLeft = Offset(i * cellSize, j * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}
