package com.artemkhateev.carlog.feature.catalogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.data.CarLogRepository
import com.artemkhateev.carlog.data.model.Fuel
import com.artemkhateev.carlog.data.model.FuelCategory
import com.artemkhateev.carlog.ui.components.ConfirmDialog
import com.artemkhateev.carlog.ui.components.EditNameDialog
import com.artemkhateev.carlog.ui.components.ListRow
import com.artemkhateev.carlog.ui.components.ListScreenScaffold
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@get:StringRes
val FuelCategory.labelRes: Int
    get() = when (this) {
        FuelCategory.Gasoline -> R.string.fuel_category_gasoline
        FuelCategory.Diesel -> R.string.fuel_category_diesel
        FuelCategory.Ethanol -> R.string.fuel_category_ethanol
        FuelCategory.Lpg -> R.string.fuel_category_lpg
        FuelCategory.Cng -> R.string.fuel_category_cng
        FuelCategory.Electricity -> R.string.fuel_category_electricity
        FuelCategory.Other -> R.string.fuel_category_other
    }

class FuelsViewModel(private val repository: CarLogRepository) : ViewModel() {

    val fuels: StateFlow<List<Fuel>?> = repository.catalogs.map { it.fuels }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val mutableInUse = MutableStateFlow(false)
    val inUse: StateFlow<Boolean> = mutableInUse.asStateFlow()

    fun save(fuel: Fuel) {
        viewModelScope.launch { repository.saveFuel(fuel) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            if (!repository.deleteFuel(id)) mutableInUse.value = true
        }
    }

    fun inUseShown() {
        mutableInUse.value = false
    }
}

@Composable
fun FuelsScreen(navigator: AppNavigator) {
    val viewModel: FuelsViewModel = viewModel { FuelsViewModel(AppGraph.repository) }
    val fuels = viewModel.fuels.collectAsStateWithLifecycle().value
    val inUse by viewModel.inUse.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val inUseMessage = stringResource(R.string.catalog_in_use)
    LaunchedEffect(inUse) {
        if (inUse) {
            snackbar.showSnackbar(inUseMessage)
            viewModel.inUseShown()
        }
    }

    ListScreenScaffold(
        title = stringResource(R.string.more_fuels),
        onBack = navigator::back,
        onAdd = { adding = true },
        snackbar = snackbar,
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            items(fuels.orEmpty(), key = { it.id }) { fuel ->
                ListRow(
                    title = fuel.name,
                    subtitle = stringResource(fuel.category.labelRes),
                    onClick = { editingId = fuel.id },
                    leading = { Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = CarLogTheme.colors.icon) },
                )
            }
        }
    }

    if (adding) {
        FuelDialog(
            initial = null,
            onSave = { name, category ->
                adding = false
                viewModel.save(Fuel(name = name, category = category))
            },
            onDelete = null,
            onDismiss = { adding = false },
        )
    }
    val editing = fuels?.firstOrNull { it.id == editingId }
    if (editing != null) {
        FuelDialog(
            initial = editing,
            onSave = { name, category ->
                editingId = null
                viewModel.save(editing.copy(name = name, category = category))
            },
            onDelete = {
                editingId = null
                deletingId = editing.id
            },
            onDismiss = { editingId = null },
        )
    }
    val deleting = fuels?.firstOrNull { it.id == deletingId }
    if (deleting != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            message = deleting.name,
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.delete(deleting.id) },
            onDismiss = { deletingId = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FuelDialog(initial: Fuel?, onSave: (String, FuelCategory) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var category by rememberSaveable { mutableStateOf(initial?.category ?: FuelCategory.Gasoline) }
    val categories: @Composable () -> Unit = {
        Text(
            text = stringResource(R.string.fuel_category),
            style = CarLogTheme.typography.caption,
            color = CarLogTheme.colors.textSecondary,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FuelCategory.entries.forEach { option ->
                FilterChip(
                    selected = option == category,
                    onClick = { category = option },
                    label = { Text(stringResource(option.labelRes)) },
                )
            }
        }
    }
    EditNameDialog(
        title = stringResource(if (initial == null) R.string.add_new else R.string.catalog_rename),
        initial = initial?.name.orEmpty(),
        onSave = { name -> onSave(name, category) },
        onDelete = onDelete,
        onDismiss = onDismiss,
        extraContent = categories,
    )
}
