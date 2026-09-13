package com.artemkhateev.carlog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.ui.theme.AccentScope
import com.artemkhateev.carlog.ui.theme.CarLogTheme

/**
 * Экран формы записи: шапка цветом типа, поля в столбик, «Сохранить» в конце.
 * [onDelete] есть только у существующей записи.
 */
@Composable
fun EditorScaffold(
    title: String,
    accent: Color,
    onBack: (() -> Unit)?,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    saveLabel: String = stringResource(R.string.action_save),
    content: @Composable ColumnScope.() -> Unit,
) {
    AccentScope(accent) {
        Column(
            modifier
                .background(CarLogTheme.colors.background)
                .imePadding(),
        ) {
            TitleTopBar(title = title, color = accent, onBack = onBack) {
                if (onDelete != null) {
                    TopBarAction(Icons.Filled.Delete, stringResource(R.string.action_delete), onDelete)
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(top = 8.dp, bottom = 24.dp),
            ) {
                content()
                SaveButton(saveLabel, accent, onSave, Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
            }
        }
    }
}

/** Строка формы: значок слева в своей колонке, поле справа. */
@Composable
fun FormRow(icon: ImageVector?, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 16.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, contentDescription = null, tint = CarLogTheme.colors.icon)
        }
        content()
    }
}

@Composable
private fun underlineColors() = CarLogTheme.colors.let { colors ->
    TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        unfocusedIndicatorColor = colors.outline,
        disabledIndicatorColor = colors.outline,
        disabledTextColor = colors.textPrimary,
        disabledLabelColor = colors.textSecondary,
        unfocusedLabelColor = colors.textSecondary,
        disabledSupportingTextColor = colors.textSecondary,
        unfocusedSupportingTextColor = colors.textSecondary,
        focusedSupportingTextColor = colors.textSecondary,
        disabledTrailingIconColor = colors.icon,
        disabledSuffixColor = colors.textSecondary,
    )
}

/** Поле с подчёркиванием, как у референса. [supporting] — подсказка справа под полем, [error] — вместо неё. */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    error: String? = null,
    supporting: String? = null,
    suffix: String? = null,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
) {
    val below = error ?: supporting
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1) },
        singleLine = singleLine,
        isError = error != null,
        suffix = suffix?.let { { Text(it) } },
        supportingText = below?.let {
            { Text(it, textAlign = if (error == null) TextAlign.End else TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
        },
        keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType, imeAction = imeAction),
        colors = underlineColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Поле выбора: выглядит как поле ввода, по нажатию открывает список или календарь. */
@Composable
fun FormPickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    trailingIcon: ImageVector? = null,
) {
    Box(modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = {},
            label = { Text(label, maxLines = 1) },
            singleLine = true,
            enabled = false,
            isError = error != null,
            supportingText = error?.let { { Text(it, color = CarLogTheme.colors.danger) } },
            trailingIcon = trailingIcon?.let { { Icon(it, contentDescription = null) } },
            colors = underlineColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        // Отключённое поле нажатий не получает: ловим их поверх.
        Box(
            Modifier
                .matchParentSize()
                .clickable(onClick = onClick),
        )
    }
}

@Composable
fun FormSwitchRow(icon: ImageVector?, text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, accent: Color) {
    FormRow(icon, Modifier.clickable { onCheckedChange(!checked) }) {
        Text(
            text = text,
            style = CarLogTheme.typography.body,
            color = CarLogTheme.colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 14.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accent,
                checkedTrackColor = accent.copy(alpha = 0.45f),
                checkedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** Текстовая кнопка цветом формы: «+ More options», «+ Add». */
@Composable
fun FormTextButton(text: String, icon: ImageVector, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FormRow(null, modifier) {
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = accent)
            Text(text, style = CarLogTheme.typography.body, color = accent, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
fun SaveButton(text: String, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = CarLogTheme.typography.button, color = Color.White)
    }
}

/** Переключатель из двух-трёх вариантов: TRIP / FREIGHT. */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val colors = CarLogTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .border(1.dp, colors.outline, RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) accent else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.uppercase(),
                    style = CarLogTheme.typography.button,
                    color = if (selected) Color.White else colors.textPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Небольшой отступ между группами полей. */
@Composable
fun FormGap() {
    Box(Modifier.size(8.dp))
}
