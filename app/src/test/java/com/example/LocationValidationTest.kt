package com.example

import com.example.model.LocationEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationValidationTest {

    @Test
    fun validCoordinates_returnTrue() {
        assertTrue(LocationEvent.isValidCoordinate(0.0, 0.0))
        assertTrue(LocationEvent.isValidCoordinate(37.7749, -122.4194))
        assertTrue(LocationEvent.isValidCoordinate(90.0, 180.0))
        assertTrue(LocationEvent.isValidCoordinate(-90.0, -180.0))
    }

    @Test
    fun invalidCoordinates_returnFalse() {
        assertFalse(LocationEvent.isValidCoordinate(90.1, 0.0))
        assertFalse(LocationEvent.isValidCoordinate(-90.1, 0.0))
        assertFalse(LocationEvent.isValidCoordinate(0.0, 180.1))
        assertFalse(LocationEvent.isValidCoordinate(0.0, -180.1))
        assertFalse(LocationEvent.isValidCoordinate(Double.NaN, 0.0))
        assertFalse(LocationEvent.isValidCoordinate(0.0, Double.NaN))
    }

    @Test
    fun validAccuracy_returnTrue() {
        assertTrue(LocationEvent.isValidAccuracy(5.0f))
        assertTrue(LocationEvent.isValidAccuracy(0.0f))
        assertTrue(LocationEvent.isValidAccuracy(150.0f))
    }

    @Test
    fun invalidAccuracy_returnFalse() {
        assertFalse(LocationEvent.isValidAccuracy(-1.0f))
        assertFalse(LocationEvent.isValidAccuracy(5001.0f))
        assertFalse(LocationEvent.isValidAccuracy(Float.NaN))
    }
}
