package com.artemkhateev.carlog.feature.reminders

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
import com.artemkhateev.carlog.data.model.ReminderKind
import com.artemkhateev.carlog.domain.lastOdometer
import com.artemkhateev.carlog.domain.rescheduled
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Всё, что форма показывает рядом с черновиком; собирается на экране. */
data class ReminderEditorUiState(
    val draft: ReminderDraft,
    val isNew: Boolean,
    val catalogs: Catalogs,
    val currentOdometer: Long?,
    val today: LocalDate,
    /** Показываются после первой попытки сохранить. */
    val errors: Set<ReminderField>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderEditorViewModel(
    private val reminderId: Long,
    private val repository: CarLogRepository,
    private val currentVehicle: CurrentVehicle,
    /** Напоминание поменялось — пусть проверка сроков пройдёт сразу, а не завтра. */
    private val onChanged: () -> Unit = {},
    val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** Черновик — состояние Compose, чтобы поля ввода получали свой текст в том же кадре. */
    var draft by mutableStateOf<ReminderDraft?>(null)
        private set

    var showErrors by mutableStateOf(false)
        private set

    val catalogs: StateFlow<Catalogs?> = repository.catalogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Пробег машины напоминания — не обязательно той, что выбрана сейчас. */
    val currentOdometer: StateFlow<Long?> = snapshotFlow { draft?.vehicleId }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { id -> repository.entries(id).map { lastOdometer(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val mutableDone = MutableStateFlow(false)

    /** Сохранено, выполнено или удалено — экран пора закрыть. */
    val done: StateFlow<Boolean> = mutableDone.asStateFlow()

    private var busy = false

    init {
        viewModelScope.launch {
            val loaded = if (reminderId != 0L) {
                repository.reminder(reminderId)?.let(ReminderDraft::from)
            } else {
                ReminderDraft(vehicleId = currentVehicle.data.filterNotNull().first().vehicle.id)
            }
            if (loaded == null) mutableDone.value = true else draft = loaded
        }
    }

    fun update(change: (ReminderDraft) -> ReminderDraft) {
        draft = draft?.let(change)
    }

    /** Проверка до сохранения: разрешение на уведомления спрашиваем только у готового напоминания. */
    fun validate(): Boolean {
        val current = draft ?: return false
        val valid = current.errors(currentOdometer.value, today()).isEmpty()
        if (!valid) showErrors = true
        return valid
    }

    fun save() {
        val reminder = draft?.toReminder(currentOdometer.value, today())
        if (reminder == null) {
            showErrors = true
            return
        }
        if (busy) return
        busy = true
        viewModelScope.launch {
            repository.saveReminder(reminder)
            onChanged()
            mutableDone.value = true
        }
    }

    /** Выполнено: повторяющееся переносится от сегодня и нынешнего пробега, разовое закрывается. */
    fun markDone() {
        val odometer = currentOdometer.value
        val date = today()
        val reminder = draft?.toReminder(odometer, date) ?: return
        if (busy || reminderId == 0L) return
        busy = true
        viewModelScope.launch {
            if (reminder.repeats) {
                repository.saveReminder(reminder.rescheduled(odometer, date))
            } else {
                repository.deleteReminder(reminderId)
            }
            onChanged()
            mutableDone.value = true
        }
    }

    fun delete() {
        if (busy || reminderId == 0L) return
        busy = true
        viewModelScope.launch {
            repository.deleteReminder(reminderId)
            mutableDone.value = true
        }
    }

    fun addType(name: String) {
        val kind = draft?.kind ?: return
        viewModelScope.launch {
            val catalogKind = if (kind == ReminderKind.Service) CatalogKind.ServiceType else CatalogKind.ExpenseType
            val id = repository.saveCatalogItem(CatalogItem(kind = catalogKind, name = name))
            update { it.copy(typeId = id) }
        }
    }
}
