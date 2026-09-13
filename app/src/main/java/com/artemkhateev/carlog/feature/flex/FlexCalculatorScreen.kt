package com.artemkhateev.carlog.feature.flex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.ui.components.AppCard
import com.artemkhateev.carlog.ui.components.FormRow
import com.artemkhateev.carlog.ui.components.FormTextField
import com.artemkhateev.carlog.ui.components.TitleTopBar
import com.artemkhateev.carlog.ui.format.sanitizeDecimalInput
import com.artemkhateev.carlog.ui.navigation.AppNavigator
import com.artemkhateev.carlog.ui.theme.AccentScope
import com.artemkhateev.carlog.ui.theme.CarLogTheme

@Composable
fun FlexCalculatorScreen(navigator: AppNavigator) {
    val colors = CarLogTheme.colors
    val formats = CarLogTheme.formats
    var firstPrice by rememberSaveable { mutableStateOf("") }
    var secondPrice by rememberSaveable { mutableStateOf("") }
    var firstEfficiency by rememberSaveable { mutableStateOf("") }
    var secondEfficiency by rememberSaveable { mutableStateOf("") }
    val result = compareFuels(
        firstPrice = firstPrice.toDoubleOrNull() ?: 0.0,
        secondPrice = secondPrice.toDoubleOrNull() ?: 0.0,
        firstEfficiency = firstEfficiency.toDoubleOrNull(),
        secondEfficiency = secondEfficiency.toDoubleOrNull(),
        format = formats.consumptionFormat,
    )

    AccentScope(colors.brand) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .imePadding(),
        ) {
            TitleTopBar(stringResource(R.string.flex_title), colors.brand, onBack = navigator::back)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.flex_hint),
                    style = CarLogTheme.typography.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 64.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                )
                FormRow(Icons.Filled.LocalGasStation) {
                    FormTextField(
                        value = firstPrice,
                        onValueChange = { firstPrice = sanitizeDecimalInput(it, maxDecimals = 3) },
                        label = stringResource(R.string.flex_first_price),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(16.dp))
                    FormTextField(
                        value = secondPrice,
                        onValueChange = { secondPrice = sanitizeDecimalInput(it, maxDecimals = 3) },
                        label = stringResource(R.string.flex_second_price),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
                FormRow(Icons.Filled.Speed) {
                    FormTextField(
                        value = firstEfficiency,
                        onValueChange = { firstEfficiency = sanitizeDecimalInput(it, maxDecimals = 2) },
                        label = stringResource(R.string.flex_first_consumption, formats.consumptionLabel),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(16.dp))
                    FormTextField(
                        value = secondEfficiency,
                        onValueChange = { secondEfficiency = sanitizeDecimalInput(it, maxDecimals = 2) },
                        label = stringResource(R.string.flex_second_consumption, formats.consumptionLabel),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (result != null) FlexResultCard(result)
            }
        }
    }
}

@Composable
private fun FlexResultCard(result: FlexResult) {
    val colors = CarLogTheme.colors
    val typography = CarLogTheme.typography
    val formats = CarLogTheme.formats
    AppCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(
                when (result.cheaper) {
                    CheaperFuel.First -> R.string.flex_first_wins
                    CheaperFuel.Second -> R.string.flex_second_wins
                    CheaperFuel.Same -> R.string.flex_equal
                },
            ),
            style = typography.statHero,
            color = colors.brandText,
        )
        Text(
            text = stringResource(R.string.flex_ratio, formats.percent(result.priceRatioPercent)),
            style = typography.body,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 8.dp),
        )
        val first = result.firstCostPerDistance
        val second = result.secondCostPerDistance
        if (first != null && second != null) {
            listOf(R.string.flex_first_cost to first, R.string.flex_second_cost to second).forEach { (label, cost) ->
                Text(
                    text = stringResource(label, formats.money(cost * 100), "100 ${formats.distanceLabel}"),
                    style = typography.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
