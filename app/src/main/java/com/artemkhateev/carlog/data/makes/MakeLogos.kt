package com.artemkhateev.carlog.data.makes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.net.URLEncoder

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
    /** Картинка — круглый значок с фото: заливает весь круг, а не вписывается в белый. */
    val fill: Boolean = false,
    /** Файл картинки на Wikimedia Commons. */
    val file: String? = null,
    /** Лицензия, требующая указать автора (CC BY, CC BY-SA); у файлов в общественном достоянии её нет. */
    val license: String? = null,
    val author: String? = null,
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

private val localFile = Regex("^([a-z-]+):(.+)$")

/**
 * Страница файла с автором, лицензией и оригиналом: на Wikimedia Commons, у «en:…» — в разделе Википедии,
 * а у фото с других сайтов (Flickr) в [MakeLogo.file] уже лежит адрес страницы.
 */
val MakeLogo.filePage: String?
    get() = file?.let { name ->
        if (name.startsWith("https://")) return@let name
        val local = localFile.matchEntire(name)
        val host = local?.let { "${it.groupValues[1]}.wikipedia.org" } ?: "commons.wikimedia.org"
        val title = local?.groupValues?.get(2) ?: name
        "https://$host/wiki/File:" + URLEncoder.encode(title.replace(' ', '_'), "UTF-8")
    }
