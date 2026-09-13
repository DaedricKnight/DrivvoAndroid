package com.artemkhateev.carlog.feature.more

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.ui.components.EmptyState
import com.artemkhateev.carlog.ui.navigation.AppNavigator

@Composable
fun MoreScreen(navigator: AppNavigator) {
    EmptyState(Icons.Filled.MoreHoriz, stringResource(R.string.more_title))
}
