package com.artemkhateev.carlog.data.backup

import com.artemkhateev.carlog.data.db.CatalogItemEntity
import com.artemkhateev.carlog.data.db.RefuelingEntity
import com.artemkhateev.carlog.data.db.ReminderEntity
import com.artemkhateev.carlog.data.db.VehicleEntity
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.data.model.ReminderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupFileTest {

    private val file = BackupFile(
        exportedAt = "2025-03-10T12:00",
        vehicles = listOf(VehicleEntity(1, "Van", "Make", "Model", 2019, "AB-123", 60_000, null, 2, true, "")),
        fuels = emptyList(),
        places = emptyList(),
        catalogItems = listOf(CatalogItemEntity(4, CatalogKind.ServiceType, "Oil")),
        refuelings = listOf(RefuelingEntity(9, 1, "2025-03-02T18:00", 10_000, null, 1_500, 6_000, 40_000, true, false, null, null, null, null, "note")),
        expenses = emptyList(),
        expenseItems = emptyList(),
        services = emptyList(),
        serviceItems = emptyList(),
        incomes = emptyList(),
        routes = emptyList(),
        readings = emptyList(),
        reminders = listOf(ReminderEntity(2, 1, ReminderKind.Service, 4, "", 20_000, "2025-06-01", 10_000, 12, "", null)),
    )

    @Test
    fun `survives a round trip`() {
        assertEquals(file, BackupFile.decode(BackupFile.encode(file)))
    }

    @Test
    fun `ignores fields it does not know`() {
        val withExtra = BackupFile.encode(file).replaceFirst("{", "{\"futureField\":true,")

        assertEquals(file, BackupFile.decode(withExtra))
    }

    @Test
    fun `refuses a newer format and random json`() {
        val newer = BackupFile.encode(file.copy(format = BackupFile.FORMAT + 1))

        assertThrows(IllegalArgumentException::class.java) { BackupFile.decode(newer) }
        assertThrows(Exception::class.java) { BackupFile.decode("{\"hello\":1}") }
    }
}
