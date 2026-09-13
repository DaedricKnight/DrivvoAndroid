package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.UnitPrice
import com.artemkhateev.carlog.data.model.Volume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ReportsTest {

    private val march = DateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31))

    private val entries = listOf(
        reading(id = 1, odometer = 9_500, dateTime = at(2, 20)),
        refueling(id = 1, odometer = 10_000, milli = 40_000, dateTime = at(3, 1), priceMilli = 1_500),
        service(id = 1, odometer = 10_200, dateTime = at(3, 5), 11L to 3_000, 12L to 2_000, discountCents = 500),
        refueling(id = 2, odometer = 10_500, milli = 30_000, dateTime = at(3, 11), priceMilli = 1_600),
        expense(id = 1, odometer = 10_700, dateTime = at(3, 20), 21L to 1_250),
        refueling(id = 3, odometer = 11_000, milli = 33_000, full = false, dateTime = at(3, 21), priceMilli = 1_400),
        refueling(id = 4, odometer = 11_300, milli = 12_000, dateTime = at(3, 30), priceMilli = 1_700),
        income(id = 1, dateTime = at(3, 31), cents = 10_000, typeId = 31),
    )

    private val reports = buildReports(entries, march, tankCapacity = Volume(55_000))

    @Test
    fun `distance counts from the last reading before the period`() {
        assertEquals(1_800L, reports.general.distance)
        assertEquals(1_800.0 / 31, reports.general.dailyDistance, 1e-9)
    }

    @Test
    fun `a single reading in the period gives no distance`() {
        val february = DateRange(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28))
        assertEquals(0L, buildReports(entries, february, tankCapacity = null).general.distance)
    }

    @Test
    fun `general report sums every cost and subtracts it from income`() {
        val general = reports.general

        assertEquals(Money(23_210), general.cost.total)
        assertEquals(232.10 / 31, general.cost.perDay, 1e-9)
        assertEquals(232.10 / 1_800, general.cost.perDistance!!, 1e-9)
        assertEquals(Money(-13_210), general.balance.total)
        assertEquals(Money(17_460), general.costByType.getValue(EntryType.Refueling))
        assertEquals(Money(4_500), general.costByType.getValue(EntryType.Service))
        assertEquals(Money(1_250), general.costByType.getValue(EntryType.Expense))
        // Показание 20 февраля в период не входит.
        assertEquals(7, general.entryCount)
    }

    @Test
    fun `refueling report`() {
        val refueling = reports.refueling

        assertEquals(4, refueling.count)
        assertEquals(Money(17_460), refueling.cost.total)
        assertEquals(Volume(115_000), refueling.volume)
        assertEquals(28.75, refueling.volumePerRefueling!!, 1e-9)
        assertEquals(174.60 / 115, refueling.averagePrice!!, 1e-9)
        assertEquals(UnitPrice(1_400), refueling.lowestPrice)
        assertEquals(UnitPrice(1_700), refueling.highestPrice)
        assertEquals(3, refueling.fullTankCount)
    }

    @Test
    fun `refueling routine and projections`() {
        val refueling = reports.refueling

        // Отрезки 10 000 → 10 500 (30 л) и 10 500 → 11 300 (33 + 12 л).
        assertEquals(75.0 / 1_300 * 100, refueling.economy!!.average, 1e-9)
        assertEquals(5.625, refueling.economy.best, 1e-9)
        assertEquals(6.0, refueling.economy.worst, 1e-9)
        assertEquals(1_300.0 / 3, refueling.distanceBetween!!, 1e-9)
        assertEquals(29.0 / 3, refueling.daysBetween!!, 1e-9)
        assertEquals(55.0 / refueling.economy.average * 100, refueling.estimatedRange!!, 1e-9)
        assertEquals(174.60 / 31 * 30, refueling.monthlyProjection, 1e-9)
    }

    @Test
    fun `itemized reports split amounts by type before the discount`() {
        assertEquals(Money(4_500), reports.service.cost.total)
        assertEquals(listOf(TypeAmount(11, Money(3_000)), TypeAmount(12, Money(2_000))), reports.service.byType)
        assertEquals(Money(1_250), reports.expense.cost.total)
        assertEquals(listOf(TypeAmount(31, Money(10_000))), reports.income.byType)
    }

    @Test
    fun `monthly totals cover every month of the period`() {
        val range = DateRange(LocalDate.of(2025, 2, 15), LocalDate.of(2025, 4, 10))
        val months = buildReports(entries, range, tankCapacity = null).months

        assertEquals(listOf(YearMonth.of(2025, 2), YearMonth.of(2025, 3), YearMonth.of(2025, 4)), months.map { it.month })
        assertEquals(Money(17_460), months[1].byType.getValue(EntryType.Refueling))
        assertEquals(Money(10_000), months[1].byType.getValue(EntryType.Income))
        assertEquals(Money.ZERO, months[2].byType.getValue(EntryType.Refueling))
    }

    @Test
    fun `routes are valued by distance and rate`() {
        val routes = listOf(
            route(id = 1, from = 10_000, to = 10_120, start = at(3, 2, hour = 9), minutes = 90, rateMilli = 250),
            route(id = 2, from = 10_120, to = 10_200, start = at(3, 3, hour = 9), minutes = 30),
        )
        val report = buildReports(routes, march, tankCapacity = null).route

        assertEquals(Money(3_000), report.value.total)
        assertEquals(200L, report.distance)
        assertEquals(100.0, report.distancePerRoute!!, 1e-9)
        assertEquals(120L, report.durationMinutes)
    }

    @Test
    fun `reading report measures distance and days between readings`() {
        val readings = listOf(
            reading(id = 1, odometer = 10_050, dateTime = at(3, 2)),
            reading(id = 2, odometer = 10_550, dateTime = at(3, 12)),
        )
        val report = buildReports(readings, march, tankCapacity = null).reading

        assertEquals(500L, report.distance)
        assertEquals(10.0, report.daysBetween!!, 1e-9)
    }

    @Test
    fun `no estimated range without a tank capacity`() {
        assertNull(buildReports(entries, march, tankCapacity = null).refueling.estimatedRange)
    }
}
