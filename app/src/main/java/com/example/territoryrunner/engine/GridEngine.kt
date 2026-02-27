package com.example.territoryrunner.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Handles all mathematical and spatial calculations for the Territory grid system.
 * Free of Android UI dependencies, pure business logic.
 */
object GridEngine {

    const val DEFAULT_GRID_SIZE_METERS = 30.0
    // Roughly 0.00027 degrees is ~30 meters at the equator.
    private const val DEGREES_PER_METER = 0.000009
    private const val DEFAULT_GRID_SIZE_DEGREES = DEFAULT_GRID_SIZE_METERS * DEGREES_PER_METER

    /**
     * Converts a raw GPS coordinate into a discrete grid string hash "latIndex-lngIndex"
     */
    fun getGridId(lat: Double, lng: Double): String {
        val latIndex = (lat / DEFAULT_GRID_SIZE_DEGREES).toInt()
        val lngIndex = (lng / DEFAULT_GRID_SIZE_DEGREES).toInt()
        return "$latIndex-$lngIndex"
    }

    /**
     * Returns the 4 corner coordinates (lat, lng) of a given grid ID.
     */
    fun getGridBoundingBox(gridId: String): List<Pair<Double, Double>> {
        val parts = gridId.split("-")
        if (parts.size != 2) return emptyList()
        
        val latIndex = parts[0].toIntOrNull() ?: return emptyList()
        val lngIndex = parts[1].toIntOrNull() ?: return emptyList()

        val baseLat = latIndex * DEFAULT_GRID_SIZE_DEGREES
        val baseLng = lngIndex * DEFAULT_GRID_SIZE_DEGREES

        return listOf(
            Pair(baseLat, baseLng),
            Pair(baseLat + DEFAULT_GRID_SIZE_DEGREES, baseLng),
            Pair(baseLat + DEFAULT_GRID_SIZE_DEGREES, baseLng + DEFAULT_GRID_SIZE_DEGREES),
            Pair(baseLat, baseLng + DEFAULT_GRID_SIZE_DEGREES)
        )
    }

    /**
     * Calculates the distance between two points using the Haversine formula and returns
     * true if the user has moved beyond the minimum threshold (reduces GPS jitter).
     */
    fun shouldCapture(
        lastLat: Double,
        lastLng: Double,
        newLat: Double,
        newLng: Double,
        minDistanceMeters: Double = 10.0
    ): Boolean {
        val distance = calculateHaversineDistance(lastLat, lastLng, newLat, newLng)
        return distance >= minDistanceMeters
    }

    /**
     * Filters out vehicular speeds. 
     * Returns true if the user is running/walking (<= threshold).
     */
    fun isValidSpeed(speedMetersPerSecond: Float, maxSpeed: Float = 7f): Boolean {
        return speedMetersPerSecond <= maxSpeed
    }

    /**
     * Filters out poor GPS accuracy signals.
     */
    fun isValidAccuracy(accuracy: Float, maxAccuracy: Float = 20f): Boolean {
        return accuracy <= maxAccuracy
    }

    /**
     * Pure Haversine formula to compute distance between two lat/lng coordinates in meters.
     */
    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
