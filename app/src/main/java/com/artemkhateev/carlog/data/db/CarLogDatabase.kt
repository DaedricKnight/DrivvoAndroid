package com.artemkhateev.carlog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VehicleEntity::class,
        FuelEntity::class,
        PlaceEntity::class,
        CatalogItemEntity::class,
        RefuelingEntity::class,
        ExpenseEntity::class,
        ExpenseItemEntity::class,
        ServiceEntity::class,
        ServiceItemEntity::class,
        IncomeEntity::class,
        RouteEntity::class,
        ReadingEntity::class,
        ReminderEntity::class,
    ],
    version = 1,
)
abstract class CarLogDatabase : RoomDatabase() {
    abstract fun dao(): CarLogDao

    abstract fun backupDao(): BackupDao

    companion object {
        fun build(context: Context): CarLogDatabase {
            val defaults = DefaultCatalogs.from(context.resources)
            return Room.databaseBuilder(context, CarLogDatabase::class.java, "carlog.db")
                .addCallback(object : Callback() {
                    // Стартовые справочники — один раз, при создании базы: дальше их правит пользователь.
                    override fun onCreate(db: SupportSQLiteDatabase) = defaults.insertInto(db)
                })
                .build()
        }
    }
}
