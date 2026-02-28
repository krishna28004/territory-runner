package com.example.territoryrunner.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.territoryrunner.viewmodel.RunViewModel
import com.example.territoryrunner.viewmodel.RunViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapScreen(
    viewModel: RunViewModel = viewModel(
        factory = RunViewModelFactory(LocalContext.current)
    )
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val uiState by viewModel.uiState.collectAsState()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    LaunchedEffect(hasLocationPermission) {
        // Kick off viewmodel background tracking once permission is granted
        if (hasLocationPermission) {
            viewModel.startLocationUpdates(context)
        }
    }

    var hasAutoCentered by remember { mutableStateOf(false) }
    val territoryPolygons = remember { mutableMapOf<String, Polygon>() }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // Default center coordinate to avoid starting at (0,0)
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(39.8283, -98.5795))
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mapView.onResume()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                mapView.onPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
            locationOverlay.enableMyLocation()
            if (mapView.overlays.none { it is MyLocationNewOverlay }) {
                mapView.overlays.add(locationOverlay)
            }
        }
    }

    LaunchedEffect(uiState.currentLocation) {
        val currentGeoPoint = uiState.currentLocation
        
        // 1-time auto center logic!
        if (currentGeoPoint != null && !hasAutoCentered) {
            mapView.controller.apply {
                setZoom(18.0)
                setCenter(currentGeoPoint)
            }
            hasAutoCentered = true 
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                // ON CREATE: If the ViewModel already has captured cells (e.g. from surviving a rotation),
                // we must immediately draw them onto this brand new MapView instance.
                uiState.capturedCells.forEach { gridId ->
                    if (!territoryPolygons.containsKey(gridId)) {
                        val boundingBox = com.example.territoryrunner.engine.GridEngine.getGridBoundingBox(gridId)
                        if (boundingBox.isNotEmpty()) {
                            val gridPoints = boundingBox.map { GeoPoint(it.first, it.second) }
                            val polygon = createTerritoryPolygon(gridPoints)
                            
                            // Add to both our Compose tracking state AND the physical map view
                            territoryPolygons[gridId] = polygon
                            mapView.overlays.add(polygon)
                        }
                    }
                }
                mapView
            },
            update = { mv ->
                 var newOverlaysAdded = false

                 // ON UPDATE: Check for any cells that the ViewModel knows about,
                 // but our MapScreen dictionary (territoryPolygons) hasn't drawn yet.
                 uiState.capturedCells.forEach { gridId ->
                     if (!territoryPolygons.containsKey(gridId)) {
                         val boundingBox = com.example.territoryrunner.engine.GridEngine.getGridBoundingBox(gridId)
                         if (boundingBox.isNotEmpty()) {
                             val gridPoints = boundingBox.map { GeoPoint(it.first, it.second) }
                             val polygon = createTerritoryPolygon(gridPoints)
                             
                             territoryPolygons[gridId] = polygon
                             mv.overlays.add(polygon)
                             newOverlaysAdded = true
                         }
                     }
                 }
                 
                 // Only issue an invalidate cycle if we actually mutated the MapView. 
                 // This prevents massive FPS drop during normal recompositions.
                 if (newOverlaysAdded) {
                    mv.invalidate() 
                 }
            }
        )

        RunUIScreen(
            territoryCount = uiState.territoryCount,
            isRunning = uiState.isRunning,
            captureEvent = uiState.eventCapture, // Pulling from VM now
            onStartClick = { viewModel.toggleRun() },
            onStopClick = { viewModel.toggleRun() }
        )

        // Focus My Location Button
        FloatingActionButton(
            onClick = {
                uiState.currentLocation?.let { currentGeoPoint ->
                    mapView.controller.animateTo(currentGeoPoint, 18.0, 1000L)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 120.dp), // Adjust padding to avoid start/stop button 
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Focus My Location"
            )
        }
    }
}

@Composable
fun RunUIScreen(
    territoryCount: Int,
    isRunning: Boolean,
    captureEvent: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        
        // Animated +1 Popup
        AnimatedVisibility(
            visible = captureEvent,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Text(
                text = "+1",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFFFF9800), // Orange tint for game popup
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Top semi-transparent territory counter card
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp), // Pushed down slightly to give room for popup
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TERRITORY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "$territoryCount",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (isRunning) "Status: Running" else "Status: Paused",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRunning) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Bottom circular Start/Stop button
        Button(
            onClick = if (isRunning) onStopClick else onStartClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Filled.Close else Icons.Filled.PlayArrow,
                contentDescription = if (isRunning) "Stop Run" else "Start Run",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/* ---------------- GRID LOGIC ---------------- */

fun createTerritoryPolygon(points: List<GeoPoint>): Polygon {
    return Polygon().apply {
        this.points = points
        fillPaint.color = AndroidColor.argb(80, 0, 150, 255)
        strokeColor = AndroidColor.rgb(0, 100, 200)
        strokeWidth = 2f
    }
}