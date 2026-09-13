package com.artemkhateev.carlog.data

import android.database.sqlite.SQLiteConstraintException
import com.artemkhateev.carlog.data.db.CarLogDao
import com.artemkhateev.carlog.data.db.itemEntities
import com.artemkhateev.carlog.data.db.toEntity
import com.artemkhateev.carlog.data.db.toModel
import com.artemkhateev.carlog.data.model.CatalogItem
import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.Entry
import com.artemkhateev.carlog.data.model.EntryNewestFirst
import com.artemkhateev.carlog.data.model.EntryType
import com.artemkhateev.carlog.data.model.Expense
import com.artemkhateev.carlog.data.model.Fuel
import com.artemkhateev.carlog.data.model.Income
import com.artemkhateev.carlog.data.model.Place
import com.artemkhateev.carlog.data.model.Reading
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Reminder
import com.artemkhateev.carlog.data.model.Route
import com.artemkhateev.carlog.data.model.Service
import com.artemkhateev.carlog.data.model.Vehicle
import com.artemkhateev.carlog.domain.ReminderUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class CarLogRepository(private val dao: CarLogDao) {

    val vehicles: Flow<List<Vehicle>> = dao.observeVehicles().map { list -> list.map { it.toModel() } }

    val catalogs: Flow<Catalogs> =
        combine(dao.observeFuels(), dao.observePlaces(), dao.observeCatalogItems()) { fuels, places, items ->
            Catalogs(
                fuels = fuels.map { it.toModel() },
                places = places.map { it.toModel() },
                items = items.map { it.toModel() },
            )
        }

    /** Все записи машины, новые сверху. */
    fun entries(vehicleId: Long): Flow<List<Entry>> {
        val fuelAndCosts = combine(
            dao.observeRefuelings(vehicleId),
            dao.observeExpenses(vehicleId),
            dao.observeServices(vehicleId),
        ) { refuelings, expenses, services ->
            buildList<Entry> {
                refuelings.mapTo(this) { it.toModel() }
                expenses.mapTo(this) { it.toModel() }
                services.mapTo(this) { it.toModel() }
            }
        }
        val others = combine(
            dao.observeIncomes(vehicleId),
            dao.observeRoutes(vehicleId),
            dao.observeReadings(vehicleId),
        ) { incomes, routes, readings ->
            buildList<Entry> {
                incomes.mapTo(this) { it.toModel() }
                routes.mapTo(this) { it.toModel() }
                readings.mapTo(this) { it.toModel() }
            }
        }
        return combine(fuelAndCosts, others) { first, second -> (first + second).sortedWith(EntryNewestFirst) }
    }

    fun reminders(vehicleId: Long): Flow<List<Reminder>> =
        dao.observeReminders(vehicleId).map { list -> list.map { it.toModel() } }

    suspend fun vehicle(id: Long): Vehicle? = dao.getVehicle(id)?.toModel()

    suspend fun saveVehicle(vehicle: Vehicle): Long {
        val entity = vehicle.toEntity()
        return if (entity.id == 0L) dao.insertVehicle(entity) else entity.id.also { dao.updateVehicle(entity) }
    }

    /** Вместе с машиной удаляются все её записи и напоминания. */
    suspend fun deleteVehicle(id: Long) = dao.deleteVehicle(id)

    suspend fun entry(type: EntryType, id: Long): Entry? = when (type) {
        EntryType.Refueling -> dao.getRefueling(id)?.toModel()
        EntryType.Expense -> dao.getExpense(id)?.toModel()
        EntryType.Income -> dao.getIncome(id)?.toModel()
        EntryType.Service -> dao.getService(id)?.toModel()
        EntryType.Route -> dao.getRoute(id)?.toModel()
        EntryType.Reading -> dao.getReading(id)?.toModel()
    }

    /** Снимок записей машины — для фоновой проверки напоминаний. */
    suspend fun entriesSnapshot(vehicleId: Long): List<Entry> = entries(vehicleId).first()

    suspend fun saveEntry(entry: Entry): Long = when (entry) {
        is Refueling -> entry.toEntity().let { entity ->
            if (entity.id == 0L) dao.insertRefueling(entity) else entity.id.also { dao.updateRefueling(entity) }
        }
        is Expense -> dao.saveExpense(entry.toEntity(), entry.itemEntities())
        is Service -> dao.saveService(entry.toEntity(), entry.itemEntities())
        is Income -> entry.toEntity().let { entity ->
            if (entity.id == 0L) dao.insertIncome(entity) else entity.id.also { dao.updateIncome(entity) }
        }
        is Route -> entry.toEntity().let { entity ->
            if (entity.id == 0L) dao.insertRoute(entity) else entity.id.also { dao.updateRoute(entity) }
        }
        is Reading -> entry.toEntity().let { entity ->
            if (entity.id == 0L) dao.insertReading(entity) else entity.id.also { dao.updateReading(entity) }
        }
    }

    suspend fun deleteEntry(type: EntryType, id: Long) = when (type) {
        EntryType.Refueling -> dao.deleteRefueling(id)
        EntryType.Expense -> dao.deleteExpense(id)
        EntryType.Income -> dao.deleteIncome(id)
        EntryType.Service -> dao.deleteService(id)
        EntryType.Route -> dao.deleteRoute(id)
        EntryType.Reading -> dao.deleteReading(id)
    }

    suspend fun saveFuel(fuel: Fuel): Long {
        val entity = fuel.toEntity()
        return if (entity.id == 0L) dao.insertFuel(entity) else entity.id.also { dao.updateFuel(entity) }
    }

    /** false — топливом заправлялись, удалить его нельзя. */
    suspend fun deleteFuel(id: Long): Boolean = deleteUnlessReferenced { dao.deleteFuel(id) }

    suspend fun savePlace(place: Place): Long {
        val entity = place.toEntity()
        return if (entity.id == 0L) dao.insertPlace(entity) else entity.id.also { dao.updatePlace(entity) }
    }

    /** Место из записей просто пропадает: оно необязательное. */
    suspend fun deletePlace(id: Long) = dao.deletePlace(id)

    suspend fun saveCatalogItem(item: CatalogItem): Long {
        val entity = item.toEntity()
        return if (entity.id == 0L) dao.insertCatalogItem(entity) else entity.id.also { dao.updateCatalogItem(entity) }
    }

    /** false — вид расхода, сервиса или дохода уже есть в записях или напоминаниях. */
    suspend fun deleteCatalogItem(id: Long): Boolean = deleteUnlessReferenced { dao.deleteCatalogItem(id) }

    suspend fun reminder(id: Long): Reminder? = dao.getReminder(id)?.toModel()

    suspend fun remindersSnapshot(vehicleId: Long): List<Reminder> = dao.getReminders(vehicleId).map { it.toModel() }

    suspend fun saveReminder(reminder: Reminder): Long {
        val entity = reminder.toEntity()
        return if (entity.id == 0L) dao.insertReminder(entity) else entity.id.also { dao.updateReminder(entity) }
    }

    suspend fun deleteReminder(id: Long) = dao.deleteReminder(id)

    suspend fun applyReminderUpdates(updates: List<ReminderUpdate>) {
        for (update in updates) {
            when (update) {
                is ReminderUpdate.Reschedule -> saveReminder(update.reminder)
                is ReminderUpdate.Close -> deleteReminder(update.reminderId)
            }
        }
    }

    private suspend fun deleteUnlessReferenced(delete: suspend () -> Unit): Boolean =
        try {
            delete()
            true
        } catch (_: SQLiteConstraintException) {
            // Внешний ключ с RESTRICT: на элемент ещё ссылаются записи.
            false
        }
}
