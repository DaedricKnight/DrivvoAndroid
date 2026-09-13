package com.artemkhateev.carlog.feature.vehicles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    /** Черновик — состояние Compose, чтобы поля ввода получали свой текст в том же кадре. null — машина грузится. */
    var draft by mutableStateOf<VehicleDraft?>(null)
        private set

    /** Ошибки показываем после первой попытки сохранить, а не пока человек ещё печатает. */
    var showErrors by mutableStateOf(false)
        private set

    val fuels: StateFlow<List<Fuel>> = repository.catalogs.map { it.fuels }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currencyCode: StateFlow<String?> = settings.settings.map { it.currencyCode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val mutableDone = MutableStateFlow(false)

    /** Машина сохранена или удалена — экран пора закрыть. */
    val done: StateFlow<Boolean> = mutableDone.asStateFlow()

    /** Сохранение уже идёт: второе нажатие не должно создать вторую машину. */
    private var saving = false

    init {
        viewModelScope.launch {
            draft = if (vehicleId == 0L) {
                val count = repository.vehicles.first().size
                val gasoline = repository.catalogs.first().fuels.firstOrNull { it.category == FuelCategory.Gasoline }
                VehicleDraft(colorIndex = count % VehicleColors.size, fuelId = gasoline?.id)
            } else {
                repository.vehicle(vehicleId)?.let(VehicleDraft::from) ?: VehicleDraft()
            }
        }
    }

    fun update(change: (VehicleDraft) -> VehicleDraft) {
        draft = draft?.let(change)
    }

    fun setCurrency(code: String) {
        viewModelScope.launch { settings.setCurrency(code) }
    }

    fun save() {
        val current = draft ?: return
        val vehicle = current.toVehicle()
        if (vehicle == null) {
            showErrors = true
            return
        }
        if (saving) return
        saving = true
        viewModelScope.launch {
            val id = repository.saveVehicle(vehicle)
            if (vehicleId == 0L) {
                current.initialOdometer?.let { odometer ->
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
