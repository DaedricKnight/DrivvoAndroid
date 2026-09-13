package com.artemkhateev.carlog.feature.entry

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.data.model.PlaceKind
import com.artemkhateev.carlog.data.settings.VolumeUnit
import com.artemkhateev.carlog.ui.components.FormRow
import com.artemkhateev.carlog.ui.components.FormSwitchRow
import com.artemkhateev.carlog.ui.components.FormTextButton
import com.artemkhateev.carlog.ui.components.FormTextField
import com.artemkhateev.carlog.ui.components.accent
import com.artemkhateev.carlog.ui.theme.CarLogTheme

@Composable
fun RefuelingForm(draft: RefuelingDraft, state: EntryEditorUiState, viewModel: EntryEditorViewModel) {
    val formats = CarLogTheme.formats
    val accent = EntryType.Refueling.accent
    val errors = state.errors
    fun change(block: RefuelingDraft.() -> RefuelingDraft) = viewModel.update { (it as? RefuelingDraft)?.block() ?: it }
    // Редкие поля спрятаны, как у референса; у записи, где они заполнены, раскрыты сразу.
    var moreOptions by rememberSaveable {
        mutableStateOf(draft.missedPrevious || draft.paymentMethodId != null || draft.reasonId != null || draft.notes.isNotBlank())
    }

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
        onChange = { value -> change { copy(odometerText = value) } },
        error = errors[DraftField.Odometer],
        previous = state.previousOdometer,
    )
    FuelField(
        selectedId = draft.fuelId,
        catalogs = state.catalogs,
        onSelect = { id -> change { copy(fuelId = id) } },
        onAdd = { name -> viewModel.addFuel(name) { current, id -> (current as? RefuelingDraft)?.copy(fuelId = id) ?: current } },
    )
    FormRow(Icons.Filled.Payments) {
        FormTextField(
            value = draft.priceText,
            onValueChange = { value -> change { withPriceInput(PriceField.UnitPrice, value) } },
            label = stringResource(R.string.field_price_per_unit, formats.volumeLabel),
            keyboardType = KeyboardType.Decimal,
            error = errors[DraftField.UnitPrice].message(),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        FormTextField(
            value = draft.totalText,
            onValueChange = { value -> change { withPriceInput(PriceField.Total, value) } },
            label = stringResource(R.string.field_total_cost),
            keyboardType = KeyboardType.Decimal,
            error = errors[DraftField.Total].message(),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        FormTextField(
            value = draft.volumeText,
            onValueChange = { value -> change { withPriceInput(PriceField.Volume, value) } },
            label = stringResource(if (formats.volumeUnit == VolumeUnit.Liter) R.string.field_liters else R.string.field_gallons),
            keyboardType = KeyboardType.Decimal,
            error = errors[DraftField.Volume].message(),
            modifier = Modifier.weight(1f),
        )
    }
    FormSwitchRow(
        icon = Icons.Filled.LocalGasStation,
        text = stringResource(R.string.field_full_tank),
        checked = draft.fullTank,
        onCheckedChange = { checked -> change { copy(fullTank = checked) } },
        accent = accent,
    )
    PlaceField(
        icon = Icons.Filled.Place,
        label = stringResource(R.string.field_gas_station),
        kind = PlaceKind.GasStation,
        selectedId = draft.placeId,
        catalogs = state.catalogs,
        onSelect = { id -> change { copy(placeId = id) } },
        onAdd = { name ->
            viewModel.addPlace(name, PlaceKind.GasStation) { current, id -> (current as? RefuelingDraft)?.copy(placeId = id) ?: current }
        },
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
    if (!moreOptions) {
        FormTextButton(stringResource(R.string.more_options), Icons.Filled.Add, accent, onClick = { moreOptions = true })
        return
    }
    FormSwitchRow(
        icon = Icons.Filled.Restore,
        text = stringResource(R.string.field_missed_previous),
        checked = draft.missedPrevious,
        onCheckedChange = { checked -> change { copy(missedPrevious = checked) } },
        accent = accent,
    )
    CatalogField(
        icon = Icons.Filled.AttachMoney,
        label = stringResource(R.string.field_payment_method),
        kind = CatalogKind.PaymentMethod,
        selectedId = draft.paymentMethodId,
        catalogs = state.catalogs,
        onSelect = { id -> change { copy(paymentMethodId = id) } },
        onAdd = { name ->
            viewModel.addCatalogItem(CatalogKind.PaymentMethod, name) { current, id -> (current as? RefuelingDraft)?.copy(paymentMethodId = id) ?: current }
        },
    )
    CatalogField(
        icon = Icons.Filled.Work,
        label = stringResource(R.string.field_reason),
        kind = CatalogKind.Reason,
        selectedId = draft.reasonId,
        catalogs = state.catalogs,
        onSelect = { id -> change { copy(reasonId = id) } },
        onAdd = { name ->
            viewModel.addCatalogItem(CatalogKind.Reason, name) { current, id -> (current as? RefuelingDraft)?.copy(reasonId = id) ?: current }
        },
    )
    NotesField(draft.notes) { notes -> viewModel.update { it.withNotes(notes) } }
}
