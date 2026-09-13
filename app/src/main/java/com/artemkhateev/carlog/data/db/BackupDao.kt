package com.artemkhateev.carlog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.artemkhateev.carlog.data.backup.BackupFile

/** Чтение и замена всех таблиц разом — для резервной копии. */
@Dao
abstract class BackupDao {

    @Query("SELECT * FROM vehicles") abstract suspend fun vehicles(): List<VehicleEntity>
    @Query("SELECT * FROM fuels") abstract suspend fun fuels(): List<FuelEntity>
    @Query("SELECT * FROM places") abstract suspend fun places(): List<PlaceEntity>
    @Query("SELECT * FROM catalog_items") abstract suspend fun catalogItems(): List<CatalogItemEntity>
    @Query("SELECT * FROM refuelings") abstract suspend fun refuelings(): List<RefuelingEntity>
    @Query("SELECT * FROM expenses") abstract suspend fun expenses(): List<ExpenseEntity>
    @Query("SELECT * FROM expense_items") abstract suspend fun expenseItems(): List<ExpenseItemEntity>
    @Query("SELECT * FROM services") abstract suspend fun services(): List<ServiceEntity>
    @Query("SELECT * FROM service_items") abstract suspend fun serviceItems(): List<ServiceItemEntity>
    @Query("SELECT * FROM incomes") abstract suspend fun incomes(): List<IncomeEntity>
    @Query("SELECT * FROM routes") abstract suspend fun routes(): List<RouteEntity>
    @Query("SELECT * FROM readings") abstract suspend fun readings(): List<ReadingEntity>
    @Query("SELECT * FROM reminders") abstract suspend fun reminders(): List<ReminderEntity>

    @Insert abstract suspend fun insertVehicles(items: List<VehicleEntity>)
    @Insert abstract suspend fun insertFuels(items: List<FuelEntity>)
    @Insert abstract suspend fun insertPlaces(items: List<PlaceEntity>)
    @Insert abstract suspend fun insertCatalogItems(items: List<CatalogItemEntity>)
    @Insert abstract suspend fun insertRefuelings(items: List<RefuelingEntity>)
    @Insert abstract suspend fun insertExpenses(items: List<ExpenseEntity>)
    @Insert abstract suspend fun insertExpenseItems(items: List<ExpenseItemEntity>)
    @Insert abstract suspend fun insertServices(items: List<ServiceEntity>)
    @Insert abstract suspend fun insertServiceItems(items: List<ServiceItemEntity>)
    @Insert abstract suspend fun insertIncomes(items: List<IncomeEntity>)
    @Insert abstract suspend fun insertRoutes(items: List<RouteEntity>)
    @Insert abstract suspend fun insertReadings(items: List<ReadingEntity>)
    @Insert abstract suspend fun insertReminders(items: List<ReminderEntity>)

    @Query("DELETE FROM vehicles") abstract suspend fun clearVehicles()
    @Query("DELETE FROM fuels") abstract suspend fun clearFuels()
    @Query("DELETE FROM places") abstract suspend fun clearPlaces()
    @Query("DELETE FROM catalog_items") abstract suspend fun clearCatalogItems()
    @Query("DELETE FROM refuelings") abstract suspend fun clearRefuelings()
    @Query("DELETE FROM expenses") abstract suspend fun clearExpenses()
    @Query("DELETE FROM expense_items") abstract suspend fun clearExpenseItems()
    @Query("DELETE FROM services") abstract suspend fun clearServices()
    @Query("DELETE FROM service_items") abstract suspend fun clearServiceItems()
    @Query("DELETE FROM incomes") abstract suspend fun clearIncomes()
    @Query("DELETE FROM routes") abstract suspend fun clearRoutes()
    @Query("DELETE FROM readings") abstract suspend fun clearReadings()
    @Query("DELETE FROM reminders") abstract suspend fun clearReminders()

    @Transaction
    open suspend fun snapshot(exportedAt: String) = BackupFile(
        exportedAt = exportedAt,
        vehicles = vehicles(),
        fuels = fuels(),
        places = places(),
        catalogItems = catalogItems(),
        refuelings = refuelings(),
        expenses = expenses(),
        expenseItems = expenseItems(),
        services = services(),
        serviceItems = serviceItems(),
        incomes = incomes(),
        routes = routes(),
        readings = readings(),
        reminders = reminders(),
    )

    /** Всё или ничего: копия заменяет данные целиком в одной транзакции. */
    @Transaction
    open suspend fun replaceAll(file: BackupFile) {
        // Сначала то, что ссылается: справочники с RESTRICT не дадут удалить себя раньше записей.
        clearReminders()
        clearExpenseItems()
        clearServiceItems()
        clearRefuelings()
        clearExpenses()
        clearServices()
        clearIncomes()
        clearRoutes()
        clearReadings()
        clearVehicles()
        clearCatalogItems()
        clearPlaces()
        clearFuels()
        // Вставка в обратном порядке: сначала то, на что ссылаются.
        insertFuels(file.fuels)
        insertPlaces(file.places)
        insertCatalogItems(file.catalogItems)
        insertVehicles(file.vehicles)
        insertRefuelings(file.refuelings)
        insertExpenses(file.expenses)
        insertExpenseItems(file.expenseItems)
        insertServices(file.services)
        insertServiceItems(file.serviceItems)
        insertIncomes(file.incomes)
        insertRoutes(file.routes)
        insertReadings(file.readings)
        insertReminders(file.reminders)
    }
}
