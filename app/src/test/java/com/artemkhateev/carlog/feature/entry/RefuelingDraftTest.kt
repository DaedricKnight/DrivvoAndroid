package com.artemkhateev.carlog.feature.entry

import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.UnitPrice
import com.artemkhateev.carlog.data.model.Volume
import com.artemkhateev.carlog.domain.OdometerBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RefuelingDraftTest {

    private val empty = RefuelingDraft(vehicleId = 1, date = LocalDate.of(2025, 3, 1), time = LocalTime.NOON)
    private val noBounds = BoundsAt { OdometerBounds(null, null) }

    @Test
    fun `price and total give the volume`() {
        val draft = empty
            .withPriceInput(PriceField.UnitPrice, "1.799")
            .withPriceInput(PriceField.Total, "45.5")

        assertEquals("25.292", draft.volumeText)
    }

    @Test
    fun `price and volume give the total`() {
        val draft = empty
            .withPriceInput(PriceField.UnitPrice, "1.5")
            .withPriceInput(PriceField.Volume, "38.215")

        assertEquals("57.32", draft.totalText)
    }

    @Test
    fun `total and volume give the price`() {
        val draft = empty
            .withPriceInput(PriceField.Total, "60")
            .withPriceInput(PriceField.Volume, "40")

        assertEquals("1.5", draft.priceText)
    }

    @Test
    fun `the field typed longest ago is the one recalculated`() {
        val draft = empty
            .withPriceInput(PriceField.UnitPrice, "1.5")
            .withPriceInput(PriceField.Total, "60")
            .withPriceInput(PriceField.Volume, "30")

        // Цену вводили раньше всех — теперь она считается из суммы и литров.
        assertEquals("2", draft.priceText)
        assertEquals(listOf(PriceField.Total, PriceField.Volume), draft.typedPriceFields)
    }

    @Test
    fun `clearing a typed field clears the calculated one`() {
        val draft = empty
            .withPriceInput(PriceField.UnitPrice, "1.5")
            .withPriceInput(PriceField.Total, "60")
            .withPriceInput(PriceField.Total, "")

        assertEquals("", draft.volumeText)
    }

    @Test
    fun `editing an existing refueling total recalculates its price`() {
        val existing = empty.copy(priceText = "1.5", totalText = "60", volumeText = "40", typedPriceFields = listOf(PriceField.UnitPrice, PriceField.Volume))

        assertEquals("1.6", existing.withPriceInput(PriceField.Total, "64").priceText)
    }

    @Test
    fun `odometer must fit between neighbouring records`() {
        val draft = empty.copy(odometerText = "10400", priceText = "1.5", totalText = "60", volumeText = "40")
        val bounds = BoundsAt { OdometerBounds(min = 10_500, max = 11_000) }

        assertEquals(mapOf(DraftField.Odometer to FieldError.BelowPrevious(10_500)), draft.errors(bounds))
        assertNull(draft.toEntry(bounds))
    }

    @Test
    fun `empty and zero values are errors`() {
        val errors = empty.copy(priceText = "0").errors(noBounds)

        assertEquals(FieldError.Required, errors[DraftField.Odometer])
        assertEquals(FieldError.NotPositive, errors[DraftField.UnitPrice])
        assertEquals(FieldError.Required, errors[DraftField.Total])
    }

    @Test
    fun `valid draft becomes a refueling`() {
        val refueling = empty.copy(odometerText = "10700", fuelId = 2, priceText = "1.5", totalText = "60", volumeText = "40", notes = "  ok ")
            .toEntry(noBounds)!!

        assertEquals(10_700L, refueling.odometer)
        assertEquals(UnitPrice(1_500), refueling.unitPrice)
        assertEquals(Money(6_000), refueling.totalCost)
        assertEquals(Volume(40_000), refueling.volume)
        assertEquals("ok", refueling.notes)
    }
}
