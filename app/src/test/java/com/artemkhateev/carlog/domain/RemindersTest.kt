package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.Reminder
import com.artemkhateev.carlog.data.model.ReminderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RemindersTest {

    private val today = LocalDate.of(2025, 3, 10)

    @Test
    fun `due state by distance`() {
        assertEquals(DueState.Later, dueStatus(20_000, null, currentOdometer = 19_000, today).state)
        assertEquals(DueState.Soon, dueStatus(20_000, null, currentOdometer = 19_600, today).state)
        assertEquals(DueState.Overdue, dueStatus(20_000, null, currentOdometer = 20_000, today).state)
        assertEquals(-150L, dueStatus(20_000, null, currentOdometer = 20_150, today).remainingDistance)
    }

    @Test
    fun `due state by date`() {
        assertEquals(DueState.Later, dueStatus(null, LocalDate.of(2025, 4, 30), null, today).state)
        assertEquals(DueState.Soon, dueStatus(null, LocalDate.of(2025, 3, 15), null, today).state)
        assertEquals(DueState.Soon, dueStatus(null, today, null, today).state)
        assertEquals(DueState.Overdue, dueStatus(null, LocalDate.of(2025, 3, 9), null, today).state)
    }

    @Test
    fun `whichever comes first decides`() {
        val status = dueStatus(30_000, LocalDate.of(2025, 3, 1), currentOdometer = 12_000, today)

        assertEquals(DueState.Overdue, status.state)
        assertEquals(18_000L, status.remainingDistance)
        assertEquals(-9L, status.remainingDays)
    }

    @Test
    fun `next refueling is forecast from the average step between refuelings`() {
        val forecast = forecastNextRefueling(
            listOf(
                refueling(id = 1, odometer = 10_000, milli = 40_000, dateTime = at(3, 1)),
                refueling(id = 2, odometer = 10_400, milli = 30_000, dateTime = at(3, 5)),
                refueling(id = 3, odometer = 10_900, milli = 35_000, dateTime = at(3, 11)),
            ),
        )!!

        assertEquals(11_350L, forecast.odometer)
        assertEquals(LocalDate.of(2025, 3, 16), forecast.date)
    }

    @Test
    fun `no forecast from a single refueling`() {
        assertNull(forecastNextRefueling(listOf(refueling(id = 1, odometer = 10_000, milli = 40_000))))
    }

    private val oilEvery10k = Reminder(
        id = 1,
        vehicleId = 1,
        kind = ReminderKind.Service,
        typeId = 7,
        dueOdometer = 52_000,
        dueDate = LocalDate.of(2025, 5, 1),
        repeatDistance = 10_000,
        repeatMonths = 12,
        notifiedKey = "52000|2025-05-01",
    )
    private val oneTimeOil = Reminder(id = 2, vehicleId = 1, kind = ReminderKind.Service, typeId = 7, dueOdometer = 51_000)
    private val otherService = Reminder(id = 3, vehicleId = 1, kind = ReminderKind.Service, typeId = 8, dueOdometer = 51_000)
    private val expenseOfSameId = Reminder(id = 4, vehicleId = 1, kind = ReminderKind.Expense, typeId = 7, dueOdometer = 51_000)
    private val reminders = listOf(oilEvery10k, oneTimeOil, otherService, expenseOfSameId)

    @Test
    fun `fresh service reschedules repeating reminders and closes one-time ones`() {
        val oilChange = service(id = 9, odometer = 50_000, dateTime = today.minusDays(2).atTime(10, 0), 7L to 6_000)

        val updates = remindersAfterEntry(oilChange, reminders, today, currentOdometer = 50_100)

        assertEquals(
            listOf(
                ReminderUpdate.Reschedule(
                    oilEvery10k.copy(dueOdometer = 60_000, dueDate = LocalDate.of(2026, 3, 8), notifiedKey = null),
                ),
                ReminderUpdate.Close(2),
            ),
            updates,
        )
    }

    @Test
    fun `entries added long after the fact leave reminders alone`() {
        val oldOilChange = service(id = 9, odometer = 40_000, dateTime = today.minusDays(40).atTime(10, 0), 7L to 6_000)

        assertEquals(emptyList<ReminderUpdate>(), remindersAfterEntry(oldOilChange, reminders, today, currentOdometer = 50_100))
    }

    @Test
    fun `expense without odometer reschedules from the current odometer`() {
        val tax = Reminder(id = 5, vehicleId = 1, kind = ReminderKind.Expense, typeId = 3, dueOdometer = 1, repeatDistance = 1_000)
        val payment = expense(id = 1, odometer = null, dateTime = today.atTime(9, 0), 3L to 12_000)

        val update = remindersAfterEntry(payment, listOf(tax), today, currentOdometer = 50_100).single()

        assertEquals(51_100L, (update as ReminderUpdate.Reschedule).reminder.dueOdometer)
    }
}
