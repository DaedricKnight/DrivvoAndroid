package com.artemkhateev.carlog.data.backup

import android.content.ContentResolver
import android.net.Uri
import com.artemkhateev.carlog.data.CarLogRepository
import com.artemkhateev.carlog.data.db.BackupDao
import com.artemkhateev.carlog.data.db.toDbString
import com.artemkhateev.carlog.data.export.CsvExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime

/** Выгрузка в CSV, резервная копия и восстановление из неё — через файлы, выбранные в системном диалоге. */
class DataTransfer(
    private val backupDao: BackupDao,
    private val repository: CarLogRepository,
    private val resolver: ContentResolver,
) {
    suspend fun exportCsv(uri: Uri) {
        val vehicles = repository.vehicles.first()
        val entries = vehicles.associate { it.id to repository.entriesSnapshot(it.id) }
        // BOM — чтобы таблицы узнали UTF-8 и не испортили названия мест не на латинице.
        write(uri, "﻿" + CsvExport.build(vehicles, entries, repository.catalogs.first()))
    }

    suspend fun exportBackup(uri: Uri, now: LocalDateTime = LocalDateTime.now()) {
        write(uri, BackupFile.encode(backupDao.snapshot(now.toDbString())))
    }

    /** Разбирает копию целиком до того, как что-то удалить: битый файл данные не тронет. */
    suspend fun importBackup(uri: Uri) {
        val text = withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: throw IOException("Не открылся $uri")
        }
        backupDao.replaceAll(BackupFile.decode(text))
    }

    private suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        resolver.openOutputStream(uri, "wt")?.use { it.write(text.encodeToByteArray()) } ?: throw IOException("Не открылся $uri")
    }
}
