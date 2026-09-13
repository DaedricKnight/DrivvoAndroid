package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OdometerTest {

    private val entries = listOf(
        reading(id = 1, odometer = 10_000, dateTime = at(3, 1)),
        reading(id = 2, odometer = 10_500, dateTime = at(3, 10)),
        refueling(id = 1, odometer = 11_000, milli = 30_000, dateTime = at(3, 20)),
    )

    @Test
    fun `a record fits between the readings before and after it`() {
        val bounds = odometerBounds(entries, at(3, 15))

        assertEquals(OdometerBounds(min = 10_500, max = 11_000), bounds)
        assertEquals(OdometerViolation.BelowPrevious(10_500), bounds.violation(10_400))
        assertEquals(OdometerViolation.AboveNext(11_000), bounds.violation(11_100))
        assertNull(bounds.violation(10_700))
    }

    @Test
    fun `the record being edited does not bound itself`() {
        val bounds = odometerBounds(entries, at(3, 10), editing = EntryType.Reading to 2L)

        assertEquals(OdometerBounds(min = 10_000, max = 11_000), bounds)
    }

    @Test
    fun `ids of different types do not collide`() {
        val bounds = odometerBounds(entries, at(3, 25), editing = EntryType.Refueling to 2L)

        assertEquals(OdometerBounds(min = 11_000, max = null), bounds)
    }

    @Test
    fun `last odometer includes the end of a route`() {
        val withRoute = entries + route(id = 1, from = 11_000, to = 11_250, start = at(3, 21), minutes = 30)

        assertEquals(11_250L, lastOdometer(withRoute))
        assertNull(lastOdometer(emptyList()))
    }
}
