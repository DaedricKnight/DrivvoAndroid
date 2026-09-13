package com.artemkhateev.carlog.ui.format

import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.UnitPrice
import com.artemkhateev.carlog.data.model.Volume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputFiltersTest {

    @Test
    fun `decimal input keeps one separator and limited decimals`() {
        assertEquals("12.345", sanitizeDecimalInput("12,3456", maxDecimals = 3))
        assertEquals("0.5", sanitizeDecimalInput(".5", maxDecimals = 2))
        assertEquals("12.3", sanitizeDecimalInput("1a2b.3c", maxDecimals = 2))
        assertEquals("1.23", sanitizeDecimalInput("1.2.3", maxDecimals = 2))
        assertEquals("123", sanitizeDecimalInput("12.3", maxDecimals = 0))
    }

    @Test
    fun `whole input keeps digits only`() {
        assertEquals("12500", sanitizeWholeInput("12 500 km"))
    }

    @Test
    fun parsing() {
        assertEquals(Money(4_550), parseMoney("45.5"))
        assertEquals(Money(4_550), parseMoney("45,50"))
        assertNull(parseMoney(""))
        assertNull(parseMoney("4.555"))
        assertEquals(Volume(38_215), parseVolume("38.215"))
        assertEquals(UnitPrice(1_799), parseUnitPrice("1,799"))
        assertEquals(12_500L, parseWhole("12500"))
        assertNull(parseWhole("12.5"))
    }

    @Test
    fun `values go back into fields without trailing zeros`() {
        assertEquals("45.5", Money(4_550).toInputText())
        assertEquals("1200", Money(120_000).toInputText())
        assertEquals("9", Volume(9_000).toInputText())
        assertEquals("1.799", UnitPrice(1_799).toInputText())
        assertEquals("0", Money.ZERO.toInputText())
    }
}
