package com.artemkhateev.carlog.feature.flex

import com.artemkhateev.carlog.data.settings.ConsumptionFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlexCalculatorTest {

    @Test
    fun `without efficiency the seventy percent rule decides`() {
        assertEquals(CheaperFuel.Second, compareFuels(2.0, 1.3, null, null, ConsumptionFormat.VolumePer100)!!.cheaper)
        assertEquals(CheaperFuel.First, compareFuels(2.0, 1.5, null, null, ConsumptionFormat.VolumePer100)!!.cheaper)
        assertEquals(CheaperFuel.Same, compareFuels(2.0, 1.4, null, null, ConsumptionFormat.VolumePer100)!!.cheaper)
        assertEquals(65.0, compareFuels(2.0, 1.3, null, null, ConsumptionFormat.VolumePer100)!!.priceRatioPercent, 1e-9)
    }

    @Test
    fun `with efficiency the cost per distance decides`() {
        // 1.60 × 7 л/100 = 0.112 за км против 0.90 × 9 л/100 = 0.081: газ дешевле, хотя его и уходит больше.
        val result = compareFuels(1.6, 0.9, 7.0, 9.0, ConsumptionFormat.VolumePer100)!!

        assertEquals(0.112, result.firstCostPerDistance!!, 1e-9)
        assertEquals(0.081, result.secondCostPerDistance!!, 1e-9)
        assertEquals(CheaperFuel.Second, result.cheaper)
    }

    @Test
    fun `distance per volume format divides the price`() {
        val result = compareFuels(1.5, 1.2, 15.0, 10.0, ConsumptionFormat.DistancePerVolume)!!

        assertEquals(0.1, result.firstCostPerDistance!!, 1e-9)
        assertEquals(0.12, result.secondCostPerDistance!!, 1e-9)
        assertEquals(CheaperFuel.First, result.cheaper)
    }

    @Test
    fun `no result without both prices`() {
        assertNull(compareFuels(0.0, 1.2, null, null, ConsumptionFormat.VolumePer100))
    }
}
