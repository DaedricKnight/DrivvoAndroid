package com.artemkhateev.carlog.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.AppGraph
import com.artemkhateev.carlog.data.CurrentVehicle
import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.Entry
import com.artemkhateev.carlog.data.model.Income
import com.artemkhateev.carlog.data.model.Reading
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Route
import com.artemkhateev.carlog.domain.consumptionByRefuelingId
import com.artemkhateev.carlog.domain.fuelSegments
import com.artemkhateev.carlog.domain.searchEntries
import com.artemkhateev.carlog.ui.components.ColoredTopBar
import com.artemkhateev.carlog.ui.components.EmptyState
import com.artemkhateev.carlog.ui.components.TypeBadge
import com.artemkhateev.carlog.ui.components.accent
import com.artemkhateev.carlog.ui.components.entrySubtitle
import com.artemkhateev.carlog.ui.components.entryTitle
import com.artemkhateev.carlog.ui.components.icon
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import com.artemkhateev.carlog.ui.theme.EntryColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val query: String,
    val results: List<Entry>,
    val catalogs: Catalogs,
    val consumption: Map<Long, Double>,
)

class SearchViewModel(currentVehicle: CurrentVehicle) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<SearchUiState?> =
        combine(currentVehicle.data.filterNotNull(), query) { data, current ->
            SearchUiState(
                query = current,
                results = searchEntries(data.entries, data.catalogs, current),
                catalogs = data.catalogs,
                consumption = consumptionByRefuelingId(fuelSegments(data.entries.filterIsInstance<Refueling>())),
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(value: String) {
        query.value = value
    }
}

@Composable
fun SearchScreen(navigator: AppNavigator) {
    val viewModel: SearchViewModel = viewModel { SearchViewModel(AppGraph.currentVehicle) }
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    // Текст поля живёт на экране: поиск считается в фоне и не должен дёргать курсор.
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        viewModel.setQuery(text)
        focus.requestFocus()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding(),
    ) {
        ColoredTopBar(colors.brand) {
            IconButton(onClick = navigator::back) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
            }
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    viewModel.setQuery(it)
                },
                singleLine = true,
                textStyle = typography.body.copy(color = Color.White),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) Text(stringResource(R.string.search_hint), style = typography.body, color = Color.White.copy(alpha = 0.7f))
                        inner()
                    }
                },
            )
            if (text.isNotEmpty()) {
                IconButton(onClick = {
                    text = ""
                    viewModel.setQuery("")
                }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear), tint = Color.White)
                }
            }
        }
        when {
            state == null || text.isBlank() -> Unit
            state.results.isEmpty() -> EmptyState(Icons.Filled.Search, stringResource(R.string.search_empty))
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(state.results, key = { "${it.type}-${it.id}" }) { entry ->
                    SearchResultRow(entry, state) { navigator.openEntry(entry.type, entry.id) }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(entry: Entry, state: SearchUiState, onClick: () -> Unit) {
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    val formats = CarLogTheme.formats
    val amount = when (entry) {
        is Income -> "+" + formats.money(entry.amount)
        is Route -> entry.value.takeIf { it.minor != 0L }?.let { formats.money(it) }
        is Reading -> null
        else -> formats.money(entry.cost)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) {
            TypeBadge(entry.type.icon, entry.type.accent, size = 44.dp)
        }
        Column(Modifier.weight(1f)) {
            Row(Modifier.padding(top = 14.dp, bottom = 14.dp, end = 16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(entryTitle(entry, state.catalogs), style = typography.listTitle, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = entrySubtitle(entry, if (entry is Refueling) state.consumption[entry.id] else null, state.catalogs),
                        style = typography.listSubtitle,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                    if (amount != null) {
                        Text(amount, style = typography.listTitle, color = if (entry is Income) EntryColors.Income else colors.textPrimary)
                    }
                    Text(formats.fullDate(entry.dateTime.toLocalDate()), style = typography.listSubtitle, color = colors.textSecondary)
                }
            }
            HorizontalDivider(Modifier.padding(end = 16.dp), color = colors.divider)
        }
    }
}
