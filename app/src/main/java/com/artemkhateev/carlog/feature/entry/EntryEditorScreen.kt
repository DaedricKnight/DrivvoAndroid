package com.artemkhateev.carlog.feature.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.ui.components.ConfirmDialog
import com.artemkhateev.carlog.ui.components.EditorScaffold
import com.artemkhateev.carlog.ui.components.accent
import com.artemkhateev.carlog.ui.components.titleRes
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.navigation.EntryEditorRoute

@Composable
fun EntryEditorScreen(route: EntryEditorRoute, navigator: AppNavigator) {
    val viewModel: EntryEditorViewModel = viewModel {
        EntryEditorViewModel(route.type, route.id, AppGraph.repository, AppGraph.currentVehicle)
    }
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val done by viewModel.done.collectAsStateWithLifecycle()
    LaunchedEffect(done) {
        if (done) navigator.back()
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    EditorScaffold(
        title = stringResource(route.type.titleRes),
        accent = route.type.accent,
        onBack = navigator::back,
        onSave = viewModel::save,
        onDelete = if (route.id == 0L) null else ({ confirmDelete = true }),
    ) {
        if (state != null) {
            when (val draft = state.draft) {
                is RefuelingDraft -> RefuelingForm(draft, state, viewModel)
                is ItemizedDraft -> ItemizedForm(draft, state, viewModel)
                is IncomeDraft -> IncomeForm(draft, state, viewModel)
                is RouteDraft -> RouteForm(draft, state, viewModel)
                is ReadingDraft -> ReadingForm(draft, state, viewModel)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            message = stringResource(R.string.delete_entry_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = viewModel::delete,
            onDismiss = { confirmDelete = false },
        )
    }
}
