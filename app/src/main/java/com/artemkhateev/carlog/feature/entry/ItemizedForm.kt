package com.artemkhateev.carlog.feature.entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.data.model.PlaceKind
import com.artemkhateev.carlog.ui.components.FormPickerField
import com.artemkhateev.carlog.ui.components.FormRow
import com.artemkhateev.carlog.ui.components.FormTextField
import com.artemkhateev.carlog.ui.components.MultiOptionSheet
import com.artemkhateev.carlog.ui.components.accent
import com.artemkhateev.carlog.ui.format.sanitizeDecimalInput
import com.artemkhateev.carlog.ui.theme.CarLogTheme

/** Форма расхода и сервиса: виды из справочника, у каждого своя сумма, общая скидка. */
@Composable
fun ItemizedForm(draft: ItemizedDraft, state: EntryEditorUiState, viewModel: EntryEditorViewModel) {
    val isService = draft.type == EntryType.Service
    val kind = if (isService) CatalogKind.ServiceType else CatalogKind.ExpenseType
    val accent = draft.type.accent
    val errors = state.errors
    val catalogs = state.catalogs
    fun change(block: ItemizedDraft.() -> ItemizedDraft) = viewModel.update { (it as? ItemizedDraft)?.block() ?: it }
    var typesOpen by rememberSaveable { mutableStateOf(false) }
    val typesLabel = stringResource(if (isService) R.string.field_service_types else R.string.field_expense_types)
    val odometerLabel = stringResource(R.string.field_odometer)

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
        label = if (isService) odometerLabel else stringResource(R.string.optional_suffix, odometerLabel),
        text = draft.odometerText,
        onChange = { value -> change { copy(odometerText = value) } },
        error = errors[DraftField.Odometer],
        previous = state.previousOdometer,
    )
    FormRow(Icons.Filled.Title) {
        FormTextField(
            value = draft.title,
            onValueChange = { value -> change { copy(title = value) } },
            label = stringResource(R.string.field_title),
        )
    }
    FormRow(Icons.Filled.Category) {
        FormPickerField(
            label = typesLabel,
            value = draft.items.mapNotNull { catalogs.item(it.typeId)?.name }.joinToString(", "),
            onClick = { typesOpen = true },
            error = errors[DraftField.Items].message(),
        )
    }
    draft.items.forEach { item ->
        FormRow(null) {
            FormTextField(
                value = item.amountText,
                onValueChange = { value -> change { withItemAmount(item.typeId, value) } },
                label = catalogs.item(item.typeId)?.name.orEmpty(),
                keyboardType = KeyboardType.Decimal,
                error = if (item.amountText.isBlank()) errors[DraftField.ItemAmount].message() else null,
            )
        }
    }
    FormRow(Icons.Filled.Discount) {
        FormTextField(
            value = draft.discountText,
            onValueChange = { value -> change { copy(discountText = sanitizeDecimalInput(value, maxDecimals = 2)) } },
            label = stringResource(R.string.field_discount),
            keyboardType = KeyboardType.Decimal,
            error = errors[DraftField.Discount].message(),
        )
    }
    if (draft.items.isNotEmpty()) {
        TotalRow(stringResource(R.string.field_total), CarLogTheme.formats.money(draft.total), accent)
    }
    PlaceField(
        icon = Icons.Filled.Place,
        label = stringResource(R.string.field_place),
        kind = PlaceKind.Other,
        selectedId = draft.placeId,
        catalogs = catalogs,
        onSelect = { id -> change { copy(placeId = id) } },
        onAdd = { name -> viewModel.addPlace(name, PlaceKind.Other) { current, id -> (current as? ItemizedDraft)?.copy(placeId = id) ?: current } },
    )
    CatalogField(
        icon = Icons.Filled.Badge,
        label = stringResource(R.string.field_driver),
        kind = CatalogKind.Driver,
        selectedId = draft.driverId,
        catalogs = catalogs,
        onSelect = { id -> viewModel.update { it.withDriver(id) } },
        onAdd = { name -> viewModel.addCatalogItem(CatalogKind.Driver, name) { current, id -> current.withDriver(id) } },
    )
    CatalogField(
        icon = Icons.Filled.AttachMoney,
        label = stringResource(R.string.field_payment_method),
        kind = CatalogKind.PaymentMethod,
        selectedId = draft.paymentMethodId,
        catalogs = catalogs,
        onSelect = { id -> change { copy(paymentMethodId = id) } },
        onAdd = { name ->
            viewModel.addCatalogItem(CatalogKind.PaymentMethod, name) { current, id -> (current as? ItemizedDraft)?.copy(paymentMethodId = id) ?: current }
        },
    )
    CatalogField(
        icon = Icons.Filled.Work,
        label = stringResource(R.string.field_reason),
        kind = CatalogKind.Reason,
        selectedId = draft.reasonId,
        catalogs = catalogs,
        onSelect = { id -> change { copy(reasonId = id) } },
        onAdd = { name ->
            viewModel.addCatalogItem(CatalogKind.Reason, name) { current, id -> (current as? ItemizedDraft)?.copy(reasonId = id) ?: current }
        },
    )
    NotesField(draft.notes) { notes -> viewModel.update { it.withNotes(notes) } }

    if (typesOpen) {
        MultiOptionSheet(
            title = typesLabel,
            options = catalogs.itemsOf(kind),
            optionKey = { it.id },
            optionLabel = { it.name },
            initiallySelected = draft.items.map { it.typeId }.toSet(),
            onDone = { ids ->
                change { withTypes(ids) }
                typesOpen = false
            },
            onAdd = { name ->
                viewModel.addCatalogItem(kind, name) { current, id ->
                    (current as? ItemizedDraft)?.let { itemized -> itemized.withTypes(itemized.items.map { it.typeId }.toSet() + id) } ?: current
                }
            },
        )
    }
}
