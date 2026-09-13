package com.artemkhateev.carlog.data.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.domain.DueState
import com.artemkhateev.carlog.domain.displayTitle
import com.artemkhateev.carlog.domain.lastOdometer
import com.artemkhateev.carlog.domain.status
import com.artemkhateev.carlog.ui.format.Formats
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Проверяет сроки напоминаний всех активных машин и показывает уведомления о наступивших. */
class ReminderCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = AppGraph.repository
        val notifier = ReminderNotifier(applicationContext, Formats.from(AppGraph.settings.settings.first()))
        val catalogs = repository.catalogs.first()
        val today = LocalDate.now()
        for (vehicle in repository.vehicles.first().filter { it.active }) {
            val odometer = lastOdometer(repository.entriesSnapshot(vehicle.id))
            for (reminder in repository.remindersSnapshot(vehicle.id)) {
                val status = reminder.status(odometer, today)
                // О каждом сроке — одно уведомление; следующее — когда срок перенесут.
                if (status.state == DueState.Later || reminder.notifiedKey == reminder.dueKey) continue
                if (notifier.show(vehicle, reminder, reminder.displayTitle(catalogs), status)) {
                    repository.saveReminder(reminder.copy(notifiedKey = reminder.dueKey))
                }
            }
        }
        return Result.success()
    }
}
