package com.artemkhateev.carlog.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemkhateev.carlog.data.CurrentVehicle
import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.domain.DateRange
import com.artemkhateev.carlog.domain.PeriodPreset
import com.artemkhateev.carlog.domain.Reports
import com.artemkhateev.carlog.domain.buildReports
import com.artemkhateev.carlog.domain.toRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Период отчёта: пресет или свой диапазон дат. */
sealed interface PeriodChoice {
    data class Preset(val preset: PeriodPreset) : PeriodChoice
    data class Custom(val range: DateRange) : PeriodChoice
}

data class ReportsUiState(
    val choice: PeriodChoice,
    /** null — считать не из чего: записей нет. */
    val reports: Reports?,
    val catalogs: Catalogs,
    val hasTankCapacity: Boolean,
)

class ReportsViewModel(
    currentVehicle: CurrentVehicle,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val choice = MutableStateFlow<PeriodChoice>(PeriodChoice.Preset(PeriodPreset.AllTime))

    val state: StateFlow<ReportsUiState?> =
        combine(currentVehicle.data.filterNotNull(), choice) { data, current ->
            val range = when (current) {
                is PeriodChoice.Preset -> current.preset.toRange(today(), data.entries)
                is PeriodChoice.Custom -> current.range
            }
            ReportsUiState(
                choice = current,
                reports = range?.let { buildReports(data.entries, it, data.vehicle.tankCapacity) },
                catalogs = data.catalogs,
                hasTankCapacity = data.vehicle.tankCapacity != null,
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun choose(period: PeriodChoice) {
        choice.value = period
    }
}
