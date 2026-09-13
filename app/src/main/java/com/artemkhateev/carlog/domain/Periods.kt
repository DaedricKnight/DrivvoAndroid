package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.Entry
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Период с границами включительно. */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    init {
        require(!end.isBefore(start)) { "Конец периода раньше начала: $start..$end" }
    }

    /** Сколько дней в периоде, считая оба конца: с 1 по 10 число — 10 дней. */
    val days: Long get() = ChronoUnit.DAYS.between(start, end) + 1

    val months: List<YearMonth>
        get() = generateSequence(YearMonth.from(start)) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(YearMonth.from(end)) }
            .toList()

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    operator fun contains(dateTime: LocalDateTime): Boolean = contains(dateTime.toLocalDate())

    companion object {
        fun of(month: YearMonth) = DateRange(month.atDay(1), month.atEndOfMonth())
    }
}

enum class PeriodPreset { AllTime, ThisMonth, LastMonth, Last3Months, Last6Months, ThisYear, Last12Months }

/**
 * Период пресета. «Всё время» — от первой записи до последней: так «в день» считается по дням,
 * когда машиной пользовались, а не до сегодняшнего числа. Без записей такого периода нет.
 */
fun PeriodPreset.toRange(today: LocalDate, entries: List<Entry>): DateRange? = when (this) {
    PeriodPreset.AllTime ->
        if (entries.isEmpty()) {
            null
        } else {
            DateRange(entries.minOf { it.dateTime }.toLocalDate(), entries.maxOf { it.dateTime }.toLocalDate())
        }
    PeriodPreset.ThisMonth -> DateRange(today.withDayOfMonth(1), today)
    PeriodPreset.LastMonth -> DateRange.of(YearMonth.from(today).minusMonths(1))
    PeriodPreset.Last3Months -> DateRange(today.minusMonths(3).plusDays(1), today)
    PeriodPreset.Last6Months -> DateRange(today.minusMonths(6).plusDays(1), today)
    PeriodPreset.ThisYear -> DateRange(today.withDayOfYear(1), today)
    PeriodPreset.Last12Months -> DateRange(today.minusMonths(12).plusDays(1), today)
}
