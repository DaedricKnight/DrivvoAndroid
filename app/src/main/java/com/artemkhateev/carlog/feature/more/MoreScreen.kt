package com.artemkhateev.carlog.feature.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.makes.commonsPage
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.ui.components.LocalMakeLogos
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.theme.CarLogTheme

/** «Ещё»: машины и люди, справочники, калькулятор, настройки — группами, как у референса. */
@Composable
fun MoreScreen(navigator: AppNavigator) {
    var aboutOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        MoreItem(Icons.Filled.DirectionsCar, R.string.more_vehicles) { navigator.openVehicles() }
        MoreItem(Icons.Filled.Badge, R.string.more_drivers) { navigator.openCatalog(CatalogKind.Driver) }
        SectionDivider()
        MoreItem(Icons.Filled.LocalGasStation, R.string.more_fuels) { navigator.openFuels() }
        MoreItem(Icons.Filled.Place, R.string.more_places) { navigator.openPlaces() }
        MoreItem(Icons.Filled.Build, R.string.more_service_types) { navigator.openCatalog(CatalogKind.ServiceType) }
        MoreItem(Icons.Filled.CreditCard, R.string.more_expense_types) { navigator.openCatalog(CatalogKind.ExpenseType) }
        MoreItem(Icons.Filled.AddCard, R.string.more_income_types) { navigator.openCatalog(CatalogKind.IncomeType) }
        MoreItem(Icons.Filled.Work, R.string.more_reasons) { navigator.openCatalog(CatalogKind.Reason) }
        MoreItem(Icons.Filled.AttachMoney, R.string.more_payment_methods) { navigator.openCatalog(CatalogKind.PaymentMethod) }
        SectionDivider()
        MoreItem(Icons.Filled.Calculate, R.string.more_flex) { navigator.openFlexCalculator() }
        SectionDivider()
        MoreItem(Icons.Filled.Settings, R.string.more_settings) { navigator.openSettings() }
        MoreItem(Icons.Filled.Info, R.string.more_about) { aboutOpen = true }
    }
    if (aboutOpen) {
        val context = LocalContext.current
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
        val logos = LocalMakeLogos.current
        val credits = remember(logos) { logos.values.filter { it.license != null }.sortedBy { it.make } }
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { aboutOpen = false },
            title = { Text(stringResource(R.string.app_name)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.about_text, version))
                    Text(stringResource(R.string.about_logos), Modifier.padding(top = 12.dp, bottom = 4.dp))
                    // CC BY и CC BY-SA требуют указать автора; строка открывает страницу файла с лицензией.
                    credits.forEach { logo ->
                        Text(
                            text = stringResource(R.string.about_logo_credit, logo.make, logo.author.orEmpty(), logo.license.orEmpty()),
                            style = CarLogTheme.typography.caption,
                            color = CarLogTheme.colors.brand,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { logo.commonsPage?.let { page -> runCatching { uriHandler.openUri(page) } } }
                                .padding(vertical = 6.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { aboutOpen = false }) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}

@Composable
private fun MoreItem(icon: ImageVector, title: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = CarLogTheme.colors.icon)
        }
        Text(stringResource(title), style = CarLogTheme.typography.listTitle, color = CarLogTheme.colors.textPrimary)
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = CarLogTheme.colors.divider)
}
