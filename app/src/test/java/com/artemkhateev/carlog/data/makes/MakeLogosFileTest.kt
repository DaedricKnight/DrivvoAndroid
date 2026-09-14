package com.artemkhateev.carlog.data.makes

import androidx.compose.ui.graphics.vector.PathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Проверяет логотипы, которые едут в приложение: сборщик выбирает их по эвристикам и спискам, собранным вручную. */
class MakeLogosFileTest {

    // Юнит-тесты запускаются из каталога модуля app.
    private val assets = File("src/main/assets")
    private val logos = MakeLogosCatalog.parse(File(assets, "make_logos.json").readText())
    private val makes = CarMakesCatalog.parse(File(assets, "car_makes.json").readText())

    @Test
    fun `every logo belongs to a make from the list`() {
        val makeKeys = makes.map { searchKey(it.name) }.toSet()
        logos.values.forEach { logo -> assertTrue(logo.make, searchKey(logo.make) in makeKeys) }
    }

    @Test
    fun `popular makes have logos`() {
        listOf("Toyota", "Volkswagen", "Mercedes-Benz", "BMW", "Audi", "Škoda", "Renault", "Tesla", "Hyundai", "Kia", "Ford")
            .forEach { make -> assertNotNull(make, logos.forVehicle(make, "")) }
    }

    @Test
    fun `logo is either a colored path or an image`() {
        logos.values.forEach { logo ->
            val vector = logo.path != null && logo.color != null
            val image = logo.image != null && logo.path == null && logo.color == null
            assertTrue(logo.make, vector != image)
        }
    }

    @Test
    fun `paths parse and colors are hex`() {
        logos.values.filter { it.path != null }.forEach { logo ->
            assertTrue(logo.make, Regex("[0-9A-Fa-f]{6}").matches(logo.color.orEmpty()))
            assertTrue(logo.make, PathParser().parsePathString(logo.path!!).toNodes().isNotEmpty())
        }
    }

    @Test
    fun `images are webp files in assets and none is left unused`() {
        val used = logos.values.mapNotNull { it.image }.toSet()
        used.forEach { path ->
            val bytes = File(assets, path).readBytes()
            assertEquals(path, "RIFF", bytes.copyOfRange(0, 4).decodeToString())
            assertEquals(path, "WEBP", bytes.copyOfRange(8, 12).decodeToString())
        }
        val files = File(assets, "make_logos").listFiles().orEmpty().map { "make_logos/${it.name}" }.toSet()
        assertEquals(used, files)
    }

    @Test
    fun `images name their commons file and attribution licenses name the author`() {
        logos.values.filter { it.image != null }.forEach { logo -> assertNotNull(logo.make, logo.file) }
        logos.values.filter { it.license != null }.forEach { logo ->
            assertTrue(logo.make, logo.license!!.startsWith("CC BY"))
            assertTrue(logo.make, !logo.author.isNullOrBlank())
        }
    }

    @Test
    fun `commons page link escapes the file name`() {
        val logo = MakeLogo("Abarth", image = "make_logos/abarth.webp", file = "Abarth Logo (2).png")

        assertEquals("https://commons.wikimedia.org/wiki/File:Abarth_Logo_%282%29.png", logo.commonsPage)
    }

    @Test
    fun `vehicle without a make is matched by its name`() {
        val logo = MakeLogo("Škoda", path = "M0 0h24v24H0z", color = "4BA82E")
        val byKey = mapOf(searchKey(logo.make) to logo)

        assertNotNull(byKey.forVehicle(make = "", name = "Skoda"))
        assertNull(byKey.forVehicle(make = "", name = "My car"))
    }
}
