package com.artemkhateev.carlog.feature.reminders

import com.artemkhateev.carlog.data.model.ReminderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReminderDraftTest {

    private val today = LocalDate.of(2025, 3, 10)
    private val empty = ReminderDraft(vehicleId = 1, typeId = 4)

    @Test
    fun `repeat without a due date starts counting from now`() {
        val reminder = empty.copy(repeatDistanceText = "10000", repeatMonthsText = "12").toReminder(currentOdometer = 45_300, today = today)!!

        assertEquals(55_300L, reminder.dueOdometer)
        assertEquals(LocalDate.of(2026, 3, 10), reminder.dueDate)
        assertEquals(10_000L, reminder.repeatDistance)
        assertEquals(12, reminder.repeatMonths)
    }

    @Test
    fun `explicit due wins over the suggestion`() {
        val reminder = empty.copy(dueOdometerText = "50000", repeatDistanceText = "10000").toReminder(currentOdometer = 45_300, today = today)!!

        assertEquals(50_000L, reminder.dueOdometer)
        assertNull(reminder.dueDate)
    }

    @Test
    fun `needs a type or a title and some due`() {
        assertEquals(setOf(ReminderField.Type, ReminderField.Due), ReminderDraft(vehicleId = 1).errors(currentOdometer = 1_000, today = today))
        assertEquals(emptySet<ReminderField>(), ReminderDraft(vehicleId = 1, title = "Vignette", dueDate = today).errors(null, today))
        // Повтор по пробегу без единого показания одометра срока не даёт.
        assertEquals(setOf(ReminderField.Due), empty.copy(repeatDistanceText = "5000").errors(currentOdometer = null, today = today))
    }

    @Test
    fun `switching the kind drops a type from the other catalog`() {
        assertNull(empty.withKind(ReminderKind.Expense).typeId)
        assertEquals(4L, empty.withKind(ReminderKind.Service).typeId)
    }

    @Test
    fun `notification mark survives only while the due stays the same`() {
        val notified = empty.copy(dueOdometerText = "50000", notifiedKey = "50000|")

        assertEquals("50000|", notified.toReminder(45_000, today)!!.notifiedKey)
        assertNull(notified.copy(dueOdometerText = "51000").toReminder(45_000, today)!!.notifiedKey)
    }
}
