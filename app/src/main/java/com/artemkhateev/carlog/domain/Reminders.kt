package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.Entry
import com.artemkhateev.carlog.data.model.Expense
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Reminder
import com.artemkhateev.carlog.data.model.ReminderKind
import com.artemkhateev.carlog.data.model.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

enum class DueState { Overdue, Soon, Later }

/** Сколько осталось до срока: отрицательное — срок уже прошёл. */
data class DueStatus(val remainingDistance: Long?, val remainingDays: Long?, val state: DueState)

/** «Скоро» — за столько километров или дней до срока. */
const val SOON_DISTANCE = 500L
const val SOON_DAYS = 7L

fun dueStatus(dueOdometer: Long?, dueDate: LocalDate?, currentOdometer: Long?, today: LocalDate): DueStatus {
    val distance = if (dueOdometer != null && currentOdometer != null) dueOdometer - currentOdometer else null
    val days = dueDate?.let { ChronoUnit.DAYS.between(today, it) }
    val state = when {
        (distance != null && distance <= 0) || (days != null && days < 0) -> DueState.Overdue
        (distance != null && distance <= SOON_DISTANCE) || (days != null && days <= SOON_DAYS) -> DueState.Soon
        else -> DueState.Later
    }
    return DueStatus(distance, days, state)
}

fun Reminder.status(currentOdometer: Long?, today: LocalDate): DueStatus =
    dueStatus(dueOdometer, dueDate, currentOdometer, today)

/** Своё название, иначе — вид сервиса или расхода. */
fun Reminder.displayTitle(catalogs: Catalogs): String = title.ifBlank { catalogs.item(typeId)?.name.orEmpty() }

/** Когда ждать следующую заправку. */
data class RefuelingForecast(val odometer: Long, val date: LocalDate)

/** Сколько последних заправок берём для прогноза: темп езды со временем меняется. */
private const val FORECAST_REFUELINGS = 10

/** Прогноз по среднему пробегу и среднему числу дней между последними заправками. */
fun forecastNextRefueling(refuelings: List<Refueling>): RefuelingForecast? {
    val recent = refuelings.sortedBy { it.dateTime }.takeLast(FORECAST_REFUELINGS)
    if (recent.size < 2) return null
    val first = recent.first()
    val last = recent.last()
    val intervals = recent.size - 1
    val distanceStep = (last.odometer - first.odometer).toDouble() / intervals
    if (distanceStep <= 0) return null
    val daysStep = ChronoUnit.DAYS.between(first.dateTime.toLocalDate(), last.dateTime.toLocalDate()).toDouble() / intervals
    return RefuelingForecast(
        odometer = last.odometer + distanceStep.roundToLong(),
        date = last.dateTime.toLocalDate().plusDays(daysStep.roundToLong()),
    )
}

/** Запись старше этого числа дней внесена задним числом и напоминаний не трогает. */
const val RECENT_ENTRY_DAYS = 30L

sealed interface ReminderUpdate {
    data class Reschedule(val reminder: Reminder) : ReminderUpdate
    data class Close(val reminderId: Long) : ReminderUpdate
}

/**
 * Что сделать с напоминаниями после свежей записи сервиса или расхода того же вида:
 * повторяющееся переносится от этой записи, разовое закрывается.
 */
fun remindersAfterEntry(entry: Entry, reminders: List<Reminder>, today: LocalDate, currentOdometer: Long?): List<ReminderUpdate> {
    val (kind, typeIds) = when (entry) {
        is Service -> ReminderKind.Service to entry.items.map { it.typeId }.toSet()
        is Expense -> ReminderKind.Expense to entry.items.map { it.typeId }.toSet()
        else -> return emptyList()
    }
    val date = entry.dateTime.toLocalDate()
    if (date.isBefore(today.minusDays(RECENT_ENTRY_DAYS))) return emptyList()
    return reminders
        .filter { it.vehicleId == entry.vehicleId && it.kind == kind && it.typeId != null && it.typeId in typeIds }
        .map { reminder ->
            if (reminder.repeats) {
                ReminderUpdate.Reschedule(reminder.rescheduled(entry.odometer ?: currentOdometer, date))
            } else {
                ReminderUpdate.Close(reminder.id)
            }
        }
}

/** Следующий срок повторяющегося напоминания, отсчитанный от выполнения. */
fun Reminder.rescheduled(doneOdometer: Long?, doneDate: LocalDate): Reminder = copy(
    dueOdometer = if (repeatDistance != null && doneOdometer != null) doneOdometer + repeatDistance else dueOdometer,
    dueDate = if (repeatMonths != null) doneDate.plusMonths(repeatMonths.toLong()) else dueDate,
    notifiedKey = null,
)
