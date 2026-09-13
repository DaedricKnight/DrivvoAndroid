package com.artemkhateev.carlog.feature.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemkhateev.carlog.data.CarLogRepository
import com.artemkhateev.carlog.data.CurrentVehicle
import com.artemkhateev.carlog.data.model.CatalogItem
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.Entry
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.data.model.Fuel
import com.artemkhateev.carlog.data.model.FuelCategory
import com.artemkhateev.carlog.data.model.Place
import com.artemkhateev.carlog.data.model.PlaceKind
import com.artemkhateev.carlog.data.model.Vehicle
import com.artemkhateev.carlog.domain.lastOdometer
import com.artemkhateev.carlog.domain.odometerBounds
import com.artemkhateev.carlog.domain.remindersAfterEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class EntryEditorUiState(
    val draft: EntryDraft,
    val isNew: Boolean,
    val vehicles: List<Vehicle>,
    val catalogs: Catalogs,
    /** Показываются после первой попытки сохранить. */
    val errors: Map<DraftField, FieldError>,
    /** Последнее показание до даты записи — подсказка под одометром. */
    val previousOdometer: Long?,
    /** Показание до конца маршрута — подсказка под конечным одометром. */
    val previousEndOdometer: Long?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class EntryEditorViewModel(
    private val type: EntryType,
    private val entryId: Long,
    private val repository: CarLogRepository,
    private val currentVehicle: CurrentVehicle,
    /** Записи поменялись — сроки напоминаний по пробегу стоит проверить сразу. */
    private val onSaved: () -> Unit = {},
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : ViewModel() {

    private val draft = MutableStateFlow<EntryDraft?>(null)
    private val showErrors = MutableStateFlow(false)
    private val mutableDone = MutableStateFlow(false)

    /** Запись сохранена или удалена — экран пора закрыть. */
    val done: StateFlow<Boolean> = mutableDone.asStateFlow()

    /** Записи той машины, что выбрана в форме: машину можно сменить прямо в ней. */
    private val vehicleEntries = draft
        .filterNotNull()
        .map { it.vehicleId }
        .distinctUntilChanged()
        .flatMapLatest { repository.entries(it) }

    val state: StateFlow<EntryEditorUiState?> =
        combine(draft.filterNotNull(), vehicleEntries, repository.vehicles, repository.catalogs, showErrors) { current, entries, vehicles, catalogs, show ->
            val bounds = boundsFor(entries)
            EntryEditorUiState(
                draft = current,
                isNew = entryId == 0L,
                vehicles = vehicles,
                catalogs = catalogs,
                errors = if (show) current.errors(bounds) else emptyMap(),
                previousOdometer = bounds.at(current.dateTime).min,
                previousEndOdometer = (current as? RouteDraft)?.let { bounds.at(it.end).min },
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            val loaded = if (entryId != 0L) {
                repository.entry(type, entryId)?.toDraft()
            } else {
                val data = currentVehicle.data.filterNotNull().first()
                newDraft(type, data.vehicle, data.entries, now())
            }
            // Запись могли удалить, пока экран открывался.
            if (loaded == null) mutableDone.value = true else draft.value = loaded
        }
    }

    fun update(change: (EntryDraft) -> EntryDraft) {
        draft.update { it?.let(change) }
    }

    /** Сохранение уже идёт: второе нажатие не должно создать вторую запись. */
    private var saving = false

    fun save() {
        if (saving) return
        saving = true
        viewModelScope.launch {
            val current = draft.value
            val entries = current?.let { repository.entries(it.vehicleId).first() }
            val entry = if (current != null && entries != null) current.toEntry(boundsFor(entries)) else null
            if (entry == null || entries == null) {
                showErrors.value = true
                saving = false
                return@launch
            }
            repository.saveEntry(entry)
            rescheduleReminders(entry, entries)
            onSaved()
            mutableDone.value = true
        }
    }

    fun delete() {
        if (entryId == 0L) return
        viewModelScope.launch {
            repository.deleteEntry(type, entryId)
            mutableDone.value = true
        }
    }

    /** Новый элемент справочника прямо из формы — и сразу выбранный в ней. */
    fun addCatalogItem(kind: CatalogKind, name: String, select: (EntryDraft, Long) -> EntryDraft) {
        viewModelScope.launch {
            val id = repository.saveCatalogItem(CatalogItem(kind = kind, name = name))
            update { select(it, id) }
        }
    }

    fun addPlace(name: String, kind: PlaceKind, select: (EntryDraft, Long) -> EntryDraft) {
        viewModelScope.launch {
            val id = repository.savePlace(Place(name = name, kind = kind))
            update { select(it, id) }
        }
    }

    fun addFuel(name: String, select: (EntryDraft, Long) -> EntryDraft) {
        viewModelScope.launch {
            val id = repository.saveFuel(Fuel(name = name, category = FuelCategory.Other))
            update { select(it, id) }
        }
    }

    private fun boundsFor(entries: List<Entry>) = BoundsAt { at ->
        odometerBounds(entries, at, editing = if (entryId != 0L) type to entryId else null)
    }

    private suspend fun rescheduleReminders(entry: Entry, entriesBefore: List<Entry>) {
        val reminders = repository.remindersSnapshot(entry.vehicleId)
        val odometer = lastOdometer(entriesBefore + entry)
        repository.applyReminderUpdates(remindersAfterEntry(entry, reminders, LocalDate.now(), odometer))
    }
}
