package com.artemkhateev.carlog.feature.entry

import com.artemkhateev.carlog.data.model.CostItem
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.data.model.Expense
import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.RouteKind
import com.artemkhateev.carlog.data.model.Service
import com.artemkhateev.carlog.data.model.UnitPrice
import com.artemkhateev.carlog.data.model.Vehicle
import com.artemkhateev.carlog.data.model.Volume
import com.artemkhateev.carlog.domain.OdometerBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class EntryDraftsTest {

    private val day = LocalDate.of(2025, 3, 1)
    private val noBounds = BoundsAt { OdometerBounds(null, null) }

    @Test
    fun `choosing types keeps typed amounts`() {
        val draft = ItemizedDraft(type = EntryType.Service, vehicleId = 1, date = day, time = LocalTime.NOON)
            .withTypes(setOf(1, 2))
            .withItemAmount(1, "30")
            .withTypes(setOf(1, 3))

        assertEquals(listOf(ItemDraft(1, "30"), ItemDraft(3, "")), draft.items)
    }

    @Test
    fun `itemized total subtracts the discount`() {
        val draft = ItemizedDraft(
            type = EntryType.Expense,
            vehicleId = 1,
            date = day,
            time = LocalTime.NOON,
            items = listOf(ItemDraft(1, "30"), ItemDraft(2, "20.5")),
            discountText = "5",
        )

        assertEquals(Money(4_550), draft.total)
        val expense = draft.toEntry(noBounds) as Expense
        assertEquals(listOf(CostItem(1, Money(3_000)), CostItem(2, Money(2_050))), expense.items)
        assertEquals(Money(4_550), expense.total)
    }

    @Test
    fun `service needs an odometer, types and amounts`() {
        val errors = ItemizedDraft(type = EntryType.Service, vehicleId = 1, date = day, time = LocalTime.NOON).errors(noBounds)

        assertEquals(FieldError.Required, errors[DraftField.Odometer])
        assertEquals(FieldError.NoTypes, errors[DraftField.Items])
        val withEmptyAmount = ItemizedDraft(type = EntryType.Service, vehicleId = 1, date = day, time = LocalTime.NOON, odometerText = "100")
            .withTypes(setOf(4))
        assertEquals(mapOf(DraftField.ItemAmount to FieldError.Required), withEmptyAmount.errors(noBounds))
    }

    @Test
    fun `expense odometer is optional and discount cannot exceed the items`() {
        val draft = ItemizedDraft(
            type = EntryType.Expense,
            vehicleId = 1,
            date = day,
            time = LocalTime.NOON,
            items = listOf(ItemDraft(1, "10")),
            discountText = "12",
        )

        assertEquals(mapOf(DraftField.Discount to FieldError.DiscountTooBig), draft.errors(noBounds))
    }

    @Test
    fun `route end cannot be before its start`() {
        val draft = RouteDraft(
            vehicleId = 1,
            date = day,
            time = LocalTime.of(10, 0),
            startOdometerText = "1000",
            endDate = day,
            endTime = LocalTime.of(9, 0),
            endOdometerText = "900",
        )
        val errors = draft.errors(noBounds)

        assertEquals(FieldError.EndBeforeStart, errors[DraftField.End])
        assertEquals(FieldError.EndBeforeStart, errors[DraftField.EndOdometer])
    }

    @Test
    fun `moving the route start keeps its duration`() {
        val draft = RouteDraft(vehicleId = 1, date = day, time = LocalTime.of(10, 0), endDate = day, endTime = LocalTime.of(11, 30))
            .withDate(day.minusDays(1))
            .withTime(LocalTime.of(23, 0))

        assertEquals(LocalDateTime.of(2025, 3, 1, 0, 30), draft.end)
    }

    @Test
    fun `trip value is distance times rate, freight needs a value`() {
        val trip = RouteDraft(
            vehicleId = 1,
            date = day,
            time = LocalTime.NOON,
            startOdometerText = "1000",
            endDate = day,
            endTime = LocalTime.of(13, 0),
            endOdometerText = "1120",
            rateText = "0.25",
        )

        assertEquals(Money(3_000), trip.value)
        assertEquals(FieldError.Required, trip.copy(kind = RouteKind.Freight).errors(noBounds)[DraftField.FreightValue])
    }

    @Test
    fun `new refueling copies fuel, price and station from the last one`() {
        val last = Refueling(
            id = 7,
            vehicleId = 1,
            dateTime = LocalDateTime.of(2025, 2, 20, 9, 0),
            odometer = 9_000,
            fuelId = 3,
            unitPrice = UnitPrice(1_650),
            totalCost = Money(6_600),
            volume = Volume(40_000),
            placeId = 5,
            driverId = 8,
            paymentMethodId = 9,
        )
        val draft = newDraft(EntryType.Refueling, Vehicle(id = 1, name = "Car", fuelId = 1), listOf(last), LocalDateTime.of(2025, 3, 1, 18, 45, 31))
            as RefuelingDraft

        assertEquals(3L, draft.fuelId)
        assertEquals("1.65", draft.priceText)
        assertEquals(listOf(PriceField.UnitPrice), draft.typedPriceFields)
        assertEquals(5L, draft.placeId)
        assertEquals(8L, draft.driverId)
        assertEquals(LocalTime.of(18, 45), draft.time)
        assertTrue(draft.fullTank)
    }

    @Test
    fun `editing keeps every field of a service`() {
        val service = Service(
            id = 4,
            vehicleId = 1,
            dateTime = LocalDateTime.of(2025, 3, 2, 8, 15),
            odometer = 12_000,
            title = "Oil",
            items = listOf(CostItem(2, Money(4_500))),
            discount = Money(500),
            placeId = 3,
            notes = "note",
        )

        assertEquals(service, service.toDraft().toEntry(noBounds))
    }
}
