package com.artemkhateev.carlog.domain

import com.artemkhateev.carlog.data.model.Catalogs
import com.artemkhateev.carlog.data.model.Entry
import com.artemkhateev.carlog.data.model.Income
import com.artemkhateev.carlog.data.model.ItemizedEntry
import com.artemkhateev.carlog.data.model.Reading
import com.artemkhateev.carlog.data.model.Refueling
import com.artemkhateev.carlog.data.model.Route

/** Всё, по чему запись находится: названия из справочников, заметки, места, показания одометра. */
fun searchableText(entry: Entry, catalogs: Catalogs): String = buildList {
    add(entry.type.name)
    add(entry.notes)
    addAll(entry.odometerReadings.map { it.toString() })
    catalogs.item(entry.driverId)?.let { add(it.name) }
    when (entry) {
        is Refueling -> {
            catalogs.fuel(entry.fuelId)?.let { add(it.name) }
            catalogs.place(entry.placeId)?.let { add(it.name); add(it.address) }
            catalogs.item(entry.paymentMethodId)?.let { add(it.name) }
            catalogs.item(entry.reasonId)?.let { add(it.name) }
        }
        is ItemizedEntry -> {
            add(entry.title)
            entry.items.forEach { item -> catalogs.item(item.typeId)?.let { add(it.name) } }
            catalogs.place(entry.placeId)?.let { add(it.name); add(it.address) }
            catalogs.item(entry.paymentMethodId)?.let { add(it.name) }
            catalogs.item(entry.reasonId)?.let { add(it.name) }
        }
        is Income -> {
            add(entry.title)
            catalogs.item(entry.typeId)?.let { add(it.name) }
            catalogs.item(entry.reasonId)?.let { add(it.name) }
        }
        is Route -> {
            add(entry.origin)
            add(entry.destination)
            catalogs.item(entry.reasonId)?.let { add(it.name) }
        }
        is Reading -> Unit
    }
}.joinToString("\n").lowercase()

/** Записи, в которых есть каждое слово запроса, в любом порядке. */
fun searchEntries(entries: List<Entry>, catalogs: Catalogs, query: String): List<Entry> {
    val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()
    return entries.filter { entry ->
        val text = searchableText(entry, catalogs)
        words.all { it in text }
    }
}
