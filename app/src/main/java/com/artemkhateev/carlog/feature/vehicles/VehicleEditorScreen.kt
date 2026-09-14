package com.artemkhateev.carlog.feature.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.data.makes.modelsOf
import com.artemkhateev.carlog.ui.components.ConfirmDialog
import com.artemkhateev.carlog.ui.components.CurrencySheet
import com.artemkhateev.carlog.ui.components.EditorScaffold
import com.artemkhateev.carlog.ui.components.FormPickerField
import com.artemkhateev.carlog.ui.components.FormRow
import com.artemkhateev.carlog.ui.components.FormSwitchRow
import com.artemkhateev.carlog.ui.components.FormTextField
import com.artemkhateev.carlog.ui.components.MakeBadge
import com.artemkhateev.carlog.ui.components.OptionSheet
import com.artemkhateev.carlog.ui.components.SearchableSheet
import com.artemkhateev.carlog.ui.format.sanitizeDecimalInput
import com.artemkhateev.carlog.ui.format.sanitizeWholeInput
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.navigation.VehicleEditorRoute
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import com.artemkhateev.carlog.ui.theme.VehicleColors

@Composable
fun VehicleEditorScreen(route: VehicleEditorRoute, navigator: AppNavigator) {
    val viewModel: VehicleEditorViewModel = viewModel {
        VehicleEditorViewModel(route.id, AppGraph.repository, AppGraph.settings, AppGraph.currentVehicle, AppGraph.carMakes)
    }
    val makes by viewModel.makes.collectAsStateWithLifecycle()
    val draft = viewModel.draft
    val fuels by viewModel.fuels.collectAsStateWithLifecycle()
    val currencyCode by viewModel.currencyCode.collectAsStateWithLifecycle()
    val showErrors = viewModel.showErrors
    val done by viewModel.done.collectAsStateWithLifecycle()
    LaunchedEffect(done) {
        if (done) {
            if (route.first) navigator.openMainClearingBackStack() else navigator.back()
        }
    }
    if (draft == null) return

    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var fuelSheetOpen by rememberSaveable { mutableStateOf(false) }
    var currencySheetOpen by rememberSaveable { mutableStateOf(false) }
    var makeSheetOpen by rememberSaveable { mutableStateOf(false) }
    var modelSheetOpen by rememberSaveable { mutableStateOf(false) }
    val colors = CarLogTheme.colors
    val formats = CarLogTheme.formats
    val accent = colors.brand
    val isNew = route.id == 0L

    EditorScaffold(
        title = stringResource(
            when {
                route.first -> R.string.vehicle_first_title
                isNew -> R.string.vehicle_new
                else -> R.string.vehicle_edit
            },
        ),
        accent = accent,
        onBack = if (route.first) null else navigator::back,
        onSave = viewModel::save,
        onDelete = if (isNew) null else ({ confirmDelete = true }),
    ) {
        if (route.first) {
            Text(
                text = stringResource(R.string.vehicle_first_subtitle),
                style = CarLogTheme.typography.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 64.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            )
        }
        FormRow(Icons.Filled.DirectionsCar) {
            FormTextField(
                value = draft.name,
                onValueChange = { value -> viewModel.update { it.copy(name = value) } },
                label = stringResource(R.string.vehicle_name),
                capitalization = KeyboardCapitalization.Words,
                error = if (showErrors && draft.displayName.isEmpty()) stringResource(R.string.vehicle_name_required) else null,
            )
        }
        FormRow(null, leading = draft.make.takeIf { it.isNotBlank() }?.let { make -> @Composable { MakeBadge(make) } }) {
            FormPickerField(
                label = stringResource(R.string.vehicle_make),
                value = draft.make,
                onClick = { makeSheetOpen = true },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(16.dp))
            FormPickerField(
                label = stringResource(R.string.vehicle_model),
                value = draft.model,
                onClick = { modelSheetOpen = true },
                modifier = Modifier.weight(1f),
            )
        }
        FormRow(null) {
            FormTextField(
                value = draft.yearText,
                onValueChange = { value -> viewModel.update { it.copy(yearText = sanitizeWholeInput(value, maxDigits = 4)) } },
                label = stringResource(R.string.vehicle_year),
                keyboardType = KeyboardType.Number,
                error = if (showErrors && !draft.yearValid) stringResource(R.string.invalid_value) else null,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(16.dp))
            FormTextField(
                value = draft.plate,
                onValueChange = { value -> viewModel.update { it.copy(plate = value) } },
                label = stringResource(R.string.vehicle_plate),
                capitalization = KeyboardCapitalization.Characters,
                modifier = Modifier.weight(1f),
            )
        }
        FormRow(Icons.Filled.LocalGasStation) {
            FormPickerField(
                label = stringResource(R.string.vehicle_fuel),
                value = fuels.firstOrNull { it.id == draft.fuelId }?.name.orEmpty(),
                onClick = { fuelSheetOpen = true },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(16.dp))
            FormTextField(
                value = draft.tankText,
                onValueChange = { value -> viewModel.update { it.copy(tankText = sanitizeDecimalInput(value, maxDecimals = 3)) } },
                label = stringResource(R.string.vehicle_tank),
                keyboardType = KeyboardType.Decimal,
                suffix = formats.volumeLabel,
                error = if (showErrors && !draft.tankValid) stringResource(R.string.invalid_value) else null,
                modifier = Modifier.weight(1f),
            )
        }
        if (isNew) {
            FormRow(Icons.Filled.Speed) {
                FormTextField(
                    value = draft.odometerText,
                    onValueChange = { value -> viewModel.update { it.copy(odometerText = sanitizeWholeInput(value)) } },
                    label = stringResource(R.string.vehicle_current_odometer),
                    keyboardType = KeyboardType.Number,
                    suffix = formats.distanceLabel,
                )
            }
        }
        if (route.first) {
            FormRow(Icons.Filled.Payments) {
                FormPickerField(
                    label = stringResource(R.string.vehicle_currency),
                    value = currencyCode.orEmpty(),
                    onClick = { currencySheetOpen = true },
                )
            }
        }
        FormRow(Icons.Filled.Palette) {
            ColorChoice(selected = draft.colorIndex, onSelect = { index -> viewModel.update { it.copy(colorIndex = index) } })
        }
        if (!isNew) {
            FormSwitchRow(
                icon = Icons.Filled.CheckCircle,
                text = stringResource(R.string.vehicle_active),
                checked = draft.active,
                onCheckedChange = { active -> viewModel.update { it.copy(active = active) } },
                accent = accent,
            )
        }
        FormRow(Icons.AutoMirrored.Filled.Notes) {
            FormTextField(
                value = draft.notes,
                onValueChange = { value -> viewModel.update { it.copy(notes = value) } },
                label = stringResource(R.string.field_notes),
                singleLine = false,
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            message = stringResource(R.string.vehicle_delete_message, draft.displayName),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = viewModel::delete,
            onDismiss = { confirmDelete = false },
        )
    }
    if (fuelSheetOpen) {
        OptionSheet(
            title = stringResource(R.string.vehicle_fuel),
            options = fuels,
            optionLabel = { it.name },
            isSelected = { it.id == draft.fuelId },
            onSelect = { fuel -> viewModel.update { it.copy(fuelId = fuel.id) } },
            onDismiss = { fuelSheetOpen = false },
            onNone = { viewModel.update { it.copy(fuelId = null) } },
            noneSelected = draft.fuelId == null,
        )
    }
    if (makeSheetOpen) {
        val names = remember(makes) { makes.map { it.name } }
        SearchableSheet(
            title = stringResource(R.string.vehicle_make),
            options = names,
            selected = draft.make,
            onSelect = { make -> viewModel.update { it.withMake(make) } },
            onDismiss = { makeSheetOpen = false },
            leading = { name -> MakeBadge(name) },
        )
    }
    if (modelSheetOpen) {
        SearchableSheet(
            title = stringResource(R.string.vehicle_model),
            options = remember(makes, draft.make) { makes.modelsOf(draft.make) },
            selected = draft.model,
            onSelect = { model -> viewModel.update { it.copy(model = model) } },
            onDismiss = { modelSheetOpen = false },
            emptyHint = stringResource(R.string.vehicle_models_empty),
        )
    }
    if (currencySheetOpen) {
        CurrencySheet(
            selectedCode = currencyCode,
            onSelect = viewModel::setCurrency,
            onDismiss = { currencySheetOpen = false },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ColorChoice(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .weight(1f)
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VehicleColors.forEachIndexed { index, color ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(if (isSelected) 3.dp else 0.dp, if (isSelected) CarLogTheme.colors.textPrimary else Color.Transparent, CircleShape)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}
