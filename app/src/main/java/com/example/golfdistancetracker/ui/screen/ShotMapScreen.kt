package com.example.golfdistancetracker.ui.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.golfdistancetracker.data.entity.Shot
import com.example.golfdistancetracker.ui.viewmodel.HistorySession
import com.example.golfdistancetracker.ui.viewmodel.StatsViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotMapScreen(
    session: HistorySession,
    viewModel: StatsViewModel,
    onBack: () -> Unit
) {
    val shots by viewModel.getShotsForSession(session).collectAsState(initial = emptyList())
    val cameraPositionState = rememberCameraPositionState()
    
    val blueMarker = remember { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE) }
    val whiteMarker = remember { createCircleBitmap(Color.White, Color.Black) }

    // Auto-zoom to fit all markers
    LaunchedEffect(shots) {
        if (shots.isNotEmpty()) {
            val builder = LatLngBounds.Builder()
            var hasPoints = false
            shots.forEach { shot ->
                if (shot.startLatitude != null && shot.startLongitude != null) {
                    builder.include(LatLng(shot.startLatitude, shot.startLongitude))
                    hasPoints = true
                }
                if (shot.endLatitude != null && shot.endLongitude != null) {
                    builder.include(LatLng(shot.endLatitude, shot.endLongitude))
                    hasPoints = true
                }
            }
            if (hasPoints) {
                val bounds = builder.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.HYBRID),
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            shots.forEach { shot ->
                val start = if (shot.startLatitude != null && shot.startLongitude != null) {
                    LatLng(shot.startLatitude, shot.startLongitude)
                } else null
                
                val end = if (shot.endLatitude != null && shot.endLongitude != null) {
                    LatLng(shot.endLatitude, shot.endLongitude)
                } else null

                if (start != null && end != null) {
                    Polyline(
                        points = listOf(start, end),
                        color = Color.Yellow,
                        width = 5f,
                        geodesic = true
                    )
                }

                if (start != null) {
                    Marker(
                        state = rememberMarkerState(position = start),
                        title = "Start",
                        icon = blueMarker
                    )
                }

                if (end != null) {
                    Marker(
                        state = rememberMarkerState(position = end),
                        title = "End",
                        icon = whiteMarker,
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
                    )
                }
                
                // Target location calculation (Optional)
                if (start != null && shot.intendedHeading != null && shot.distance != null) {
                    val target = calculateTarget(start, shot.intendedHeading, shot.distance)
                    Marker(
                        state = rememberMarkerState(position = target),
                        title = "Target",
                        snippet = "Intended Heading: ${shot.intendedHeading}°",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }
        }
    }
}

private fun createCircleBitmap(fillColor: Color, strokeColor: Color): BitmapDescriptor {
    val size = 30
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
    }

    // Draw Fill
    paint.color = fillColor.toArgb()
    paint.style = Paint.Style.FILL
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

    // Draw Stroke
    paint.color = strokeColor.toArgb()
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Calculates a target coordinate given start, heading (degrees), and distance (meters).
 */
private fun calculateTarget(start: LatLng, heading: Float, distance: Double): LatLng {
    val r = 6371000.0 // Earth radius in meters
    val lat1 = Math.toRadians(start.latitude)
    val lon1 = Math.toRadians(start.longitude)
    val brng = Math.toRadians(heading.toDouble())
    val dR = distance / r

    val lat2 = Math.asin(Math.sin(lat1) * Math.cos(dR) + Math.cos(lat1) * Math.sin(dR) * Math.cos(brng))
    val lon2 = lon1 + Math.atan2(
        Math.sin(brng) * Math.sin(dR) * Math.cos(lat1),
        Math.cos(dR) - Math.sin(lat1) * Math.sin(lat2)
    )

    return LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2))
}
