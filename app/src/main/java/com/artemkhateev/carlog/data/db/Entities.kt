package com.artemkhateev.carlog.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.data.model.FuelCategory
import com.artemkhateev.carlog.data.model.PlaceKind
import com.artemkhateev.carlog.data.model.ReminderKind
import com.artemkhateev.carlog.data.model.RouteKind
import kotlinx.serialization.Serializable

/*
 * Таблицы базы — они же формат резервной копии.
 *
 * Даты — строки без часового пояса ("2026-08-29T21:15", "2026-08-29"): так они сортируются как даты
 * и читаются в копии. Деньги — в центах, объём — в тысячных литра, цена за единицу — в тысячных валюты.
 *
 * Справочники, без которых запись теряет смысл (топливо, виды расходов, сервиса и доходов), нельзя удалить,
 * пока на них ссылаются (RESTRICT). Необязательные — водитель, место, способ оплаты, цель — при удалении
 * просто очищаются в записях (SET NULL).
 */

@Serializable
@Entity(
    tableName = "vehicles",
    foreignKeys = [
        ForeignKey(entity = FuelEntity::class, parentColumns = ["id"], childColumns = ["fuelId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("fuelId")],
)
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val make: String,
    val model: String,
    val year: Int?,
    val plate: String,
    val tankCapacityMilli: Long?,
    val fuelId: Long?,
    val colorIndex: Int,
    val active: Boolean,
    val notes: String,
)

@Serializable
@Entity(tableName = "fuels")
data class FuelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: FuelCategory,
)

@Serializable
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: PlaceKind,
    val address: String,
)

@Serializable
@Entity(tableName = "catalog_items", indices = [Index("kind")])
data class CatalogItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: CatalogKind,
    val name: String,
)

@Serializable
@Entity(
    tableName = "refuelings",
    foreignKeys = [
        ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FuelEntity::class, parentColumns = ["id"], childColumns = ["fuelId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = PlaceEntity::class, parentColumns = ["id"], childColumns = ["placeId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["driverId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["paymentMethodId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["reasonId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index("vehicleId"), Index("fuelId"), Index("placeId"), Index("driverId"), Index("paymentMethodId"), Index("reasonId"),
    ],
)
data class RefuelingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateTime: String,
    val odometer: Long,
    val fuelId: Long?,
    val unitPriceMilli: Long,
    val totalCostMinor: Long,
    val volumeMilli: Long,
    val fullTank: Boolean,
    val missedPrevious: Boolean,
    val placeId: Long?,
    val driverId: Long?,
    val paymentMethodId: Long?,
    val reasonId: Long?,
    val notes: String,
)

@Serializable
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlaceEntity::class, parentColumns = ["id"], childColumns = ["placeId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["driverId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["paymentMethodId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["reasonId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("vehicleId"), Index("placeId"), Index("driverId"), Index("paymentMethodId"), Index("reasonId")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateTime: String,
    val odometer: Long?,
    val title: String,
    val discountMinor: Long,
    val placeId: Long?,
    val driverId: Long?,
    val paymentMethodId: Long?,
    val reasonId: Long?,
    val notes: String,
)

@Serializable
@Entity(
    tableName = "expense_items",
    foreignKeys = [
        ForeignKey(entity = ExpenseEntity::class, parentColumns = ["id"], childColumns = ["expenseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["typeId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("expenseId"), Index("typeId")],
)
data class ExpenseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val typeId: Long,
    val amountMinor: Long,
)

data class ExpenseWithItems(
    @Embedded val expense: ExpenseEntity,
    @Relation(parentColumn = "id", entityColumn = "expenseId") val items: List<ExpenseItemEntity>,
)

@Serializable
@Entity(
    tableName = "services",
    foreignKeys = [
        ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlaceEntity::class, parentColumns = ["id"], childColumns = ["placeId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["driverId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["paymentMethodId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["reasonId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("vehicleId"), Index("placeId"), Index("driverId"), Index("paymentMethodId"), Index("reasonId")],
)
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateTime: String,
    val odometer: Long?,
    val title: String,
    val discountMinor: Long,
    val placeId: Long?,
    val driverId: Long?,
    val paymentMethodId: Long?,
    val reasonId: Long?,
    val notes: String,
)

@Serializable
@Entity(
    tableName = "service_items",
    foreignKeys = [
        ForeignKey(entity = ServiceEntity::class, parentColumns = ["id"], childColumns = ["serviceId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["typeId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("serviceId"), Index("typeId")],
)
data class ServiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceId: Long,
    val typeId: Long,
    val amountMinor: Long,
)

data class ServiceWithItems(
    @Embedded val service: ServiceEntity,
    @Relation(parentColumn = "id", entityColumn = "serviceId") val items: List<ServiceItemEntity>,
)

@Serializable
@Entity(
    tableName = "incomes",
    foreignKeys = [
        ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["typeId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["driverId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["reasonId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("vehicleId"), Index("typeId"), Index("driverId"), Index("reasonId")],
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateTime: String,
    val odometer: Long?,
    val title: String,
    val typeId: Long?,
    val amountMinor: Long,
    val driverId: Long?,
    val reasonId: Long?,
    val notes: String,
)

@Serializable
@Entity(
    tableName = "routes",
    foreignKeys = [
        ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["driverId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["reasonId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("vehicleId"), Index("driverId"), Index("reasonId")],
)
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val origin: String,
    val startDateTime: String,
    val startOdometer: Long,
    val destination: String,
    val endDateTime: String,
    val endOdometer: Long,
    val kind: RouteKind,
    val rateMilli: Long?,
    val freightValueMinor: Long?,
    val driverId: Long?,
    val reasonId: Long?,
    val notes: String,
)

@Serializable
@Entity(
    tableName = "readings",
    foreignKeys = [
        ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["driverId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("vehicleId"), Index("driverId")],
)
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateTime: String,
    val odometer: Long,
    val driverId: Long?,
    val notes: String,
)

@Serializable
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(entity = VehicleEntity::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CatalogItemEntity::class, parentColumns = ["id"], childColumns = ["typeId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("vehicleId"), Index("typeId")],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val kind: ReminderKind,
    val typeId: Long?,
    val title: String,
    val dueOdometer: Long?,
    val dueDate: String?,
    val repeatDistance: Long?,
    val repeatMonths: Int?,
    val notes: String,
    val notifiedKey: String?,
)
