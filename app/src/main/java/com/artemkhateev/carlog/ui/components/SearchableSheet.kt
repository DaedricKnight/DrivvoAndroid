package com.artemkhateev.carlog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.makes.filterByName
import com.artemkhateev.carlog.data.makes.searchKey
import com.artemkhateev.carlog.ui.theme.CarLogTheme

/**
 * Выбор из длинного списка с поиском — марки и модели. Если нужного нет, введённый текст берётся как есть:
 * справочник не должен мешать записать редкую машину.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableSheet(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    emptyHint: String? = null,
    /** Значок перед вариантом: логотип марки. */
    leading: (@Composable (String) -> Unit)? = null,
) {
    val colors = CarLogTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(options, query) { filterByName(options, query) { it } }
    val typed = query.trim()
    val exact = remember(filtered, typed) { filtered.firstOrNull { searchKey(it) == searchKey(typed) } }

    fun choose(value: String) {
        onSelect(value)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card,
    ) {
        SheetTitle(title)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.action_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (typed.isNotEmpty()) choose(exact ?: typed) }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyColumn(Modifier.navigationBarsPadding()) {
            if (typed.isNotEmpty() && exact == null) {
                item { UseTypedRow(typed) { choose(typed) } }
            }
            if (options.isEmpty() && typed.isEmpty() && emptyHint != null) {
                item {
                    Text(
                        text = emptyHint,
                        style = CarLogTheme.typography.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    )
                }
            }
            items(filtered) { option ->
                OptionRow(option, option == selected, leading = leading?.let { draw -> @Composable { draw(option) } }) { choose(option) }
            }
        }
    }
}

@Composable
private fun UseTypedRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Edit, contentDescription = null, tint = CarLogTheme.colors.brandText, modifier = Modifier.size(22.dp))
        Text(
            text = stringResource(R.string.use_typed, text),
            style = CarLogTheme.typography.body,
            color = CarLogTheme.colors.brandText,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
