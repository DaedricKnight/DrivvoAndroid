package com.artemkhateev.carlog.feature.vehicles

import com.artemkhateev.carlog.data.model.Volume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleDraftTest {

    @Test
    fun `another make drops the model, the same make keeps it`() {
        val draft = VehicleDraft(make = "Škoda", model = "Octavia")

        assertEquals("", draft.withMake("Toyota").model)
        assertEquals("Octavia", draft.withMake("SKODA").model)
        assertEquals("SKODA", draft.withMake("SKODA").make)
    }

    @Test
    fun `name falls back to make and model`() {
        val vehicle = VehicleDraft(make = "Toyota", model = "Corolla", tankText = "50").toVehicle()!!

        assertEquals("Toyota Corolla", vehicle.name)
        assertEquals(Volume(50_000), vehicle.tankCapacity)
    }

    @Test
    fun `invalid year or tank blocks saving`() {
        assertFalse(VehicleDraft(name = "Car", yearText = "1700").isValid)
        assertNull(VehicleDraft(name = "Car", tankText = "0").toVehicle())
        assertNull(VehicleDraft().toVehicle())
    }
}
