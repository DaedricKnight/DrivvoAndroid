package com.artemkhateev.carlog.data.makes

import org.junit.Assert.assertEquals
import org.junit.Test

class NameSearchTest {

    @Test
    fun `search key ignores case, diacritics and punctuation`() {
        assertEquals("skoda", searchKey("Škoda"))
        assertEquals("citroen", searchKey("Citroën"))
        assertEquals("rollsroyce", searchKey("Rolls-Royce"))
        assertEquals("3series", searchKey("3 Series"))
    }

    @Test
    fun `names starting with the query come first`() {
        val names = listOf("Land Rover", "Alfa Romeo", "Rover", "Renault")

        assertEquals(listOf("Rover", "Land Rover"), filterByName(names, "rover") { it })
        assertEquals(listOf("Rover", "Renault", "Land Rover", "Alfa Romeo"), filterByName(names, "r") { it })
        assertEquals(names, filterByName(names, "  ") { it })
    }

    @Test
    fun `models are found by make regardless of spelling`() {
        val makes = listOf(CarMake("Škoda", listOf("Fabia", "Octavia")), CarMake("Seat", listOf("Ibiza")))

        assertEquals(listOf("Fabia", "Octavia"), makes.modelsOf("skoda"))
        assertEquals(emptyList<String>(), makes.modelsOf("Unknown"))
        assertEquals(emptyList<String>(), makes.modelsOf(""))
    }
}
