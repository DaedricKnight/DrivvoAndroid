package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.Volume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuelEconomyTest {

    @Test
    fun `partial refuelings add their volume to the segment closed by the next full tank`() {
        val segment = fuelSegments(
            listOf(
                refueling(id = 1, odometer = 10_000, milli = 40_000),
                refueling(id = 2, odometer = 10_300, milli = 15_000, full = false),
                refueling(id = 3, odometer = 10_600, milli = 30_000),
            ),
        ).single()

        assertEquals(600L, segment.distance)
        assertEquals(Volume(45_000), segment.volume)
        assertEquals(7.5, segment.volumePer100, 1e-9)
    }

    @Test
    fun `consumption is shown on the full tank that opens the segment and on partials inside it`() {
        val consumption = consumptionByRefuelingId(
            fuelSegments(
                listOf(
                    refueling(id = 1, odometer = 10_000, milli = 40_000),
                    refueling(id = 2, odometer = 10_300, milli = 15_000, full = false),
                    refueling(id = 3, odometer = 10_600, milli = 30_000),
                    refueling(id = 4, odometer = 11_000, milli = 32_000),
                ),
            ),
        )

        assertEquals(7.5, consumption.getValue(1), 1e-9)
        assertEquals(7.5, consumption.getValue(2), 1e-9)
        assertEquals(8.0, consumption.getValue(3), 1e-9)
        // Отрезок последнего бака ещё не закрыт следующей заправкой.
        assertNull(consumption[4])
    }

    @Test
    fun `missed previous refueling breaks the segment it would close`() {
        val segments = fuelSegments(
            listOf(
                refueling(id = 1, odometer = 10_000, milli = 40_000),
                refueling(id = 2, odometer = 11_000, milli = 50_000, missedPrevious = true),
                refueling(id = 3, odometer = 11_500, milli = 35_000),
            ),
        )

        assertEquals(listOf(2L), segments.map { it.start.id })
        assertEquals(7.0, segments.single().volumePer100, 1e-9)
    }

    @Test
    fun `partial refuelings before the first full tank are ignored`() {
        val segment = fuelSegments(
            listOf(
                refueling(id = 1, odometer = 9_800, milli = 10_000, full = false),
                refueling(id = 2, odometer = 10_000, milli = 40_000),
                refueling(id = 3, odometer = 10_500, milli = 30_000),
            ),
        ).single()

        assertEquals(6.0, segment.volumePer100, 1e-9)
    }

    @Test
    fun `refuelings are ordered by odometer, not by the order they were entered`() {
        val segment = fuelSegments(
            listOf(
                refueling(id = 3, odometer = 10_600, milli = 30_000),
                refueling(id = 1, odometer = 10_000, milli = 40_000),
            ),
        ).single()

        assertEquals(5.0, segment.volumePer100, 1e-9)
    }

    @Test
    fun `average weighs segments by distance`() {
        val economy = fuelEconomy(
            fuelSegments(
                listOf(
                    refueling(id = 1, odometer = 10_000, milli = 40_000),
                    refueling(id = 2, odometer = 10_100, milli = 10_000),
                    refueling(id = 3, odometer = 11_000, milli = 45_000),
                ),
            ),
        )!!

        // 55 литров на 1000 км, хотя среднее двух отрезков — 7.5.
        assertEquals(5.5, economy.average, 1e-9)
        assertEquals(5.0, economy.best, 1e-9)
        assertEquals(10.0, economy.worst, 1e-9)
    }

    @Test
    fun `one full tank gives no economy yet`() {
        assertNull(fuelEconomy(fuelSegments(listOf(refueling(id = 1, odometer = 10_000, milli = 40_000)))))
    }
}
