package com.artemkhateev.carlog.feature.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.data.model.ReminderKind
import com.artemkhateev.carlog.feature.entry.CatalogField
import com.artemkhateev.carlog.feature.entry.NotesField
import com.artemkhateev.carlog.ui.components.ConfirmDialog
import com.artemkhateev.carlog.ui.components.DatePickerDialogFor
import com.artemkhateev.carlog.ui.components.EditorScaffold
import com.artemkhateev.carlog.ui.components.FormPickerField
import com.artemkhateev.carlog.ui.components.FormRow
import com.artemkhateev.carlog.ui.components.FormTextButton
import com.artemkhateev.carlog.ui.components.FormTextField
import com.artemkhateev.carlog.ui.components.SectionLabel
import com.artemkhateev.carlog.ui.components.SegmentedToggle
import com.artemkhateev.carlog.ui.format.sanitizeWholeInput
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.navigation.ReminderEditorRoute
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import com.artemkhateev.carlog.ui.theme.EntryColors

@Composable
fun ReminderEditorScreen(route: ReminderEditorRoute, navigator: AppNavigator) {
    val viewModel: ReminderEditorViewModel = viewModel {
        ReminderEditorViewModel(route.id, AppGraph.repository, AppGraph.currentVehicle, onChanged = AppGraph.reminderScheduler::checkNow)
    }
    val draft = viewModel.draft
    val catalogs = viewModel.catalogs.collectAsStateWithLifecycle().value
    val currentOdometer by viewModel.currentOdometer.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    LaunchedEffect(done) {
        if (done) navigator.back()
    }
    val context = LocalContext.current
    // Отказ в разрешении сохранению не мешает: напоминание видно в приложении и без уведомлений.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.save() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDone by rememberSaveable { mutableStateOf(false) }
    val accent = EntryColors.Reminder

    EditorScaffold(
        title = stringResource(if (route.id == 0L) R.string.reminder_new else R.string.reminder_edit),
        accent = accent,
        onBack = navigator::back,
        onSave = {
            if (viewModel.validate()) {
                if (needsNotificationPermission(context)) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.save()
                }
            }
        },
        onDelete = if (route.id == 0L) null else ({ confirmDelete = true }),
    ) {
        if (draft != null && catalogs != null) {
            val today = viewModel.today()
            val state = ReminderEditorUiState(
                draft = draft,
                isNew = route.id == 0L,
                catalogs = catalogs,
                currentOdometer = currentOdometer,
                today = today,
                errors = if (viewModel.showErrors) draft.errors(currentOdometer, today) else emptySet(),
            )
            ReminderForm(state, viewModel, onMarkDone = { confirmDone = true })
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            message = stringResource(R.string.delete_entry_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = viewModel::delete,
            onDismiss = { confirmDelete = false },
        )
    }
    if (confirmDone) {
        ConfirmDialog(
            title = stringResource(R.string.reminder_mark_done),
            message = stringResource(R.string.reminder_done_message),
            confirmLabel = stringResource(R.string.action_ok),
            onConfirm = viewModel::markDone,
            onDismiss = { confirmDone = false },
            destructive = false,
        )
    }
}

@Composable
private fun ReminderForm(state: ReminderEditorUiState, viewModel: ReminderEditorViewModel, onMarkDone: () -> Unit) {
    val draft = state.draft
    val colors = CarLogTheme.colors
    val formats = CarLogTheme.formats
    val accent = EntryColors.Reminder
    var dateOpen by rememberSaveable { mutableStateOf(false) }

    FormRow(null, Modifier.padding(vertical = 8.dp)) {
        SegmentedToggle(
            options = listOf(stringResource(R.string.entry_service), stringResource(R.string.entry_expense)),
            selectedIndex = draft.kind.ordinal,
            onSelect = { index -> viewModel.update { it.withKind(ReminderKind.entries[index]) } },
            accent = accent,
            modifier = Modifier.weight(1f),
        )
    }
    CatalogField(
        icon = Icons.Filled.Category,
        label = stringResource(R.string.reminder_type),
        kind = if (draft.kind == ReminderKind.Service) CatalogKind.ServiceType else CatalogKind.ExpenseType,
        selectedId = draft.typeId,
        catalogs = state.catalogs,
        onSelect = { id -> viewModel.update { it.copy(typeId = id) } },
        onAdd = viewModel::addType,
        error = if (ReminderField.Type in state.errors) stringResource(R.string.reminder_need_type) else null,
    )
    FormRow(Icons.Filled.Title) {
        FormTextField(
            value = draft.title,
            onValueChange = { value -> viewModel.update { it.copy(title = value) } },
            label = stringResource(R.string.optional_suffix, stringResource(R.string.field_title)),
        )
    }
    FormRow(Icons.Filled.Speed) {
        FormTextField(
            value = draft.dueOdometerText,
            onValueChange = { value -> viewModel.update { it.copy(dueOdometerText = sanitizeWholeInput(value)) } },
            label = stringResource(R.string.reminder_due_odometer),
            keyboardType = KeyboardType.Number,
            suffix = formats.distanceLabel,
            error = if (ReminderField.Due in state.errors) stringResource(R.string.reminder_need_due) else null,
            supporting = state.currentOdometer?.let { stringResource(R.string.field_last_odometer, formats.distance(it)) },
        )
    }
    FormRow(Icons.Filled.Event) {
        FormPickerField(
            label = stringResource(R.string.reminder_due_date),
            value = draft.dueDate?.let { formats.date(it) }.orEmpty(),
            onClick = { dateOpen = true },
            modifier = Modifier.weight(1f),
        )
        if (draft.dueDate != null) {
            IconButton(onClick = { viewModel.update { it.copy(dueDate = null) } }) {
                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear), tint = colors.icon)
            }
        }
    }
    SectionLabel(stringResource(R.string.reminder_repeat), Modifier.padding(start = 64.dp, top = 20.dp, bottom = 4.dp))
    FormRow(Icons.Filled.Repeat) {
        FormTextField(
            value = draft.repeatDistanceText,
            onValueChange = { value -> viewModel.update { it.copy(repeatDistanceText = sanitizeWholeInput(value)) } },
            label = stringResource(R.string.reminder_repeat_distance, formats.distanceLabel),
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(16.dp))
        FormTextField(
            value = draft.repeatMonthsText,
            onValueChange = { value -> viewModel.update { it.copy(repeatMonthsText = sanitizeWholeInput(value, maxDigits = 4)) } },
            label = stringResource(R.string.reminder_repeat_months),
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
    }
    val suggestions = listOfNotNull(
        draft.suggestedOdometer(state.currentOdometer)?.let { formats.distance(it) },
        draft.suggestedDate(state.today)?.let { formats.date(it) },
    )
    Text(
        text = if (suggestions.isEmpty()) {
            stringResource(R.string.reminder_repeat_hint)
        } else {
            stringResource(R.string.reminder_suggested, suggestions.joinToString(" · "))
        },
        style = CarLogTheme.typography.caption,
        color = colors.textSecondary,
        modifier = Modifier.padding(start = 64.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
    )
    NotesField(draft.notes) { notes -> viewModel.update { it.copy(notes = notes) } }
    if (!state.isNew) {
        FormTextButton(stringResource(R.string.reminder_mark_done), Icons.Filled.Check, accent, onClick = onMarkDone)
    }

    if (dateOpen) {
        DatePickerDialogFor(
            initial = draft.dueDate ?: draft.suggestedDate(state.today) ?: state.today,
            onPick = { date -> viewModel.update { it.copy(dueDate = date) } },
            onDismiss = { dateOpen = false },
        )
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
