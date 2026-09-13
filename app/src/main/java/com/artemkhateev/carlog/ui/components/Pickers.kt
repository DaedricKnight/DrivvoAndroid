package com.artemkhateev.carlog.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.domain.DateRange
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Шторка выбора одного варианта. [onNone] добавляет строку «Нет» для необязательных полей,
 * [onAdd] — строку «Добавить», которая спрашивает имя и сразу создаёт элемент справочника.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionSheet(
    title: String,
    options: List<T>,
    optionLabel: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    onNone: (() -> Unit)? = null,
    noneSelected: Boolean = false,
    onAdd: ((String) -> Unit)? = null,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CarLogTheme.colors.card,
    ) {
        SheetTitle(title)
        LazyColumn(Modifier.navigationBarsPadding()) {
            if (onNone != null) {
                item {
                    OptionRow(stringResource(R.string.none), noneSelected) {
                        onNone()
                        onDismiss()
                    }
                }
            }
            items(options) { option ->
                OptionRow(optionLabel(option), isSelected(option)) {
                    onSelect(option)
                    onDismiss()
                }
            }
            if (onAdd != null) {
                item { AddRow { adding = true } }
            }
        }
    }
    if (adding && onAdd != null) {
        NameDialog(
            title = stringResource(R.string.add_new),
            initial = "",
            onConfirm = { name ->
                adding = false
                onAdd(name)
                onDismiss()
            },
            onDismiss = { adding = false },
        )
    }
}

/**
 * Шторка выбора нескольких вариантов: виды сервиса или расхода в одной записи.
 * Закрывается кнопкой Done или жестом — в обоих случаях выбор сохраняется через [onDone].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MultiOptionSheet(
    title: String,
    options: List<T>,
    optionKey: (T) -> Long,
    optionLabel: (T) -> String,
    initiallySelected: Set<Long>,
    onDone: (Set<Long>) -> Unit,
    onAdd: ((String) -> Unit)? = null,
) {
    var selected by remember(initiallySelected) { mutableStateOf(initiallySelected) }
    var adding by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = { onDone(selected) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CarLogTheme.colors.card,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SheetTitle(title, Modifier.weight(1f))
            TextButton(onClick = { onDone(selected) }, modifier = Modifier.padding(end = 12.dp)) {
                Text(stringResource(R.string.action_done))
            }
        }
        LazyColumn(Modifier.navigationBarsPadding()) {
            items(options) { option ->
                val key = optionKey(option)
                val checked = key in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = if (checked) selected - key else selected + key }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = { selected = if (checked) selected - key else selected + key })
                    Text(
                        text = optionLabel(option),
                        style = CarLogTheme.typography.body,
                        color = CarLogTheme.colors.textPrimary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (onAdd != null) {
                item { AddRow { adding = true } }
            }
        }
    }
    if (adding && onAdd != null) {
        NameDialog(
            title = stringResource(R.string.add_new),
            initial = "",
            onConfirm = { name ->
                adding = false
                onAdd(name)
            },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun SheetTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = CarLogTheme.typography.cardTitle,
        color = CarLogTheme.colors.textPrimary,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = CarLogTheme.typography.body,
            color = CarLogTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = CarLogTheme.colors.brandText)
    }
}

@Composable
private fun AddRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = CarLogTheme.colors.brandText, modifier = Modifier.size(22.dp))
        Text(
            text = stringResource(R.string.add_new),
            style = CarLogTheme.typography.body,
            color = CarLogTheme.colors.brandText,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
fun NameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit, label: String? = null) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label ?: stringResource(R.string.name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) {
                Text(confirmLabel, color = if (destructive) CarLogTheme.colors.danger else Color.Unspecified)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogFor(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(it.toUtcDate()) }
                onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogFor(initial: LocalTime, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = DateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onPick(LocalTime.of(state.hour, state.minute))
                onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialogFor(initial: DateRange?, onPick: (DateRange) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial?.start?.toUtcMillis(),
        initialSelectedEndDateMillis = initial?.end?.toUtcMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis?.toUtcDate()
                    val end = state.selectedEndDateMillis?.toUtcDate() ?: start
                    if (start != null && end != null) onPick(DateRange(start, end))
                    onDismiss()
                },
                enabled = state.selectedStartDateMillis != null,
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DateRangePicker(state = state, modifier = Modifier.weight(1f))
    }
}

// Календари Material работают с полночью по UTC, а даты в приложении — без часового пояса.
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
