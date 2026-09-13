package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.CatalogItem
import com.artemkhateev.carlog.data.model.CatalogKind
import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.Fuel
import com.artemkhateev.carlog.data.model.FuelCategory
import com.artemkhateev.carlog.data.model.Place
import com.artemkhateev.carlog.data.model.PlaceKind
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTest {

    private val catalogs = Catalogs(
        fuels = listOf(Fuel(1, "Diesel", FuelCategory.Diesel)),
        places = listOf(Place(3, "Riverside station", PlaceKind.GasStation, "Harbour road 5")),
        items = listOf(CatalogItem(5, CatalogKind.ServiceType, "Oil filter")),
    )

    private val entries = listOf(
        refueling(id = 1, odometer = 10_000, milli = 40_000).copy(placeId = 3),
        service(id = 1, odometer = 10_500, dateTime = at(3, 5), 5L to 2_000).copy(notes = "Changed at the dealer"),
        reading(id = 1, odometer = 11_000, dateTime = at(3, 9)),
    )

    @Test
    fun `finds by catalog names, places and notes`() {
        assertEquals(listOf(entries[0]), searchEntries(entries, catalogs, "diesel"))
        assertEquals(listOf(entries[0]), searchEntries(entries, catalogs, "harbour"))
        assertEquals(listOf(entries[1]), searchEntries(entries, catalogs, "DEALER"))
        assertEquals(listOf(entries[1]), searchEntries(entries, catalogs, "filter oil"))
    }

    @Test
    fun `every word must match and odometer digits count`() {
        assertEquals(emptyList<Any>(), searchEntries(entries, catalogs, "diesel dealer"))
        assertEquals(listOf(entries[2]), searchEntries(entries, catalogs, "11000"))
        assertEquals(emptyList<Any>(), searchEntries(entries, catalogs, "   "))
    }
}
