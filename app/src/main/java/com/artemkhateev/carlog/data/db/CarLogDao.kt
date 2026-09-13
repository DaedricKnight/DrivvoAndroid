package com.artemkhateev.carlog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CarLogDao {

    // Машины

    @Query("SELECT * FROM vehicles ORDER BY active DESC, name COLLATE NOCASE")
    abstract fun observeVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    abstract suspend fun getVehicle(id: Long): VehicleEntity?

    @Insert
    abstract suspend fun insertVehicle(entity: VehicleEntity): Long

    @Update
    abstract suspend fun updateVehicle(entity: VehicleEntity)

    /** Записи и напоминания машины удаляются каскадом. */
    @Query("DELETE FROM vehicles WHERE id = :id")
    abstract suspend fun deleteVehicle(id: Long)

    // Справочники

    @Query("SELECT * FROM fuels ORDER BY name COLLATE NOCASE")
    abstract fun observeFuels(): Flow<List<FuelEntity>>

    @Insert
    abstract suspend fun insertFuel(entity: FuelEntity): Long

    @Update
    abstract suspend fun updateFuel(entity: FuelEntity)

    @Query("DELETE FROM fuels WHERE id = :id")
    abstract suspend fun deleteFuel(id: Long)

    @Query("SELECT * FROM places ORDER BY name COLLATE NOCASE")
    abstract fun observePlaces(): Flow<List<PlaceEntity>>

    @Insert
    abstract suspend fun insertPlace(entity: PlaceEntity): Long

    @Update
    abstract suspend fun updatePlace(entity: PlaceEntity)

    @Query("DELETE FROM places WHERE id = :id")
    abstract suspend fun deletePlace(id: Long)

    @Query("SELECT * FROM catalog_items ORDER BY name COLLATE NOCASE")
    abstract fun observeCatalogItems(): Flow<List<CatalogItemEntity>>

    @Insert
    abstract suspend fun insertCatalogItem(entity: CatalogItemEntity): Long

    @Update
    abstract suspend fun updateCatalogItem(entity: CatalogItemEntity)

    @Query("DELETE FROM catalog_items WHERE id = :id")
    abstract suspend fun deleteCatalogItem(id: Long)

    // Записи

    @Query("SELECT * FROM refuelings WHERE vehicleId = :vehicleId")
    abstract fun observeRefuelings(vehicleId: Long): Flow<List<RefuelingEntity>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId")
    abstract fun observeExpenses(vehicleId: Long): Flow<List<ExpenseWithItems>>

    @Transaction
    @Query("SELECT * FROM services WHERE vehicleId = :vehicleId")
    abstract fun observeServices(vehicleId: Long): Flow<List<ServiceWithItems>>

    @Query("SELECT * FROM incomes WHERE vehicleId = :vehicleId")
    abstract fun observeIncomes(vehicleId: Long): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM routes WHERE vehicleId = :vehicleId")
    abstract fun observeRoutes(vehicleId: Long): Flow<List<RouteEntity>>

    @Query("SELECT * FROM readings WHERE vehicleId = :vehicleId")
    abstract fun observeReadings(vehicleId: Long): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM refuelings WHERE id = :id")
    abstract suspend fun getRefueling(id: Long): RefuelingEntity?

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id")
    abstract suspend fun getExpense(id: Long): ExpenseWithItems?

    @Transaction
    @Query("SELECT * FROM services WHERE id = :id")
    abstract suspend fun getService(id: Long): ServiceWithItems?

    @Query("SELECT * FROM incomes WHERE id = :id")
    abstract suspend fun getIncome(id: Long): IncomeEntity?

    @Query("SELECT * FROM routes WHERE id = :id")
    abstract suspend fun getRoute(id: Long): RouteEntity?

    @Query("SELECT * FROM readings WHERE id = :id")
    abstract suspend fun getReading(id: Long): ReadingEntity?

    @Insert
    abstract suspend fun insertRefueling(entity: RefuelingEntity): Long

    @Update
    abstract suspend fun updateRefueling(entity: RefuelingEntity)

    @Query("DELETE FROM refuelings WHERE id = :id")
    abstract suspend fun deleteRefueling(id: Long)

    @Insert
    abstract suspend fun insertExpense(entity: ExpenseEntity): Long

    @Update
    abstract suspend fun updateExpense(entity: ExpenseEntity)

    @Insert
    abstract suspend fun insertExpenseItems(items: List<ExpenseItemEntity>)

    @Query("DELETE FROM expense_items WHERE expenseId = :expenseId")
    abstract suspend fun deleteExpenseItems(expenseId: Long)

    @Query("DELETE FROM expenses WHERE id = :id")
    abstract suspend fun deleteExpense(id: Long)

    /** Строки перезаписываются целиком: их единицы, и сопоставлять старые с новыми незачем. */
    @Transaction
    open suspend fun saveExpense(entity: ExpenseEntity, items: List<ExpenseItemEntity>): Long {
        val id = if (entity.id == 0L) insertExpense(entity) else entity.id.also { updateExpense(entity) }
        deleteExpenseItems(id)
        insertExpenseItems(items.map { it.copy(id = 0, expenseId = id) })
        return id
    }

    @Insert
    abstract suspend fun insertService(entity: ServiceEntity): Long

    @Update
    abstract suspend fun updateService(entity: ServiceEntity)

    @Insert
    abstract suspend fun insertServiceItems(items: List<ServiceItemEntity>)

    @Query("DELETE FROM service_items WHERE serviceId = :serviceId")
    abstract suspend fun deleteServiceItems(serviceId: Long)

    @Query("DELETE FROM services WHERE id = :id")
    abstract suspend fun deleteService(id: Long)

    @Transaction
    open suspend fun saveService(entity: ServiceEntity, items: List<ServiceItemEntity>): Long {
        val id = if (entity.id == 0L) insertService(entity) else entity.id.also { updateService(entity) }
        deleteServiceItems(id)
        insertServiceItems(items.map { it.copy(id = 0, serviceId = id) })
        return id
    }

    @Insert
    abstract suspend fun insertIncome(entity: IncomeEntity): Long

    @Update
    abstract suspend fun updateIncome(entity: IncomeEntity)

    @Query("DELETE FROM incomes WHERE id = :id")
    abstract suspend fun deleteIncome(id: Long)

    @Insert
    abstract suspend fun insertRoute(entity: RouteEntity): Long

    @Update
    abstract suspend fun updateRoute(entity: RouteEntity)

    @Query("DELETE FROM routes WHERE id = :id")
    abstract suspend fun deleteRoute(id: Long)

    @Insert
    abstract suspend fun insertReading(entity: ReadingEntity): Long

    @Update
    abstract suspend fun updateReading(entity: ReadingEntity)

    @Query("DELETE FROM readings WHERE id = :id")
    abstract suspend fun deleteReading(id: Long)

    // Напоминания

    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId")
    abstract fun observeReminders(vehicleId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId")
    abstract suspend fun getReminders(vehicleId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    abstract suspend fun getReminder(id: Long): ReminderEntity?

    @Insert
    abstract suspend fun insertReminder(entity: ReminderEntity): Long

    @Update
    abstract suspend fun updateReminder(entity: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    abstract suspend fun deleteReminder(id: Long)
}
