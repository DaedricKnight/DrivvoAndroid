package com.artemkhateev.carlog.feature.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.ui.components.AddFab
import com.artemkhateev.carlog.ui.components.ListRow
import com.artemkhateev.carlog.ui.components.TitleTopBar
import com.artemkhateev.carlog.ui.components.VehicleAvatar
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.theme.CarLogTheme

@Composable
fun VehiclesScreen(navigator: AppNavigator) {
    val selection = AppGraph.currentVehicle.selection.collectAsStateWithLifecycle().value
    val colors = CarLogTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            TitleTopBar(stringResource(R.string.vehicles_title), colors.brand, onBack = navigator::back)
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(selection?.vehicles.orEmpty(), key = { it.id }) { vehicle ->
                    ListRow(
                        title = vehicle.name,
                        subtitle = listOfNotNull(
                            vehicle.description.ifBlank { null },
                            vehicle.year?.toString(),
                            vehicle.plate.ifBlank { null },
                        ).joinToString(" · "),
                        onClick = { navigator.openVehicle(vehicle.id) },
                        dimmed = !vehicle.active,
                        leading = { VehicleAvatar(vehicle, size = 40.dp) },
                        trailing = {
                            if (!vehicle.active) {
                                Text(stringResource(R.string.vehicle_inactive), style = CarLogTheme.typography.caption, color = colors.textSecondary)
                            }
                        },
                    )
                }
            }
        }
        AddFab(onClick = { navigator.openVehicle() }, modifier = Modifier.align(Alignment.BottomEnd))
    }
}
