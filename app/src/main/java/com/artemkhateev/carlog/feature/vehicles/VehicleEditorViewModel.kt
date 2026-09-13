package com.artemkhateev.carlog.feature.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemkhateev.carlog.data.CarLogRepository
import com.artemkhateev.carlog.data.CurrentVehicle
import com.artemkhateev.carlog.data.model.Fuel
import com.artemkhateev.carlog.data.model.FuelCategory
import com.artemkhateev.carlog.data.model.Reading
import com.artemkhateev.carlog.data.settings.SettingsRepository
import com.artemkhateev.carlog.ui.theme.VehicleColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class VehicleEditorViewModel(
    private val vehicleId: Long,
    private val repository: CarLogRepository,
    private val settings: SettingsRepository,
    private val currentVehicle: CurrentVehicle,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : ViewModel() {

    private val mutableDraft = MutableStateFlow<VehicleDraft?>(null)

    /** null — машина ещё грузится. */
    val draft: StateFlow<VehicleDraft?> = mutableDraft.asStateFlow()

    val fuels: StateFlow<List<Fuel>> = repository.catalogs.map { it.fuels }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currencyCode: StateFlow<String?> = settings.settings.map { it.currencyCode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val mutableShowErrors = MutableStateFlow(false)

    /** Ошибки показываем после первой попытки сохранить, а не пока человек ещё печатает. */
    val showErrors: StateFlow<Boolean> = mutableShowErrors.asStateFlow()

    private val mutableDone = MutableStateFlow(false)

    /** Машина сохранена или удалена — экран пора закрыть. */
    val done: StateFlow<Boolean> = mutableDone.asStateFlow()

    init {
        viewModelScope.launch {
            mutableDraft.value = if (vehicleId == 0L) {
                val count = repository.vehicles.first().size
                val gasoline = repository.catalogs.first().fuels.firstOrNull { it.category == FuelCategory.Gasoline }
                VehicleDraft(colorIndex = count % VehicleColors.size, fuelId = gasoline?.id)
            } else {
                repository.vehicle(vehicleId)?.let(VehicleDraft::from) ?: VehicleDraft()
            }
        }
    }

    fun update(change: (VehicleDraft) -> VehicleDraft) {
        mutableDraft.update { it?.let(change) }
    }

    fun setCurrency(code: String) {
        viewModelScope.launch { settings.setCurrency(code) }
    }

    /** Сохранение уже идёт: второе нажатие не должно создать вторую машину. */
    private var saving = false

    fun save() {
        val draft = mutableDraft.value ?: return
        val vehicle = draft.toVehicle()
        if (vehicle == null) {
            mutableShowErrors.value = true
            return
        }
        if (saving) return
        saving = true
        viewModelScope.launch {
            val id = repository.saveVehicle(vehicle)
            if (vehicleId == 0L) {
                draft.initialOdometer?.let { odometer ->
                    repository.saveEntry(Reading(vehicleId = id, dateTime = now().truncatedTo(ChronoUnit.MINUTES), odometer = odometer))
                }
                // Новую машину добавляли, чтобы вести: она сразу становится выбранной.
                currentVehicle.select(id)
            }
            mutableDone.value = true
        }
    }

    fun delete() {
        if (vehicleId == 0L) return
        viewModelScope.launch {
            repository.deleteVehicle(vehicleId)
            mutableDone.value = true
        }
    }
}
