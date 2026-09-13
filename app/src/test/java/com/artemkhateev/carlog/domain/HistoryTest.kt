package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.Volume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class HistoryTest {

    private val entries = listOf(
        refueling(id = 1, odometer = 9_000, milli = 40_000, dateTime = at(2, 10)),
        refueling(id = 2, odometer = 9_600, milli = 36_000, dateTime = at(2, 25)),
        // Тот же id, что у заправки: таблицы разные, и расход сервису достаться не должен.
        service(id = 2, odometer = 9_700, dateTime = at(3, 3), 5L to 5_000),
        refueling(id = 3, odometer = 10_100, milli = 25_000, dateTime = at(3, 15)),
    )

    private val rows = buildHistory(entries, tankCapacity = Volume(50_000))

    @Test
    fun `months go from newest to oldest with their entries under them`() {
        val shape = rows.map { row ->
            when (row) {
                is HistoryRow.Month -> "month ${row.month}"
                is HistoryRow.Item -> "${row.entry.type} ${row.entry.id}"
                is HistoryRow.Start -> "start ${row.date}"
            }
        }

        assertEquals(
            listOf(
                "month 2025-03",
                "Refueling 3",
                "Service 2",
                "month 2025-02",
                "Refueling 2",
                "Refueling 1",
                "start 2025-02-10",
            ),
            shape,
        )
    }

    @Test
    fun `month summary sums costs, distance from the previous month and closed segments`() {
        val march = rows.filterIsInstance<HistoryRow.Month>().first { it.month == YearMonth.of(2025, 3) }

        assertEquals(Money(25_000 * 1_500 / 10_000 + 5_000), march.cost)
        assertEquals(500L, march.distance)
        assertEquals(5.0, march.averageConsumption!!, 1e-9)
        assertEquals(1_000.0, march.tankRange!!, 1e-9)
    }

    @Test
    fun `consumption belongs to refuelings only`() {
        val items = rows.filterIsInstance<HistoryRow.Item>().associateBy { it.entry.type to it.entry.id }

        assertEquals(6.0, items.getValue(com.artemkhateev.carlog.data.model.EntryType.Refueling to 1L).consumption!!, 1e-9)
        assertEquals(5.0, items.getValue(com.artemkhateev.carlog.data.model.EntryType.Refueling to 2L).consumption!!, 1e-9)
        assertNull(items.getValue(com.artemkhateev.carlog.data.model.EntryType.Refueling to 3L).consumption)
        assertNull(items.getValue(com.artemkhateev.carlog.data.model.EntryType.Service to 2L).consumption)
    }

    @Test
    fun `empty history has no start`() {
        assertEquals(emptyList<HistoryRow>(), buildHistory(emptyList(), tankCapacity = null))
        assertEquals(LocalDate.of(2025, 2, 10), (rows.last() as HistoryRow.Start).date)
    }
}
