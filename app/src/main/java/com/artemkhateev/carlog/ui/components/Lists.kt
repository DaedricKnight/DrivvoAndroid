package com.artemkhateev.carlog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.ui.theme.CarLogTheme

/** Строка списка: значок или аватар в левой колонке, название, подпись и черта под текстом. */
@Composable
fun ListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    divider: Boolean = true,
    dimmed: Boolean = false,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(if (dimmed) 0.55f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) { leading() }
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = typography.body, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = typography.listSubtitle,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                trailing()
            }
            if (divider) HorizontalDivider(Modifier.padding(end = 16.dp), color = colors.divider)
        }
    }
}

/** Круглая кнопка «+» в правом нижнем углу списков. */
@Composable
fun AddFab(onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = CarLogTheme.colors.brand) {
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(20.dp)
            .size(56.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add), tint = Color.White, modifier = Modifier.size(28.dp))
    }
}
