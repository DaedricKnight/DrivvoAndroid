package com.artemkhateev.carlog.feature.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** Всё, что форма показывает рядом с черновиком. Собирается на экране из черновика и [EditorData]. */
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

/** То, что меняется не от ввода: записи машины из формы, машины и справочники. */
data class EditorData(val entries: List<Entry>, val vehicles: List<Vehicle>, val catalogs: Catalogs)

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

    /**
     * Черновик — состояние Compose, а не Flow: поле ввода должно получить свой текст в том же кадре.
     * Пока значение идёт через поток, поле успевает показать старый текст и сбрасывает курсор в начало.
     */
    var draft by mutableStateOf<EntryDraft?>(null)
        private set

    /** Ошибки показываем после первой попытки сохранить, а не пока человек ещё печатает. */
    var showErrors by mutableStateOf(false)
        private set

    private val mutableDone = MutableStateFlow(false)

    /** Запись сохранена или удалена — экран пора закрыть. */
    val done: StateFlow<Boolean> = mutableDone.asStateFlow()

    /** Записи той машины, что выбрана в форме: машину можно сменить прямо в ней. */
    val data: StateFlow<EditorData?> = snapshotFlow { draft?.vehicleId }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { vehicleId ->
            combine(repository.entries(vehicleId), repository.vehicles, repository.catalogs) { entries, vehicles, catalogs ->
                EditorData(entries, vehicles, catalogs)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Сохранение уже идёт: второе нажатие не должно создать вторую запись. */
    private var saving = false

    init {
        viewModelScope.launch {
            val loaded = if (entryId != 0L) {
                repository.entry(type, entryId)?.toDraft()
            } else {
                val current = currentVehicle.data.filterNotNull().first()
                newDraft(type, current.vehicle, current.entries, now())
            }
            // Запись могли удалить, пока экран открывался.
            if (loaded == null) mutableDone.value = true else draft = loaded
        }
    }

    fun update(change: (EntryDraft) -> EntryDraft) {
        draft = draft?.let(change)
    }

    fun boundsFor(entries: List<Entry>) = BoundsAt { at ->
        odometerBounds(entries, at, editing = if (entryId != 0L) type to entryId else null)
    }

    fun save() {
        val current = draft ?: return
        if (saving) return
        saving = true
        viewModelScope.launch {
            val entries = repository.entries(current.vehicleId).first()
            val entry = current.toEntry(boundsFor(entries))
            if (entry == null) {
                showErrors = true
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

    private suspend fun rescheduleReminders(entry: Entry, entriesBefore: List<Entry>) {
        val reminders = repository.remindersSnapshot(entry.vehicleId)
        val odometer = lastOdometer(entriesBefore + entry)
        repository.applyReminderUpdates(remindersAfterEntry(entry, reminders, LocalDate.now(), odometer))
    }
}
