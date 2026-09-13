package com.artemkhateev.carlog.data.backup

import com.artemkhateev.carlog.data.db.CatalogItemEntity
import com.artemkhateev.carlog.data.db.ExpenseEntity
import com.artemkhateev.carlog.data.db.ExpenseItemEntity
import com.artemkhateev.carlog.data.db.FuelEntity
import com.artemkhateev.carlog.data.db.IncomeEntity
import com.artemkhateev.carlog.data.db.PlaceEntity
import com.artemkhateev.carlog.data.db.ReadingEntity
import com.artemkhateev.carlog.data.db.RefuelingEntity
import com.artemkhateev.carlog.data.db.ReminderEntity
import com.artemkhateev.carlog.data.db.RouteEntity
import com.artemkhateev.carlog.data.db.ServiceEntity
import com.artemkhateev.carlog.data.db.ServiceItemEntity
import com.artemkhateev.carlog.data.db.VehicleEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Резервная копия: все таблицы как есть, с их id — связи между записями и справочниками сохраняются. */
@Serializable
data class BackupFile(
    val format: Int = FORMAT,
    val exportedAt: String,
    val vehicles: List<VehicleEntity>,
    val fuels: List<FuelEntity>,
    val places: List<PlaceEntity>,
    val catalogItems: List<CatalogItemEntity>,
    val refuelings: List<RefuelingEntity>,
    val expenses: List<ExpenseEntity>,
    val expenseItems: List<ExpenseItemEntity>,
    val services: List<ServiceEntity>,
    val serviceItems: List<ServiceItemEntity>,
    val incomes: List<IncomeEntity>,
    val routes: List<RouteEntity>,
    val readings: List<ReadingEntity>,
    val reminders: List<ReminderEntity>,
) {
    companion object {
        /** Растёт, когда меняется формат; копию новее, чем умеет приложение, не читаем. */
        const val FORMAT = 1

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun encode(file: BackupFile): String = json.encodeToString(serializer(), file)

        /** Бросает исключение, если это не копия или она из более новой версии приложения. */
        fun decode(text: String): BackupFile {
            val file = json.decodeFromString(serializer(), text)
            require(file.format <= FORMAT) { "Формат копии ${file.format} новее поддерживаемого $FORMAT" }
            return file
        }
    }
}
