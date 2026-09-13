package com.artemkhateev.carlog.feature.reminders

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val draft = MutableStateFlow<ReminderDraft?>(null)
    private val showErrors = MutableStateFlow(false)
    private val mutableDone = MutableStateFlow(false)

    /** Сохранено, выполнено или удалено — экран пора закрыть. */
    val done: StateFlow<Boolean> = mutableDone.asStateFlow()

    private var busy = false

    /** Пробег машины напоминания — не обязательно той, что выбрана сейчас. */
    private val odometer = draft
        .filterNotNull()
        .map { it.vehicleId }
        .distinctUntilChanged()
        .flatMapLatest { id -> repository.entries(id).map { lastOdometer(it) } }

    val state: StateFlow<ReminderEditorUiState?> =
        combine(draft.filterNotNull(), repository.catalogs, odometer, showErrors) { current, catalogs, odometer, show ->
            val date = today()
            ReminderEditorUiState(
                draft = current,
                isNew = reminderId == 0L,
                catalogs = catalogs,
                currentOdometer = odometer,
                today = date,
                errors = if (show) current.errors(odometer, date) else emptySet(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            val loaded = if (reminderId != 0L) {
                repository.reminder(reminderId)?.let(ReminderDraft::from)
            } else {
                ReminderDraft(vehicleId = currentVehicle.data.filterNotNull().first().vehicle.id)
            }
            if (loaded == null) mutableDone.value = true else draft.value = loaded
        }
    }

    fun update(change: (ReminderDraft) -> ReminderDraft) {
        draft.update { it?.let(change) }
    }

    /** Проверка до сохранения: разрешение на уведомления спрашиваем только у готового напоминания. */
    fun validate(): Boolean {
        val current = state.value ?: return false
        val valid = current.draft.errors(current.currentOdometer, current.today).isEmpty()
        if (!valid) showErrors.value = true
        return valid
    }

    fun save() {
        val current = state.value ?: return
        val reminder = current.draft.toReminder(current.currentOdometer, current.today)
        if (reminder == null) {
            showErrors.value = true
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
        val current = state.value ?: return
        val reminder = current.draft.toReminder(current.currentOdometer, current.today) ?: return
        if (busy || reminderId == 0L) return
        busy = true
        viewModelScope.launch {
            if (reminder.repeats) {
                repository.saveReminder(reminder.rescheduled(current.currentOdometer, current.today))
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
        val kind = draft.value?.kind ?: return
        viewModelScope.launch {
            val catalogKind = if (kind == ReminderKind.Service) CatalogKind.ServiceType else CatalogKind.ExpenseType
            val id = repository.saveCatalogItem(CatalogItem(kind = catalogKind, name = name))
            update { it.copy(typeId = id) }
        }
    }
}
