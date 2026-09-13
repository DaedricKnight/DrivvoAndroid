package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.Entry
import com.artemkhateev.carlog.data.model.EntryNewestFirst
import com.artemkhateev.carlog.data.model.Money
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Volume
import com.artemkhateev.carlog.data.model.sum
import java.time.LocalDate
import java.time.YearMonth

sealed interface HistoryRow {
    /** Итог месяца над его записями. */
    data class Month(
        val month: YearMonth,
        val cost: Money,
        val distance: Long,
        val averageConsumption: Double?,
        /** Сколько проезжает полный бак при расходе этого месяца. */
        val tankRange: Double?,
    ) : HistoryRow

    /** [consumption] — только у заправок, чей отрезок уже закрыт следующим полным баком. */
    data class Item(val entry: Entry, val consumption: Double?) : HistoryRow

    /** Дата первой записи — внизу ленты. */
    data class Start(val date: LocalDate) : HistoryRow
}

/** Лента истории: месяцы от новых к старым, в каждом — итог и записи. */
fun buildHistory(entries: List<Entry>, tankCapacity: Volume?): List<HistoryRow> {
    if (entries.isEmpty()) return emptyList()
    val sorted = entries.sortedWith(EntryNewestFirst)
    val segments = fuelSegments(sorted.filterIsInstance<Refueling>())
    val consumption = consumptionByRefuelingId(segments)
    val rows = mutableListOf<HistoryRow>()
    for ((month, monthEntries) in sorted.groupBy { YearMonth.from(it.dateTime) }) {
        val range = DateRange.of(month)
        val economy = fuelEconomy(segments.filter { it.end.dateTime in range })
        rows += HistoryRow.Month(
            month = month,
            cost = monthEntries.map { it.cost }.sum(),
            distance = distanceIn(sorted, range),
            averageConsumption = economy?.average,
            tankRange = if (tankCapacity != null && economy != null && economy.average > 0) {
                tankCapacity.toDouble() / economy.average * 100
            } else {
                null
            },
        )
        // id у разных таблиц пересекаются: расход ищем только для заправок.
        monthEntries.mapTo(rows) { HistoryRow.Item(it, if (it is Refueling) consumption[it.id] else null) }
    }
    rows += HistoryRow.Start(sorted.last().dateTime.toLocalDate())
    return rows
}
