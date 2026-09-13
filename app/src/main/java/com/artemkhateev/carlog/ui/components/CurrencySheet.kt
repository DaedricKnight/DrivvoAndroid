package com.artemkhateev.carlog.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.settings.SettingsRepository
import java.util.Currency
import java.util.Locale

/** Выбор валюты: сначала валюта страны телефона и самые частые, дальше все по коду. */
@Composable
fun CurrencySheet(selectedCode: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val locale = Locale.getDefault()
    val currencies = remember(locale) {
        val suggested = listOf(SettingsRepository.defaultCurrencyCode(locale), "EUR", "USD", "GBP").distinct()
        val all = Currency.getAvailableCurrencies().sortedBy { it.currencyCode }
        suggested.mapNotNull { code -> all.firstOrNull { it.currencyCode == code } } + all.filterNot { it.currencyCode in suggested }
    }
    OptionSheet(
        title = stringResource(R.string.settings_currency),
        options = currencies,
        optionLabel = { "${it.currencyCode} · ${it.getDisplayName(locale)}" },
        isSelected = { it.currencyCode == selectedCode },
        onSelect = { onSelect(it.currencyCode) },
        onDismiss = onDismiss,
    )
}
