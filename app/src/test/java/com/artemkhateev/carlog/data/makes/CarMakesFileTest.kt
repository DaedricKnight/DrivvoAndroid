package com.artemkhateev.carlog.data.makes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Проверяет файл, который едет в приложение: собирается он эвристиками, и их промахи лучше ловить здесь. */
class CarMakesFileTest {

    // Юнит-тесты запускаются из каталога модуля app.
    private val makes = CarMakesCatalog.parse(File("src/main/assets/car_makes.json").readText())

    @Test
    fun `well-known makes come with their models`() {
        assertTrue(makes.size > 150)
        mapOf(
            "Toyota" to "Corolla",
            "Volkswagen" to "Golf",
            "Škoda" to "Octavia",
            "Dacia" to "Logan",
            "Renault" to "Clio",
            "Kia" to "Sportage",
            "Hyundai" to "Tucson",
            "Tesla" to "Model 3",
            "Lada" to "Vesta",
            "BMW" to "3 Series",
        ).forEach { (make, model) ->
            assertTrue("$make $model", model in makes.modelsOf(make))
        }
    }

    @Test
    fun `makes and models are unique and never empty`() {
        val makeKeys = makes.map { searchKey(it.name) }
        assertEquals(makeKeys.size, makeKeys.toSet().size)
        makes.forEach { make ->
            val modelKeys = make.models.map(::searchKey)
            assertTrue(make.name, modelKeys.isNotEmpty() && modelKeys.none { it.isEmpty() })
            assertEquals(make.name, modelKeys.size, modelKeys.toSet().size)
        }
    }

    @Test
    fun `model names carry neither the make nor a generation`() {
        val generation = Regex("""\s(Mk\s?\d+|II|III|IV|V)$|\([^)]*\)$""")
        makes.forEach { make ->
            make.models.forEach { model ->
                assertFalse("${make.name}: $model", model.startsWith(make.name + " ", ignoreCase = true))
                assertFalse("${make.name}: $model", generation.containsMatchIn(model))
            }
        }
    }

    @Test
    fun `no electronics brands slipped in`() {
        listOf("Apple", "Canon", "Nintendo", "Sony").forEach { name ->
            assertTrue(name, makes.modelsOf(name).isEmpty())
        }
    }
}
