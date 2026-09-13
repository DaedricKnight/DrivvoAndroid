package com.artemkhateev.carlog.feature.entry

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.data.model.RouteKind
import com.artemkhateev.carlog.ui.components.FormPickerField
import com.artemkhateev.carlog.ui.components.FormRow
import com.artemkhateev.carlog.ui.components.FormTextField
import com.artemkhateev.carlog.ui.components.SegmentedToggle
import com.artemkhateev.carlog.ui.format.sanitizeDecimalInput
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import com.artemkhateev.carlog.ui.theme.EntryColors

@Composable
fun IncomeForm(draft: IncomeDraft, state: EntryEditorUiState, viewModel: EntryEditorViewModel) {
    val errors = state.errors
    fun change(block: IncomeDraft.() -> IncomeDraft) = viewModel.update { (it as? IncomeDraft)?.block() ?: it }

    VehicleField(state) { id -> viewModel.update { it.withVehicle(id) } }
    DateTimeFields(
        icon = Icons.Filled.Event,
        dateLabel = stringResource(R.string.field_date),
        date = draft.date,
        time = draft.time,
        onDate = { date -> viewModel.update { it.withDate(date) } },
        onTime = { time -> viewModel.update { it.withTime(time) } },
    )
    OdometerField(
        icon = Icons.Filled.Speed,
        label = stringResource(R.string.optional_suffix, stringResource(R.string.field_odometer)),
        text = draft.odometerText,
        onChange = { value -> change { copy(odometerText = value) } },
        error = errors[DraftField.Odometer],
        previous = state.previousOdometer,
    )
    FormRow(Icons.Filled.Title) {
        FormTextField(value = draft.title, onValueChange = { value -> change { copy(title = value) } }, label = stringResource(R.string.field_title))
    }
    CatalogField(
        icon = Icons.Filled.Category,
        label = stringResource(R.string.field_income_type),
        kind = CatalogKind.IncomeType,
        selectedId = draft.typeId,
        catalogs = state.catalogs,
        onSelect = { id -> change { copy(typeId = id) } },
        onAdd = { name -> viewModel.addCatalogItem(CatalogKind.IncomeType, name) { current, id -> (current as? IncomeDraft)?.copy(typeId = id) ?: current } },
        error = errors[DraftField.IncomeType].message(),
        optional = false,
    )
    FormRow(Icons.Filled.Payments) {
        FormTextField(
            value = draft.amountText,
            onValueChange = { value -> change { copy(amountText = sanitizeDecimalInput(value, maxDecimals = 2)) } },
            label = stringResource(R.string.field_value),
            keyboardType = KeyboardType.Decimal,
            error = errors[DraftField.Amount].message(),
        )
    }
    DriverAndReason(draft.driverId, reasonId = draft.reasonId, state, viewModel, onReason = { id -> change { copy(reasonId = id) } }) { current, id ->
        (current as? IncomeDraft)?.copy(reasonId = id) ?: current
    }
    NotesField(draft.notes) { notes -> viewModel.update { it.withNotes(notes) } }
}

@Composable
fun RouteForm(draft: RouteDraft, state: EntryEditorUiState, viewModel: EntryEditorViewModel) {
    val errors = state.errors
    val formats = CarLogTheme.formats
    val accent = EntryColors.Route
    fun change(block: RouteDraft.() -> RouteDraft) = viewModel.update { (it as? RouteDraft)?.block() ?: it }

    VehicleField(state) { id -> viewModel.update { it.withVehicle(id) } }
    FormRow(Icons.Filled.TripOrigin) {
        FormTextField(
            value = draft.origin,
            onValueChange = { value -> change { copy(origin = value) } },
            label = stringResource(R.string.field_origin),
            capitalization = KeyboardCapitalization.Words,
        )
    }
    DateTimeFields(
        icon = Icons.Filled.Event,
        dateLabel = stringResource(R.string.field_start_date),
        date = draft.date,
        time = draft.time,
        onDate = { date -> viewModel.update { it.withDate(date) } },
        onTime = { time -> viewModel.update { it.withTime(time) } },
    )
    OdometerField(
        icon = Icons.Filled.Speed,
        label = stringResource(R.string.field_initial_odometer),
        text = draft.startOdometerText,
        onChange = { value -> change { copy(startOdometerText = value) } },
        error = errors[DraftField.StartOdometer],
        previous = state.previousOdometer,
    )
    FormRow(Icons.Filled.Flag) {
        FormTextField(
            value = draft.destination,
            onValueChange = { value -> change { copy(destination = value) } },
            label = stringResource(R.string.field_destination),
            capitalization = KeyboardCapitalization.Words,
        )
    }
    DateTimeFields(
        icon = Icons.Filled.Event,
        dateLabel = stringResource(R.string.field_end_date),
        date = draft.endDate,
        time = draft.endTime,
        onDate = { date -> change { withEndDate(date) } },
        onTime = { time -> change { withEndTime(time) } },
        error = errors[DraftField.End].message(),
    )
    OdometerField(
        icon = Icons.Filled.Speed,
        label = stringResource(R.string.field_final_odometer),
        text = draft.endOdometerText,
        onChange = { value -> change { copy(endOdometerText = value) } },
        error = errors[DraftField.EndOdometer],
        previous = null,
    )
    FormRow(null, Modifier.padding(vertical = 8.dp)) {
        SegmentedToggle(
            options = listOf(stringResource(R.string.route_trip), stringResource(R.string.route_freight)),
            selectedIndex = draft.kind.ordinal,
            onSelect = { index -> change { copy(kind = RouteKind.entries[index]) } },
            accent = accent,
            modifier = Modifier.weight(1f),
        )
    }
    FormRow(Icons.Filled.Payments) {
        when (draft.kind) {
            RouteKind.Trip -> {
                FormTextField(
                    value = draft.rateText,
                    onValueChange = { value -> change { copy(rateText = sanitizeDecimalInput(value, maxDecimals = 3)) } },
                    label = stringResource(R.string.field_rate_per_distance, formats.distanceLabel),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(16.dp))
                FormPickerField(
                    label = stringResource(R.string.field_total),
                    value = formats.money(draft.value),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
            RouteKind.Freight -> FormTextField(
                value = draft.freightText,
                onValueChange = { value -> change { copy(freightText = sanitizeDecimalInput(value, maxDecimals = 2)) } },
                label = stringResource(R.string.field_freight_value),
                keyboardType = KeyboardType.Decimal,
                error = errors[DraftField.FreightValue].message(),
            )
        }
    }
    DriverAndReason(draft.driverId, reasonId = draft.reasonId, state, viewModel, onReason = { id -> change { copy(reasonId = id) } }) { current, id ->
        (current as? RouteDraft)?.copy(reasonId = id) ?: current
    }
    NotesField(draft.notes) { notes -> viewModel.update { it.withNotes(notes) } }
}

@Composable
fun ReadingForm(draft: ReadingDraft, state: EntryEditorUiState, viewModel: EntryEditorViewModel) {
    VehicleField(state) { id -> viewModel.update { it.withVehicle(id) } }
    DateTimeFields(
        icon = Icons.Filled.Event,
        dateLabel = stringResource(R.string.field_date),
        date = draft.date,
        time = draft.time,
        onDate = { date -> viewModel.update { it.withDate(date) } },
        onTime = { time -> viewModel.update { it.withTime(time) } },
    )
    OdometerField(
        icon = Icons.Filled.Speed,
        label = stringResource(R.string.field_odometer),
        text = draft.odometerText,
        onChange = { value -> viewModel.update { (it as? ReadingDraft)?.copy(odometerText = value) ?: it } },
        error = state.errors[DraftField.Odometer],
        previous = state.previousOdometer,
    )
    CatalogField(
        icon = Icons.Filled.Badge,
        label = stringResource(R.string.field_driver),
        kind = CatalogKind.Driver,
        selectedId = draft.driverId,
        catalogs = state.catalogs,
        onSelect = { id -> viewModel.update { it.withDriver(id) } },
        onAdd = { name -> viewModel.addCatalogItem(CatalogKind.Driver, name) { current, id -> current.withDriver(id) } },
    )
    NotesField(draft.notes) { notes -> viewModel.update { it.withNotes(notes) } }
}

/** Водитель и цель поездки — одинаковые строки дохода и маршрута. */
@Composable
private fun DriverAndReason(
    driverId: Long?,
    reasonId: Long?,
    state: EntryEditorUiState,
    viewModel: EntryEditorViewModel,
    onReason: (Long?) -> Unit,
    selectNewReason: (EntryDraft, Long) -> EntryDraft,
) {
    CatalogField(
        icon = Icons.Filled.Badge,
        label = stringResource(R.string.field_driver),
        kind = CatalogKind.Driver,
        selectedId = driverId,
        catalogs = state.catalogs,
        onSelect = { id -> viewModel.update { it.withDriver(id) } },
        onAdd = { name -> viewModel.addCatalogItem(CatalogKind.Driver, name) { current, id -> current.withDriver(id) } },
    )
    CatalogField(
        icon = Icons.Filled.Work,
        label = stringResource(R.string.field_reason),
        kind = CatalogKind.Reason,
        selectedId = reasonId,
        catalogs = state.catalogs,
        onSelect = onReason,
        onAdd = { name -> viewModel.addCatalogItem(CatalogKind.Reason, name, selectNewReason) },
    )
}
