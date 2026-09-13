package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.Entry
import com.artemkhateev.carlog.data.model.EntryType
import java.time.LocalDateTime

/** Последнее (наибольшее) показание одометра машины. */
fun lastOdometer(entries: List<Entry>): Long? = entries.flatMap { it.odometerReadings }.maxOrNull()

/**
 * Пробег за период: от последнего показания перед периодом до последнего в периоде. Если раньше
 * показаний нет — от первого в периоде. Так пробеги соседних месяцев складываются без дыр.
 */
fun distanceIn(entries: List<Entry>, range: DateRange): Long {
    val inRange = entries.filter { it.dateTime in range }.flatMap { it.odometerReadings }
    if (inRange.isEmpty()) return 0
    val before = entries.filter { it.dateTime.toLocalDate().isBefore(range.start) }.flatMap { it.odometerReadings }.maxOrNull()
    return (inRange.max() - (before ?: inRange.min())).coerceAtLeast(0)
}

/** Допустимые показания для записи на момент: не меньше показаний до него и не больше показаний после. */
data class OdometerBounds(val min: Long?, val max: Long?) {
    fun violation(value: Long): OdometerViolation? = when {
        min != null && value < min -> OdometerViolation.BelowPrevious(min)
        max != null && value > max -> OdometerViolation.AboveNext(max)
        else -> null
    }
}

sealed interface OdometerViolation {
    data class BelowPrevious(val previous: Long) : OdometerViolation
    data class AboveNext(val next: Long) : OdometerViolation
}

/** [editing] — сама запись при правке: её прежнее показание границей не считается. */
fun odometerBounds(entries: List<Entry>, at: LocalDateTime, editing: Pair<EntryType, Long>? = null): OdometerBounds {
    val others = entries.filterNot { editing != null && it.type == editing.first && it.id == editing.second }
    return OdometerBounds(
        min = others.filter { it.dateTime <= at }.flatMap { it.odometerReadings }.maxOrNull(),
        max = others.filter { it.dateTime > at }.flatMap { it.odometerReadings }.minOrNull(),
    )
}
