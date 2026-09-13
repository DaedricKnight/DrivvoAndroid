package com.artemkhateev.carlog.feature.reminders

import com.artemkhateev.carlog.data.model.Reminder
import com.artemkhateev.carlog.data.model.ReminderKind
import com.artemkhateev.carlog.ui.format.parseWhole
import java.time.LocalDate

enum class ReminderField { Type, Due }

/** Форма напоминания. Срок можно не вводить, если задан повтор: тогда он отсчитывается от сегодня. */
data class ReminderDraft(
    val id: Long = 0,
    val vehicleId: Long,
    val kind: ReminderKind = ReminderKind.Service,
    val typeId: Long? = null,
    val title: String = "",
    val dueOdometerText: String = "",
    val dueDate: LocalDate? = null,
    val repeatDistanceText: String = "",
    val repeatMonthsText: String = "",
    val notes: String = "",
    /** Переносится как есть: пока срок тот же, о нём второй раз не напомним. */
    val notifiedKey: String? = null,
) {
    val dueOdometer: Long? get() = parseWhole(dueOdometerText)
    val repeatDistance: Long? get() = parseWhole(repeatDistanceText)?.takeIf { it > 0 }
    val repeatMonths: Int? get() = parseWhole(repeatMonthsText)?.takeIf { it in 1L..1200L }?.toInt()

    /** Вид из справочника другого типа не подходит — при смене типа он сбрасывается. */
    fun withKind(kind: ReminderKind) = if (kind == this.kind) this else copy(kind = kind, typeId = null)

    fun suggestedOdometer(currentOdometer: Long?): Long? =
        if (dueOdometer == null && repeatDistance != null && currentOdometer != null) currentOdometer + repeatDistance!! else null

    fun suggestedDate(today: LocalDate): LocalDate? =
        if (dueDate == null) repeatMonths?.let { today.plusMonths(it.toLong()) } else null

    fun errors(currentOdometer: Long?, today: LocalDate): Set<ReminderField> = buildSet {
        if (typeId == null && title.isBlank()) add(ReminderField.Type)
        if ((dueOdometer ?: suggestedOdometer(currentOdometer)) == null && (dueDate ?: suggestedDate(today)) == null) add(ReminderField.Due)
    }

    fun toReminder(currentOdometer: Long?, today: LocalDate): Reminder? {
        if (errors(currentOdometer, today).isNotEmpty()) return null
        val reminder = Reminder(
            id = id,
            vehicleId = vehicleId,
            kind = kind,
            typeId = typeId,
            title = title.trim(),
            dueOdometer = dueOdometer ?: suggestedOdometer(currentOdometer),
            dueDate = dueDate ?: suggestedDate(today),
            repeatDistance = repeatDistance,
            repeatMonths = repeatMonths,
            notes = notes.trim(),
            notifiedKey = notifiedKey,
        )
        // Срок поменяли — о новом сроке уведомления ещё не было.
        return if (reminder.dueKey == notifiedKey) reminder else reminder.copy(notifiedKey = null)
    }

    companion object {
        fun from(reminder: Reminder) = ReminderDraft(
            id = reminder.id,
            vehicleId = reminder.vehicleId,
            kind = reminder.kind,
            typeId = reminder.typeId,
            title = reminder.title,
            dueOdometerText = reminder.dueOdometer?.toString().orEmpty(),
            dueDate = reminder.dueDate,
            repeatDistanceText = reminder.repeatDistance?.toString().orEmpty(),
            repeatMonthsText = reminder.repeatMonths?.toString().orEmpty(),
            notes = reminder.notes,
            notifiedKey = reminder.notifiedKey,
        )
    }
}
