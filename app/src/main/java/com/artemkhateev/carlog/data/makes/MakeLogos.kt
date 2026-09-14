package com.artemkhateev.carlog.data.makes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

/** Логотип марки: контур из Simple Icons ([path] и [color]) либо картинка из Wikimedia Commons ([image]). */
@Serializable
data class MakeLogo(
    /** Марка так, как она названа в car_makes.json. */
    val make: String,
    /** Контур в формате SVG path. */
    val path: String? = null,
    /** Фирменный цвет контура, RRGGBB. */
    val color: String? = null,
    /** Путь к картинке WebP в assets. */
    val image: String? = null,
)

/** Файл `assets/make_logos.json`; собирает его `tools/make-logos`. */
@Serializable
data class MakeLogosFile(
    val source: String = "",
    val logos: List<MakeLogo>,
)

/** Логотипы марок из assets по ключу поиска марки: «Škoda» и «skoda» находят один и тот же. */
class MakeLogosCatalog(private val open: () -> InputStream) {
    private val mutex = Mutex()

    @Volatile
    private var cached: Map<String, MakeLogo>? = null

    suspend fun logos(): Map<String, MakeLogo> = cached ?: mutex.withLock {
        cached ?: withContext(Dispatchers.IO) { open().use { parse(it.readBytes().decodeToString()) } }.also { cached = it }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): Map<String, MakeLogo> =
            json.decodeFromString(MakeLogosFile.serializer(), text).logos.associateBy { searchKey(it.make) }
    }
}

/** Логотип машины по марке, а без марки — по имени: машину часто так и называют. */
fun Map<String, MakeLogo>.forVehicle(make: String, name: String): MakeLogo? = this[searchKey(make)] ?: this[searchKey(name)]
