package com.artemkhateev.carlog.ui.format

import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.UnitPrice
import com.artemkhateev.carlog.data.model.Volume
import com.artemkhateev.carlog.data.settings.ConsumptionFormat
import com.artemkhateev.carlog.data.settings.DistanceUnit
import com.artemkhateev.carlog.data.settings.VolumeUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency
import java.util.Locale

class FormatsTest {

    private fun formats(
        distance: DistanceUnit = DistanceUnit.Kilometer,
        volume: VolumeUnit = VolumeUnit.Liter,
        consumption: ConsumptionFormat = ConsumptionFormat.VolumePer100,
    ) = Formats(Currency.getInstance("EUR"), distance, volume, consumption, Locale.US)

    @Test
    fun money() {
        assertEquals("€1,234.56", formats().money(Money(123_456)))
        assertEquals("-€50.00", formats().money(Money(-5_000)))
        assertEquals("€7.49", formats().money(7.4871))
    }

    @Test
    fun `unit price keeps up to three decimals`() {
        assertEquals("€1.799", formats().unitPrice(UnitPrice(1_799)))
        assertEquals("€2.50", formats().unitPrice(UnitPrice(2_500)))
    }

    @Test
    fun `volume drops trailing zeros`() {
        assertEquals("38.215 L", formats().volume(Volume(38_215)))
        assertEquals("12.5 L", formats().volume(Volume(12_500)))
        assertEquals("9 L", formats().volume(Volume(9_000)))
    }

    @Test
    fun `consumption follows the chosen format`() {
        assertEquals("6.25 L/100km", formats().consumption(6.25))
        assertEquals("16 km/L", formats(consumption = ConsumptionFormat.DistancePerVolume).consumption(6.25))
        assertEquals(
            "mpg",
            formats(DistanceUnit.Mile, VolumeUnit.UsGallon, ConsumptionFormat.DistancePerVolume).consumptionLabel,
        )
        assertEquals("gal/100mi", formats(DistanceUnit.Mile, VolumeUnit.UsGallon).consumptionLabel)
    }

    @Test
    fun `distance and odometer have no thousands separators`() {
        assertEquals("123456 km", formats().distance(123_456L))
        assertEquals("433 km", formats().distance(1_300.0 / 3))
    }

    @Test
    fun dates() {
        assertEquals("MARCH 2025", formats().monthTitle(YearMonth.of(2025, 3)))
        assertEquals("1 Mar 2025", formats().fullDate(LocalDate.of(2025, 3, 1)))
        assertEquals("05 Mar", formats().dayMonth(LocalDate.of(2025, 3, 5)))
        assertEquals("2025-03-05", formats().date(LocalDate.of(2025, 3, 5)))
    }
}
