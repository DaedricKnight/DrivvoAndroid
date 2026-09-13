package com.artemkhateev.carlog.feature.entry

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.PlaceKind
import com.artemkhateev.carlog.ui.components.DatePickerDialogFor
import com.artemkhateev.carlog.ui.components.FormPickerField
import com.artemkhateev.carlog.ui.components.FormRow
import com.artemkhateev.carlog.ui.components.FormTextField
import com.artemkhateev.carlog.ui.components.OptionSheet
import com.artemkhateev.carlog.ui.components.TimePickerDialogFor
import com.artemkhateev.carlog.ui.format.sanitizeWholeInput
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun FieldError?.message(): String? {
    val formats = CarLogTheme.formats
    return when (this) {
        null -> null
        FieldError.Required -> stringResource(R.string.required)
        FieldError.NotPositive -> stringResource(R.string.error_positive)
        is FieldError.BelowPrevious -> stringResource(R.string.error_below_previous, formats.distance(previous))
        is FieldError.AboveNext -> stringResource(R.string.error_above_next, formats.distance(next))
        FieldError.EndBeforeStart -> stringResource(R.string.error_end_before_start)
        FieldError.DiscountTooBig -> stringResource(R.string.error_discount_too_big)
        FieldError.NoTypes -> stringResource(R.string.error_pick_type)
    }
}

/** Машина записи: «Имя (Марка Модель)», как у референса. */
@Composable
fun VehicleField(state: EntryEditorUiState, onSelect: (Long) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val vehicle = state.vehicles.firstOrNull { it.id == state.draft.vehicleId }
    FormRow(Icons.Filled.DirectionsCar) {
        FormPickerField(
            label = stringResource(R.string.field_vehicle),
            value = vehicle?.let { if (it.description.isBlank() || it.description == it.name) it.name else "${it.name} (${it.description})" }.orEmpty(),
            onClick = { open = true },
        )
    }
    if (open) {
        OptionSheet(
            title = stringResource(R.string.field_vehicle),
            options = state.vehicles,
            optionLabel = { it.name },
            isSelected = { it.id == state.draft.vehicleId },
            onSelect = { onSelect(it.id) },
            onDismiss = { open = false },
        )
    }
}

@Composable
fun DateTimeFields(
    icon: ImageVector?,
    dateLabel: String,
    date: LocalDate,
    time: LocalTime,
    onDate: (LocalDate) -> Unit,
    onTime: (LocalTime) -> Unit,
    error: String? = null,
) {
    var dateOpen by rememberSaveable { mutableStateOf(false) }
    var timeOpen by rememberSaveable { mutableStateOf(false) }
    val formats = CarLogTheme.formats
    FormRow(icon) {
        FormPickerField(
            label = dateLabel,
            value = formats.date(date),
            onClick = { dateOpen = true },
            error = error,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(16.dp))
        FormPickerField(
            label = stringResource(R.string.field_time),
            value = formats.time(time),
            onClick = { timeOpen = true },
            modifier = Modifier.weight(1f),
        )
    }
    if (dateOpen) DatePickerDialogFor(date, onDate) { dateOpen = false }
    if (timeOpen) TimePickerDialogFor(time, onTime) { timeOpen = false }
}

/** Одометр с подсказкой «Last odometer» — последним показанием до даты записи. */
@Composable
fun OdometerField(
    icon: ImageVector?,
    label: String,
    text: String,
    onChange: (String) -> Unit,
    error: FieldError?,
    previous: Long?,
) {
    val formats = CarLogTheme.formats
    FormRow(icon) {
        FormTextField(
            value = text,
            onValueChange = { onChange(sanitizeWholeInput(it)) },
            label = label,
            keyboardType = KeyboardType.Number,
            error = error.message(),
            supporting = previous?.let { stringResource(R.string.field_last_odometer, formats.distance(it)) },
        )
    }
}

/** Выбор из справочника с «Нет» для необязательных полей и добавлением нового прямо из шторки. */
@Composable
fun CatalogField(
    icon: ImageVector?,
    label: String,
    kind: CatalogKind,
    selectedId: Long?,
    catalogs: Catalogs,
    onSelect: (Long?) -> Unit,
    onAdd: (String) -> Unit,
    error: String? = null,
    optional: Boolean = true,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    FormRow(icon) {
        FormPickerField(label = label, value = catalogs.item(selectedId)?.name.orEmpty(), onClick = { open = true }, error = error)
    }
    if (open) {
        OptionSheet(
            title = label,
            options = catalogs.itemsOf(kind),
            optionLabel = { it.name },
            isSelected = { it.id == selectedId },
            onSelect = { onSelect(it.id) },
            onDismiss = { open = false },
            onNone = if (optional) ({ onSelect(null) }) else null,
            noneSelected = selectedId == null,
            onAdd = onAdd,
        )
    }
}

/** Место записи. Места нужного вида — первыми: у заправки сверху заправки. */
@Composable
fun PlaceField(
    icon: ImageVector?,
    label: String,
    kind: PlaceKind,
    selectedId: Long?,
    catalogs: Catalogs,
    onSelect: (Long?) -> Unit,
    onAdd: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    FormRow(icon) {
        FormPickerField(label = label, value = catalogs.place(selectedId)?.name.orEmpty(), onClick = { open = true })
    }
    if (open) {
        OptionSheet(
            title = label,
            options = catalogs.places.sortedBy { it.kind != kind },
            optionLabel = { it.name },
            isSelected = { it.id == selectedId },
            onSelect = { onSelect(it.id) },
            onDismiss = { open = false },
            onNone = { onSelect(null) },
            noneSelected = selectedId == null,
            onAdd = onAdd,
        )
    }
}

@Composable
fun FuelField(selectedId: Long?, catalogs: Catalogs, onSelect: (Long?) -> Unit, onAdd: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val label = stringResource(R.string.field_fuel)
    FormRow(Icons.Filled.LocalGasStation) {
        FormPickerField(label = label, value = catalogs.fuel(selectedId)?.name.orEmpty(), onClick = { open = true })
    }
    if (open) {
        OptionSheet(
            title = label,
            options = catalogs.fuels,
            optionLabel = { it.name },
            isSelected = { it.id == selectedId },
            onSelect = { onSelect(it.id) },
            onDismiss = { open = false },
            onAdd = onAdd,
        )
    }
}

@Composable
fun NotesField(notes: String, onChange: (String) -> Unit) {
    FormRow(Icons.AutoMirrored.Filled.Notes) {
        FormTextField(
            value = notes,
            onValueChange = onChange,
            label = stringResource(R.string.field_notes),
            singleLine = false,
            imeAction = ImeAction.Default,
        )
    }
}

/** Итог формы крупно цветом записи. */
@Composable
fun TotalRow(label: String, value: String, accent: Color) {
    FormRow(null) {
        Row(
            Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = CarLogTheme.typography.body, color = CarLogTheme.colors.textSecondary, modifier = Modifier.weight(1f))
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
