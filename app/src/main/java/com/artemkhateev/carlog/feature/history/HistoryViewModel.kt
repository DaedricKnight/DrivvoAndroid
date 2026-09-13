package com.artemkhateev.carlog.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemkhateev.carlog.data.CurrentVehicle
import com.artemkhateev.carlog.data.VehicleData
import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Reminder
import com.artemkhateev.carlog.domain.DueStatus
import com.artemkhateev.carlog.domain.HistoryRow
import com.artemkhateev.carlog.domain.RefuelingForecast
import com.artemkhateev.carlog.domain.buildHistory
import com.artemkhateev.carlog.domain.displayTitle
import com.artemkhateev.carlog.domain.dueStatus
import com.artemkhateev.carlog.domain.forecastNextRefueling
import com.artemkhateev.carlog.domain.status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

sealed interface ReminderCardUi {
    val status: DueStatus

    data class NextRefueling(val forecast: RefuelingForecast, override val status: DueStatus) : ReminderCardUi

    data class Scheduled(val reminder: Reminder, val title: String, override val status: DueStatus) : ReminderCardUi
}

data class HistoryUiState(
    val rows: List<HistoryRow>,
    val reminders: List<ReminderCardUi>,
    val catalogs: Catalogs,
)

/** Прогноз заправки всегда первым, дальше напоминания: сначала просроченные, потом ближайшие. */
fun buildReminderCards(data: VehicleData, today: LocalDate): List<ReminderCardUi> {
    val odometer = data.currentOdometer
    val forecast = forecastNextRefueling(data.entries.filterIsInstance<Refueling>())
        ?.let { ReminderCardUi.NextRefueling(it, dueStatus(it.odometer, it.date, odometer, today)) }
    val scheduled = data.reminders
        .map { ReminderCardUi.Scheduled(it, it.displayTitle(data.catalogs), it.status(odometer, today)) }
        .sortedWith(
            compareBy(
                { it.status.state },
                { it.status.remainingDays ?: Long.MAX_VALUE },
                { it.status.remainingDistance ?: Long.MAX_VALUE },
            ),
        )
    return listOfNotNull(forecast) + scheduled
}

class HistoryViewModel(
    currentVehicle: CurrentVehicle,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** null — данные ещё не пришли. */
    val state: StateFlow<HistoryUiState?> = currentVehicle.data
        .filterNotNull()
        .map { data ->
            HistoryUiState(
                rows = buildHistory(data.entries, data.vehicle.tankCapacity),
                reminders = buildReminderCards(data, today()),
                catalogs = data.catalogs,
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
