package com.artemkhateev.carlog.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.data.CurrentVehicle
import com.artemkhateev.carlog.data.settings.AppSettings
import com.artemkhateev.carlog.data.settings.SettingsRepository
import com.artemkhateev.carlog.data.settings.ThemeMode
import com.artemkhateev.carlog.feature.entry.EntryEditorScreen
import com.artemkhateev.carlog.feature.main.MainScreen
import com.artemkhateev.carlog.feature.vehicles.VehicleEditorScreen
import com.artemkhateev.carlog.feature.vehicles.VehiclesScreen
import com.artemkhateev.carlog.ui.format.Formats
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.navigation.EntryEditorRoute
import com.artemkhateev.carlog.ui.navigation.MainRoute
import com.artemkhateev.carlog.ui.navigation.VehicleEditorRoute
import com.artemkhateev.carlog.ui.navigation.VehiclesRoute
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

data class AppUiState(val settings: AppSettings, val hasVehicles: Boolean)

class AppViewModel(currentVehicle: CurrentVehicle, settings: SettingsRepository) : ViewModel() {
    /** null — настройки и список машин ещё не прочитаны: до этого показываем только фон окна. */
    val state: StateFlow<AppUiState?> =
        combine(settings.settings, currentVehicle.selection.filterNotNull()) { current, selection ->
            AppUiState(current, selection.vehicles.isNotEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun CarLogApp() {
    val viewModel: AppViewModel = viewModel { AppViewModel(AppGraph.currentVehicle, AppGraph.settings) }
    val state = viewModel.state.collectAsStateWithLifecycle().value ?: return
    val dark = when (state.settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
    val formats = remember(state.settings) { Formats.from(state.settings) }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }
    CarLogTheme(dark = dark, formats = formats) {
        val navController = rememberNavController()
        val navigator = remember(navController) { AppNavigator(navController) }
        // Первый экран выбирается один раз: дальше переходами управляет навигация.
        val startWithVehicles = rememberSaveable { state.hasVehicles }
        NavHost(
            navController = navController,
            startDestination = if (startWithVehicles) MainRoute else VehicleEditorRoute(first = true),
            modifier = Modifier
                .fillMaxSize()
                .background(CarLogTheme.colors.background),
        ) {
            composable<MainRoute> { MainScreen(navigator) }
            composable<VehicleEditorRoute> { VehicleEditorScreen(it.toRoute(), navigator) }
            composable<VehiclesRoute> { VehiclesScreen(navigator) }
            composable<EntryEditorRoute> { EntryEditorScreen(it.toRoute(), navigator) }
        }
    }
}
