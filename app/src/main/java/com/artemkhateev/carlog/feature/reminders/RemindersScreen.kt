package com.artemkhateev.carlog.feature.reminders

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.ui.components.EmptyState
import com.artemkhateev.carlog.ui.navigation.AppNavigator

@Composable
fun RemindersScreen(navigator: AppNavigator) {
    EmptyState(Icons.Filled.Alarm, stringResource(R.string.reminders_empty))
}
