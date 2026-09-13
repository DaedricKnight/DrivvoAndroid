package com.artemkhateev.carlog.data

import android.annotation.SuppressLint
import android.content.Context
import com.artemkhateev.carlog.data.backup.DataTransfer
import com.artemkhateev.carlog.data.db.CarLogDatabase
import com.artemkhateev.carlog.data.makes.CarMakesCatalog
import com.artemkhateev.carlog.data.reminders.ReminderScheduler
import com.artemkhateev.carlog.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Единственное место, где создаются база, репозитории и фоновые службы. */
object AppGraph {

    // Здесь только applicationContext, он живёт столько же, сколько процесс.
    @SuppressLint("StaticFieldLeak")
    private lateinit var context: Context

    /** Для операций, которые должны пережить закрытый экран: сохранение, импорт копии. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    val database: CarLogDatabase by lazy { CarLogDatabase.build(context) }

    val repository: CarLogRepository by lazy { CarLogRepository(database.dao()) }

    val settings: SettingsRepository by lazy { SettingsRepository(context) }

    val currentVehicle: CurrentVehicle by lazy { CurrentVehicle(repository, settings, appScope) }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(context) }

    val dataTransfer: DataTransfer by lazy { DataTransfer(database.backupDao(), repository, context.contentResolver) }

    val carMakes: CarMakesCatalog by lazy { CarMakesCatalog { context.assets.open("car_makes.json") } }
}
