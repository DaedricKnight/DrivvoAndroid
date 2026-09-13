package com.artemkhateev.carlog.data.db

import android.content.res.Resources
import androidx.annotation.ArrayRes
import androidx.sqlite.db.SupportSQLiteDatabase
import com.artemkhateev.carlog.R
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.data.model.FuelCategory

/** Справочники новой базы. Названия берутся на языке телефона в момент первого запуска. */
class DefaultCatalogs(
    private val fuels: List<Pair<String, FuelCategory>>,
    private val items: List<Pair<CatalogKind, String>>,
) {
    fun insertInto(db: SupportSQLiteDatabase) {
        for ((name, category) in fuels) {
            db.execSQL("INSERT INTO fuels (name, category) VALUES (?, ?)", arrayOf(name, category.name))
        }
        for ((kind, name) in items) {
            db.execSQL("INSERT INTO catalog_items (kind, name) VALUES (?, ?)", arrayOf(kind.name, name))
        }
    }

    companion object {
        /** По порядку названий в R.array.seed_fuels. */
        private val FUEL_CATEGORIES = listOf(
            FuelCategory.Gasoline,
            FuelCategory.Gasoline,
            FuelCategory.Diesel,
            FuelCategory.Diesel,
            FuelCategory.Ethanol,
            FuelCategory.Lpg,
            FuelCategory.Cng,
            FuelCategory.Electricity,
        )

        fun from(resources: Resources): DefaultCatalogs {
            val fuelNames = resources.getStringArray(R.array.seed_fuels).toList()
            check(fuelNames.size == FUEL_CATEGORIES.size) { "seed_fuels и FUEL_CATEGORIES разошлись" }
            fun names(kind: CatalogKind, @ArrayRes arrayId: Int) = resources.getStringArray(arrayId).map { kind to it }
            return DefaultCatalogs(
                fuels = fuelNames.zip(FUEL_CATEGORIES),
                items = names(CatalogKind.ServiceType, R.array.seed_service_types) +
                    names(CatalogKind.ExpenseType, R.array.seed_expense_types) +
                    names(CatalogKind.IncomeType, R.array.seed_income_types) +
                    names(CatalogKind.Reason, R.array.seed_reasons) +
                    names(CatalogKind.PaymentMethod, R.array.seed_payment_methods),
            )
        }
    }
}
