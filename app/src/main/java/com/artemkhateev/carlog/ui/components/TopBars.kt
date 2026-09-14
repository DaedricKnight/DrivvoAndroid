package com.artemkhateev.carlog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.makes.forVehicle
import com.artemkhateev.carlog.data.model.Vehicle
import com.artemkhateev.carlog.ui.theme.CarLogTheme
import com.artemkhateev.carlog.ui.theme.VehicleColors

/** Цветная шапка вместе с полосой статуса. */
@Composable
fun ColoredTopBar(color: Color, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color)
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun TitleTopBar(
    title: String,
    color: Color,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    ColoredTopBar(color, modifier) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
            }
        }
        Text(
            text = title,
            style = CarLogTheme.typography.appBarTitle,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack != null) 20.dp else 12.dp),
        )
        actions()
    }
}

@Composable
fun TopBarAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = Color.White)
    }
}

/** Шапка главных экранов: выбор машины слева, действия справа. */
@Composable
fun VehicleTopBar(
    color: Color,
    vehicle: Vehicle?,
    odometer: Long?,
    onVehicleClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    ColoredTopBar(color) {
        Box(Modifier.weight(1f)) {
            if (vehicle != null) VehicleChip(vehicle, odometer, onVehicleClick)
        }
        actions()
    }
}

@Composable
fun VehicleChip(vehicle: Vehicle, odometer: Long?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val typography = CarLogTheme.typography
    val formats = CarLogTheme.formats
    Row(
        modifier = modifier
            .padding(start = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VehicleAvatar(vehicle, size = 34.dp)
        Column(
            Modifier
                .padding(start = 10.dp)
                .weight(1f, fill = false),
        ) {
            Text(vehicle.name, style = typography.chipTitle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = listOfNotNull(vehicle.description.ifBlank { null }, odometer?.let { formats.distance(it) })
                .joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = typography.chipSubtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Аватар машины: логотип марки в белом круге, как у референса; у марки без логотипа — буква на цвете машины. */
@Composable
fun VehicleAvatar(vehicle: Vehicle, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val logo = LocalMakeLogos.current.forVehicle(vehicle.make, vehicle.name)
    if (logo != null) {
        MakeLogoBadge(logo, modifier, size)
        return
    }
    val color = VehicleColors[vehicle.colorIndex.mod(VehicleColors.size)]
    val letter = vehicle.make.ifBlank { vehicle.name }.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size - 4.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(letter, color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold)
        }
    }
}
