package com.artemkhateev.carlog.feature.reports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.ui.components.EmptyState

@Composable
fun ReportsScreen() {
    EmptyState(Icons.Filled.StackedLineChart, stringResource(R.string.tab_reports))
}
