package com.artemkhateev.carlog.data

import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.Entry
import com.artemkhateev.carlog.data.model.Reminder
import com.artemkhateev.carlog.data.model.Vehicle
import com.artemkhateev.carlog.data.settings.SettingsRepository
import com.artemkhateev.carlog.domain.lastOdometer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class VehicleSelection(val vehicles: List<Vehicle>, val selected: Vehicle?)

/** Выбранная машина со всем, что к ней относится. */
data class VehicleData(
    val vehicles: List<Vehicle>,
    val vehicle: Vehicle,
    /** Новые сверху. */
    val entries: List<Entry>,
    val catalogs: Catalogs,
    val reminders: List<Reminder>,
) {
    val currentOdometer: Long? by lazy { lastOdometer(entries) }
}

/**
 * Одна подписка на базу для всех экранов: история, отчёты, напоминания и формы
 * читают один и тот же поток, а не запрашивают записи каждый сам.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CurrentVehicle(
    private val repository: CarLogRepository,
    private val settings: SettingsRepository,
    scope: CoroutineScope,
) {
    /** null — ещё не загрузилось. Выбрана машина из настроек, иначе первая активная. */
    val selection: StateFlow<VehicleSelection?> =
        combine(repository.vehicles, settings.settings) { vehicles, current ->
            VehicleSelection(
                vehicles = vehicles,
                selected = vehicles.firstOrNull { it.id == current.selectedVehicleId }
                    ?: vehicles.firstOrNull { it.active }
                    ?: vehicles.firstOrNull(),
            )
        }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, null)

    /** null — грузится или машин нет (это видно по [selection]). */
    val data: StateFlow<VehicleData?> = selection
        .filterNotNull()
        .flatMapLatest { current ->
            val vehicle = current.selected ?: return@flatMapLatest flowOf(null)
            combine(
                repository.entries(vehicle.id),
                repository.catalogs,
                repository.reminders(vehicle.id),
            ) { entries, catalogs, reminders ->
                VehicleData(current.vehicles, vehicle, entries, catalogs, reminders)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun select(vehicleId: Long) = settings.selectVehicle(vehicleId)
}
