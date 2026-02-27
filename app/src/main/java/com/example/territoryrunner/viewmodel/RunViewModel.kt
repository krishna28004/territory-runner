package com.example.territoryrunner.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Correct import for the Engine logic
import com.example.territoryrunner.engine.GridEngine
// Correct explicit imports for Play Services Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint

/**
 * Single source of truth for the Active Run UI.
 */
data class RunUiState(
    val isRunning: Boolean = false,
    val territoryCount: Int = 0,
    val capturedCells: Set<String> = emptySet(),
    val eventCapture: Boolean = false,
    val currentLocation: GeoPoint? = null
)

class RunViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RunUiState())
    val uiState: StateFlow<RunUiState> = _uiState.asStateFlow()

    private var lastValidLocation: Location? = null
    
    // Hold the client instance
    private var locationClient: FusedLocationProviderClient? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                processNewLocation(location)
            }
        }
    }

    /**
     * Call this exactly once from the Composable when permissions are granted.
     * We accept 'Context' but pass 'context.applicationContext' to avoid Activity leaks!
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context) {
        if (locationClient != null) return // Already initialized

        // Safely use ApplicationContext instead of Activity Context to prevent leaks
        locationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        resumeTracking() 
    }

    @SuppressLint("MissingPermission")
    private fun resumeTracking() {
        // Modern LocationRequest.Builder API
        val activeRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 6000L)
            .setMinUpdateIntervalMillis(6000L)
            .setMinUpdateDistanceMeters(10f) 
            .build()

        locationClient?.requestLocationUpdates(
            activeRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    @SuppressLint("MissingPermission")
    private fun pauseTracking() {
        // Drop to 15s intervals to save power while paused
        val pausedRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15000L)
            .setMinUpdateDistanceMeters(20f) 
            .build()

        locationClient?.requestLocationUpdates(
            pausedRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun processNewLocation(location: Location) {
        val currentState = _uiState.value

        val newGeoPoint = GeoPoint(location.latitude, location.longitude)
        _uiState.update { it.copy(currentLocation = newGeoPoint) }

        if (!currentState.isRunning) return

        if (!GridEngine.isValidAccuracy(location.accuracy)) return
        if (!GridEngine.isValidSpeed(location.speed)) return
        
        val shouldProceed = lastValidLocation == null || GridEngine.shouldCapture(
            lastLat = lastValidLocation!!.latitude,
            lastLng = lastValidLocation!!.longitude,
            newLat = location.latitude,
            newLng = location.longitude
        )
        if (!shouldProceed) return

        lastValidLocation = location
        
        // Correctly referencing the GridEngine, not GridUtils
        val gridId = GridEngine.getGridId(location.latitude, location.longitude)
        if (!currentState.capturedCells.contains(gridId)) {
            captureNewCell(gridId)
        }
    }

    private fun captureNewCell(gridId: String) {
        _uiState.update { state ->
            state.copy(
                territoryCount = state.territoryCount + 1,
                capturedCells = state.capturedCells + gridId,
                eventCapture = true
            )
        }

        viewModelScope.launch {
            delay(1500)
            _uiState.update { it.copy(eventCapture = false) }
        }
    }

    fun toggleRun() {
        val newState = !_uiState.value.isRunning
        _uiState.update { it.copy(isRunning = newState) }
        
        if (newState) {
            resumeTracking()
        } else {
            pauseTracking()
        }
    }

    fun resetRun() {
        _uiState.value = RunUiState()
        lastValidLocation = null
    }

    override fun onCleared() {
        super.onCleared()
        // Stop tracking completely when ViewModel is destroyed
        locationClient?.removeLocationUpdates(locationCallback)
    }
}
