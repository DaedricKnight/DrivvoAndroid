package com.artemkhateev.carlog.ui.navigation

import androidx.navigation.NavController
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.data.model.EntryType
import kotlinx.serialization.Serializable

@Serializable
data object MainRoute

/** [first] — первая машина при первом запуске: без кнопки «назад», с выбором валюты. */
@Serializable
data class VehicleEditorRoute(val id: Long = 0, val first: Boolean = false)

@Serializable
data object VehiclesRoute

/** [id] 0 — новая запись. */
@Serializable
data class EntryEditorRoute(val type: EntryType, val id: Long = 0)

@Serializable
data class ReminderEditorRoute(val id: Long = 0)

@Serializable
data class CatalogRoute(val kind: CatalogKind)

@Serializable
data object FuelsRoute

@Serializable
data object PlacesRoute

@Serializable
data object SettingsRoute

@Serializable
data object FlexCalculatorRoute

@Serializable
data object SearchRoute

/** Переходы, которые просят экраны: сами экраны о графе навигации не знают. */
class AppNavigator(private val navController: NavController) {
    fun back() {
        navController.popBackStack()
    }

    fun openEntry(type: EntryType, id: Long = 0) = navController.navigate(EntryEditorRoute(type, id))

    fun openReminder(id: Long = 0) = navController.navigate(ReminderEditorRoute(id))

    fun openVehicles() = navController.navigate(VehiclesRoute)

    fun openVehicle(id: Long = 0) = navController.navigate(VehicleEditorRoute(id))

    fun openCatalog(kind: CatalogKind) = navController.navigate(CatalogRoute(kind))

    fun openFuels() = navController.navigate(FuelsRoute)

    fun openPlaces() = navController.navigate(PlacesRoute)

    fun openSettings() = navController.navigate(SettingsRoute)

    fun openFlexCalculator() = navController.navigate(FlexCalculatorRoute)

    fun openSearch() = navController.navigate(SearchRoute)

    /** После первой машины первый экран больше не нужен: назад из главного — выход. */
    fun openMainClearingBackStack() = navController.navigate(MainRoute) {
        popUpTo(navController.graph.id) { inclusive = true }
    }
}
