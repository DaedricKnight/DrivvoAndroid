package com.artemkhateev.carlog.data.export

import com.artemkhateev.carlog.data.model.CatalogItem
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.Fuel
import com.artemkhateev.carlog.data.model.FuelCategory
import com.artemkhateev.carlog.data.model.Vehicle
import com.artemkhateev.carlog.domain.at
import com.artemkhateev.carlog.domain.refueling
import com.artemkhateev.carlog.domain.service
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExportTest {

    @Test
    fun `escapes only cells that need it`() {
        assertEquals("plain", CsvExport.escape("plain"))
        assertEquals("\"a, b\"", CsvExport.escape("a, b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExport.escape("say \"hi\""))
        assertEquals("\"two\nlines\"", CsvExport.escape("two\nlines"))
    }

    @Test
    fun `rows go oldest first with plain numbers`() {
        val catalogs = Catalogs(
            fuels = listOf(Fuel(1, "Diesel", FuelCategory.Diesel)),
            items = listOf(CatalogItem(7, CatalogKind.ServiceType, "Oil, filter")),
        )
        val vehicle = Vehicle(id = 1, name = "Van")
        val entries = listOf(
            service(id = 2, odometer = 10_300, dateTime = at(3, 8, hour = 9), 7L to 4_500),
            refueling(id = 1, odometer = 10_000, milli = 38_215, dateTime = at(3, 2, hour = 18), priceMilli = 1_500),
        )

        val lines = CsvExport.build(listOf(vehicle), mapOf(1L to entries), catalogs).split("\r\n")

        assertEquals(CsvExport.header.joinToString(","), lines[0])
        assertEquals("Van,Refueling,2025-03-02,18:00,10000,,,,Diesel,38.215,1.5,yes,57.32,,,,,,,,", lines[1])
        assertEquals("Van,Service,2025-03-08,09:00,10300,,,\"Oil, filter 45.00\",,,,,45.00,,,,,,,,", lines[2])
        assertEquals("", lines[3])
    }
}
