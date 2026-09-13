package com.artemkhateev.carlog.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.ui.theme.EntryColors

val EntryType.accent: Color
    get() = when (this) {
        EntryType.Refueling -> EntryColors.Refueling
        EntryType.Expense -> EntryColors.Expense
        EntryType.Income -> EntryColors.Income
        EntryType.Service -> EntryColors.Service
        EntryType.Route -> EntryColors.Route
        EntryType.Reading -> EntryColors.Reading
    }

val EntryType.icon: ImageVector
    get() = when (this) {
        EntryType.Refueling -> Icons.Filled.LocalGasStation
        EntryType.Expense -> Icons.Filled.CreditCard
        EntryType.Income -> Icons.Filled.AddCard
        EntryType.Service -> Icons.Filled.Build
        EntryType.Route -> Icons.Filled.Route
        EntryType.Reading -> Icons.Filled.Speed
    }

@get:StringRes
val EntryType.titleRes: Int
    get() = when (this) {
        EntryType.Refueling -> R.string.entry_refueling
        EntryType.Expense -> R.string.entry_expense
        EntryType.Income -> R.string.entry_income
        EntryType.Service -> R.string.entry_service
        EntryType.Route -> R.string.entry_route
        EntryType.Reading -> R.string.entry_reading
    }

val ReminderIcon: ImageVector get() = Icons.Filled.Alarm

/** Цветной кружок (или скруглённый квадрат) с белым значком — метка типа в ленте, меню и карточках. */
@Composable
fun TypeBadge(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    shape: Shape = CircleShape,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}
