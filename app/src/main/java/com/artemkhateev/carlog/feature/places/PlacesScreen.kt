package com.artemkhateev.carlog.feature.places

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.data.CarLogRepository
import com.artemkhateev.carlog.data.model.Place
import com.artemkhateev.carlog.data.model.PlaceKind
import com.artemkhateev.carlog.ui.components.ConfirmDialog
import com.artemkhateev.carlog.ui.components.EmptyState
import com.artemkhateev.carlog.ui.components.ListRow
import com.artemkhateev.carlog.ui.components.ListScreenScaffold
import com.artemkhateev.carlog.ui.components.TypeBadge
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import com.artemkhateev.carlog.ui.theme.EntryColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlacesViewModel(private val repository: CarLogRepository) : ViewModel() {

    val places: StateFlow<List<Place>?> = repository.catalogs.map { it.places }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(place: Place) {
        viewModelScope.launch { repository.savePlace(place) }
    }

    /** Место необязательное: из записей оно просто пропадёт. */
    fun delete(id: Long) {
        viewModelScope.launch { repository.deletePlace(id) }
    }
}

@Composable
fun PlacesScreen(navigator: AppNavigator) {
    val viewModel: PlacesViewModel = viewModel { PlacesViewModel(AppGraph.repository) }
    val places = viewModel.places.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }

    ListScreenScaffold(title = stringResource(R.string.more_places), onBack = navigator::back, onAdd = { adding = true }) {
        when {
            places == null -> Unit
            places.isEmpty() -> EmptyState(Icons.Filled.Place, stringResource(R.string.catalog_empty))
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(places, key = { it.id }) { place ->
                    val gasStation = place.kind == PlaceKind.GasStation
                    ListRow(
                        title = place.name,
                        subtitle = place.address,
                        onClick = { editingId = place.id },
                        leading = {
                            TypeBadge(
                                icon = if (gasStation) Icons.Filled.LocalGasStation else Icons.Filled.Place,
                                color = if (gasStation) EntryColors.Refueling else CarLogTheme.colors.brand,
                                size = 40.dp,
                                iconSize = 22.dp,
                            )
                        },
                        trailing = {
                            IconButton(onClick = { openInMaps(context, place) }) {
                                Icon(Icons.Filled.Map, contentDescription = stringResource(R.string.place_open_map), tint = CarLogTheme.colors.icon)
                            }
                        },
                    )
                }
            }
        }
    }

    if (adding) {
        PlaceDialog(
            initial = null,
            onSave = { place ->
                adding = false
                viewModel.save(place)
            },
            onDelete = null,
            onDismiss = { adding = false },
        )
    }
    val editing = places?.firstOrNull { it.id == editingId }
    if (editing != null) {
        PlaceDialog(
            initial = editing,
            onSave = { place ->
                editingId = null
                viewModel.save(place)
            },
            onDelete = {
                editingId = null
                deletingId = editing.id
            },
            onDismiss = { editingId = null },
        )
    }
    val deleting = places?.firstOrNull { it.id == deletingId }
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

@Composable
private fun PlaceDialog(initial: Place?, onSave: (Place) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var address by rememberSaveable { mutableStateOf(initial?.address.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(initial?.kind ?: PlaceKind.GasStation) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.place_new else R.string.catalog_rename)) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    FilterChip(
                        selected = kind == PlaceKind.GasStation,
                        onClick = { kind = PlaceKind.GasStation },
                        label = { Text(stringResource(R.string.place_kind_gas_station)) },
                    )
                    FilterChip(
                        selected = kind == PlaceKind.Other,
                        onClick = { kind = PlaceKind.Other },
                        label = { Text(stringResource(R.string.place_kind_other)) },
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.place_address)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(Place(id = initial?.id ?: 0, name = name.trim(), kind = kind, address = address.trim())) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete), color = CarLogTheme.colors.danger) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

/** Карты ищут по адресу, а без адреса — по названию. */
private fun openInMaps(context: Context, place: Place) {
    val query = place.address.ifBlank { place.name }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))))
    } catch (_: ActivityNotFoundException) {
        // Карт на телефоне нет — показать место негде.
    }
}
