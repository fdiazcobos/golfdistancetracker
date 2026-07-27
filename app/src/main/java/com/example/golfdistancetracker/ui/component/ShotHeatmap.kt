package com.example.golfdistancetracker.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.golfdistancetracker.data.entity.Shot
import com.example.golfdistancetracker.data.entity.ShotType

@Composable
fun ShotHeatmap(shots: List<Shot>) {
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            
            // Draw background target rings
            drawCircle(color = Color.LightGray.copy(alpha = 0.2f), radius = radius, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            drawCircle(color = Color.LightGray.copy(alpha = 0.2f), radius = radius * 0.66f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            drawCircle(color = Color.LightGray.copy(alpha = 0.2f), radius = radius * 0.33f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            
            // Internal high-res grid for density calculation
            val gridSize = 20
            val cellSize = size.width / gridSize
            val density = Array(gridSize) { FloatArray(gridSize) }

            shots.forEach { shot ->
                val xNorm = if (shot.shotType == ShotType.FIELD) {
                    val latDev = shot.lateralDeviation ?: 0.0
                    (latDev.toFloat() / 20f).coerceIn(-1f, 1f)
                } else {
                    val dev = shot.deviation ?: 0f
                    (dev / 2f).coerceIn(-1f, 1f)
                }
                
                val yNorm = if (shot.shotType == ShotType.FIELD) {
                    0f 
                } else {
                    val qual = shot.quality ?: 1
                    (1 - qual).toFloat() 
                }

                val centerX = ((xNorm + 1f) / 2f * (gridSize - 1))
                val centerY = ((yNorm + 1f) / 2f * (gridSize - 1))

                // Splat density with Gaussian-like falloff (3x3 area)
                for (i in -2..2) {
                    for (j in -2..2) {
                        val gx = (centerX + i).toInt()
                        val gy = (centerY + j).toInt()
                        if (gx in 0 until gridSize && gy in 0 until gridSize) {
                            val distSq = (gx - centerX) * (gx - centerX) + (gy - centerY) * (gy - centerY)
                            val weight = Math.exp(-distSq.toDouble() / 1.5).toFloat()
                            density[gx][gy] += weight
                        }
                    }
                }
            }

            val maxDensity = density.maxOfOrNull { it.maxOrNull() ?: 0.1f } ?: 1f

            // Draw smoothed heatmap
            for (i in 0 until gridSize) {
                for (j in 0 until gridSize) {
                    val d = density[i][j]
                    if (d > 0.05f) {
                        val normalizedDensity = (d / maxDensity).coerceIn(0f, 1f)
                        val color = getHeatmapColor(normalizedDensity)
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color.copy(alpha = 0.6f * normalizedDensity), Color.Transparent),
                                center = Offset(i * cellSize + cellSize / 2, j * cellSize + cellSize / 2),
                                radius = cellSize * 1.5f
                            ),
                            center = Offset(i * cellSize + cellSize / 2, j * cellSize + cellSize / 2),
                            radius = cellSize * 1.5f
                        )
                    }
                }
            }
        }
    }
}

private fun getHeatmapColor(value: Float): Color {
    return when {
        value < 0.25f -> lerpColor(Color.Blue.copy(alpha = 0.3f), Color.Cyan, value / 0.25f)
        value < 0.5f -> lerpColor(Color.Cyan, Color.Green, (value - 0.25f) / 0.25f)
        value < 0.75f -> lerpColor(Color.Green, Color.Yellow, (value - 0.5f) / 0.25f)
        else -> lerpColor(Color.Yellow, Color.Red, (value - 0.75f) / 0.25f)
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
