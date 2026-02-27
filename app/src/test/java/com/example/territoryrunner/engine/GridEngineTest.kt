package com.example.territoryrunner.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM Unit Tests for the geospatial mathematics engine.
 * No Android framework dependencies allowed.
 */
class GridEngineTest {

    // Helper coordinate close to equator (~111km per latitude degree)
    private val baseLat = 0.0
    private val baseLng = 0.0

    // ~30 meters in degrees at the equator
    private val stepDegree = GridEngine.DEFAULT_GRID_SIZE_METERS * 0.000009

    // --- 1. Grid ID Consistency ---

    @Test
    fun getGridId_sameCoordinates_sameId() {
        val id1 = GridEngine.getGridId(baseLat, baseLng)
        val id2 = GridEngine.getGridId(baseLat, baseLng)
        assertEquals("Exact same coordinates must yield identical grid IDs", id1, id2)
    }

    @Test
    fun getGridId_slightMovementUnder30m_sameId() {
        val idStart = GridEngine.getGridId(baseLat, baseLng)
        
        // Move ~15 meters (half a step) positive
        val idMoved = GridEngine.getGridId(baseLat + (stepDegree / 2), baseLng + (stepDegree / 2))
        
        assertEquals("Movement under the 30m threshold should stay within the same grid cell", idStart, idMoved)
    }

    @Test
    fun getGridId_movementBeyond30m_differentId() {
        val idStart = GridEngine.getGridId(baseLat, baseLng)
        
        // Move ~45 meters (1.5 steps) positive
        val idMoved = GridEngine.getGridId(baseLat + (stepDegree * 1.5), baseLng)
        
        assertFalse("Movement exceeding the 30m grid size must enter a new grid cell", idStart == idMoved)
    }

    // --- 2. Haversine distance validation & 3. shouldCapture logic ---
    // Note: GridEngine.shouldCapture encompasses the Haversine distance calculation

    @Test
    fun shouldCapture_movementUnderMinDistance_returnsFalse() {
        // Move slightly (~5 meters)
        val tinyMovementLat = baseLat + (stepDegree / 6)
        
        // We set a 10m minimum threshold
        val capture = GridEngine.shouldCapture(
            lastLat = baseLat, lastLng = baseLng,
            newLat = tinyMovementLat, newLng = baseLng,
            minDistanceMeters = 10.0
        )
        assertFalse("Movement of ~5 meters should NOT trigger capture when threshold is 10m", capture)
    }

    @Test
    fun shouldCapture_movementAboveMinDistance_returnsTrue() {
        // Move deliberately (~20 meters)
        val largerMovementLat = baseLat + (stepDegree * 0.66)

        // Threshold 10m
        val capture = GridEngine.shouldCapture(
            lastLat = baseLat, lastLng = baseLng,
            newLat = largerMovementLat, newLng = baseLng,
            minDistanceMeters = 10.0
        )
        assertTrue("Movement > 10m should trigger a capture event to prevent GPS sitting jitter", capture)
    }

    @Test
    fun shouldCapture_exactlyAtThreshold_returnsTrue() {
        // We test the edge case. If exactly equal, the logic is distance >= minDistanceMeters
        // Haversine float precision makes "exact" tricky, but we can mock a known exact jump
        
        // A jump of exactly 0 degrees lat/lng is 0 meters. 
        val noMove = GridEngine.shouldCapture(baseLat, baseLng, baseLat, baseLng, 0.0)
        assertTrue("If distance perfectly equals threshold (0m), it should capture (>= logic)", noMove)
    }

    // --- 4. Speed Filter ---

    @Test
    fun isValidSpeed_walkingSpeed_returnsTrue() {
        // 2.0 m/s is roughly a brisk walk
        val isValid = GridEngine.isValidSpeed(2.0f, maxSpeed = 7f)
        assertTrue("2.0 m/s is a valid human walking/running speed", isValid)
    }

    @Test
    fun isValidSpeed_drivingSpeed_returnsFalse() {
        // 15.0 m/s is roughly 54 km/h (driving)
        val isValid = GridEngine.isValidSpeed(15.0f, maxSpeed = 7f)
        assertFalse("15.0 m/s vehicular speeds must be rejected", isValid)
    }

    @Test
    fun isValidSpeed_exactMaxSpeed_returnsTrue() {
        // Edge case: hitting exactly 7 m/s (approx 25 km/h, elite sprinter territory)
        val isValid = GridEngine.isValidSpeed(7.0f, maxSpeed = 7f)
        assertTrue("Exactly achieving max speed is inclusive (<=) and should be valid", isValid)
    }
    
    // --- 5. Bounding Box Correctness ---

    @Test
    fun getGridBoundingBox_validGridId_returnsFourCoordinates() {
        val gridId = "100-200"
        val box = GridEngine.getGridBoundingBox(gridId)
        
        assertEquals("Bounding box must contain exactly 4 coordinate pairs (corners)", 4, box.size)
    }

    @Test
    fun getGridBoundingBox_formsSquareRegion() {
        val gridId = "100-200"
        val box = GridEngine.getGridBoundingBox(gridId)
        
        val p1 = box[0] // BottomLeft
        val p2 = box[1] // BottomRight
        val p3 = box[2] // TopRight
        val p4 = box[3] // TopLeft

        // Verify Latitudes
        assertEquals("P1 and P2 should share base latitude (bottom edge)", p1.first, p2.first, 0.0000001)
        assertEquals("P3 and P4 should share upper latitude (top edge)", p3.first, p4.first, 0.0000001)
        assertTrue("Top edge latitude must be strictly greater than bottom edge", p3.first > p1.first)

        // Verify Longitudes
        assertEquals("P1 and P4 should share base longitude (left edge)", p1.second, p4.second, 0.0000001)
        assertEquals("P2 and P3 should share right longitude (right edge)", p2.second, p3.second, 0.0000001)
        assertTrue("Right edge longitude must be strictly greater than left edge", p2.second > p1.second)
    }

    @Test
    fun getGridBoundingBox_invalidGridId_returnsEmptyList() {
        val noHyphen = GridEngine.getGridBoundingBox("100200")
        val letters = GridEngine.getGridBoundingBox("A-B")
        val tooMany = GridEngine.getGridBoundingBox("100-200-300")

        assertTrue("Malformed string without hyphen returns empty", noHyphen.isEmpty())
        assertTrue("Malformed string with characters instead of Ints returns empty", letters.isEmpty())
        assertTrue("Malformed string with too many hyphens returns empty", tooMany.isEmpty())
    }
}
