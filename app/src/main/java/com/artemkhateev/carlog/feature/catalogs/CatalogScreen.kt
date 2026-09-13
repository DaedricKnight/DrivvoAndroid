package com.artemkhateev.carlog.feature.catalogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.data.CarLogRepository
import com.artemkhateev.carlog.data.model.CatalogItem
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.ui.components.ConfirmDialog
import com.artemkhateev.carlog.ui.components.EditNameDialog
import com.artemkhateev.carlog.ui.components.EmptyState
import com.artemkhateev.carlog.ui.components.ListRow
import com.artemkhateev.carlog.ui.components.ListScreenScaffold
import com.artemkhateev.carlog.ui.components.NameDialog
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.navigation.CatalogRoute
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@get:StringRes
val CatalogKind.titleRes: Int
    get() = when (this) {
        CatalogKind.ServiceType -> R.string.more_service_types
        CatalogKind.ExpenseType -> R.string.more_expense_types
        CatalogKind.IncomeType -> R.string.more_income_types
        CatalogKind.Reason -> R.string.more_reasons
        CatalogKind.PaymentMethod -> R.string.more_payment_methods
        CatalogKind.Driver -> R.string.more_drivers
    }

val CatalogKind.icon: ImageVector
    get() = when (this) {
        CatalogKind.ServiceType -> Icons.Filled.Build
        CatalogKind.ExpenseType -> Icons.Filled.CreditCard
        CatalogKind.IncomeType -> Icons.Filled.AddCard
        CatalogKind.Reason -> Icons.Filled.Work
        CatalogKind.PaymentMethod -> Icons.Filled.AttachMoney
        CatalogKind.Driver -> Icons.Filled.Badge
    }

class CatalogViewModel(private val kind: CatalogKind, private val repository: CarLogRepository) : ViewModel() {

    val items: StateFlow<List<CatalogItem>?> = repository.catalogs.map { it.itemsOf(kind) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val mutableInUse = MutableStateFlow(false)

    /** Удалить не вышло: элемент есть в записях. Экран показывает сообщение и сбрасывает флаг. */
    val inUse: StateFlow<Boolean> = mutableInUse.asStateFlow()

    fun save(id: Long, name: String) {
        viewModelScope.launch { repository.saveCatalogItem(CatalogItem(id = id, kind = kind, name = name)) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            if (!repository.deleteCatalogItem(id)) mutableInUse.value = true
        }
    }

    fun inUseShown() {
        mutableInUse.value = false
    }
}

@Composable
fun CatalogScreen(route: CatalogRoute, navigator: AppNavigator) {
    val kind = route.kind
    val viewModel: CatalogViewModel = viewModel { CatalogViewModel(kind, AppGraph.repository) }
    val items = viewModel.items.collectAsStateWithLifecycle().value
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
        title = stringResource(kind.titleRes),
        onBack = navigator::back,
        onAdd = { adding = true },
        snackbar = snackbar,
    ) {
        when {
            items == null -> Unit
            items.isEmpty() -> EmptyState(kind.icon, stringResource(R.string.catalog_empty))
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(items, key = { it.id }) { item ->
                    ListRow(
                        title = item.name,
                        onClick = { editingId = item.id },
                        leading = { Icon(kind.icon, contentDescription = null, tint = CarLogTheme.colors.icon) },
                    )
                }
            }
        }
    }

    if (adding) {
        NameDialog(
            title = stringResource(R.string.add_new),
            initial = "",
            onConfirm = { name ->
                adding = false
                viewModel.save(0, name)
            },
            onDismiss = { adding = false },
        )
    }
    val editing = items?.firstOrNull { it.id == editingId }
    if (editing != null) {
        EditNameDialog(
            title = stringResource(R.string.catalog_rename),
            initial = editing.name,
            onSave = { name ->
                editingId = null
                viewModel.save(editing.id, name)
            },
            onDelete = {
                editingId = null
                deletingId = editing.id
            },
            onDismiss = { editingId = null },
        )
    }
    val deleting = items?.firstOrNull { it.id == deletingId }
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
