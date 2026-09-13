package com.artemkhateev.carlog.data.makes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.text.Normalizer

@Serializable
data class CarMake(val name: String, val models: List<String>)

/** Файл `assets/car_makes.json`; собирает его `tools/car-makes`. */
@Serializable
data class CarMakesFile(
    /** Откуда и когда собран список. */
    val source: String = "",
    val makes: List<CarMake>,
)

/** Марки и модели из assets. Файл немаленький, поэтому читается один раз и только когда понадобился. */
class CarMakesCatalog(private val open: () -> InputStream) {
    private val mutex = Mutex()

    @Volatile
    private var cached: List<CarMake>? = null

    suspend fun makes(): List<CarMake> = cached ?: mutex.withLock {
        cached ?: withContext(Dispatchers.IO) { open().use { parse(it.readBytes().decodeToString()) } }.also { cached = it }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): List<CarMake> = json.decodeFromString(CarMakesFile.serializer(), text).makes
    }
}

/** Модели марки; марка ищется без регистра и диакритики, так что «skoda» находит «Škoda». */
fun List<CarMake>.modelsOf(make: String): List<String> {
    val key = searchKey(make)
    if (key.isEmpty()) return emptyList()
    return firstOrNull { searchKey(it.name) == key }?.models.orEmpty()
}

private val combiningMarks = Regex("\\p{Mn}+")

/** Ключ поиска без регистра, диакритики и знаков: «Škoda» → «skoda», «Mercedes-Benz» → «mercedesbenz». */
fun searchKey(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase().filter { it.isLetterOrDigit() }

/** Сначала названия, которые начинаются с запроса, потом те, где он внутри; внутри групп порядок прежний. */
fun <T> filterByName(items: List<T>, query: String, name: (T) -> String): List<T> {
    val key = searchKey(query)
    if (key.isEmpty()) return items
    val keyed = items.map { it to searchKey(name(it)) }
    val (starting, other) = keyed.partition { it.second.startsWith(key) }
    return starting.map { it.first } + other.filter { key in it.second }.map { it.first }
}
