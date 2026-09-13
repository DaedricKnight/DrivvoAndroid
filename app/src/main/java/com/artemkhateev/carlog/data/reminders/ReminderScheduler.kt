package com.artemkhateev.carlog.data.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /** Раз в день: сроки по датам наступают и без новых записей. */
    fun scheduleDaily() {
        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderCheckWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    /** После записи или правки напоминания: срок по пробегу мог наступить прямо сейчас. */
    fun checkNow() {
        workManager.enqueueUniqueWork(NOW_WORK, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<ReminderCheckWorker>().build())
    }

    private companion object {
        const val DAILY_WORK = "reminders-daily"
        const val NOW_WORK = "reminders-now"
    }
}
