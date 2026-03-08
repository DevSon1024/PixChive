package com.devson.pixchive.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.devson.pixchive.MainActivity
import com.devson.pixchive.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

object BackupRestoreManager {

    private const val DB_NAME = "pixchive_database"
    private const val PREFS_FILE_NAME = "pixchive_prefs.preferences_pb"

    suspend fun performBackup(context: Context, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Perform WAL Checkpoint
            val db = AppDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { 
                it.moveToFirst() 
            }

            // 2. Locate files
            val dbFile = context.getDatabasePath(DB_NAME)
            val dbWalFile = context.getDatabasePath("$DB_NAME-wal")
            val dbShmFile = context.getDatabasePath("$DB_NAME-shm")

            // DataStore file is in filesDir/datastore/
            val datastoreDir = File(context.filesDir, "datastore")
            val prefsFile = File(datastoreDir, PREFS_FILE_NAME)

            val filesToBackup = listOfNotNull(
                dbFile.takeIf { it.exists() },
                dbWalFile.takeIf { it.exists() },
                dbShmFile.takeIf { it.exists() },
                prefsFile.takeIf { it.exists() }
            )

            if (filesToBackup.isEmpty()) return@withContext false

            // 3. Zip files into outputUri
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    for (file in filesToBackup) {
                        FileInputStream(file).use { fis ->
                            val zipEntry = ZipEntry(file.name)
                            zipOut.putNextEntry(zipEntry)
                            fis.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }
                }
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun performRestore(context: Context, inputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Close active database
            AppDatabase.closeDatabase()

            // 2. Prepare destination directories
            val dbDir = context.getDatabasePath(DB_NAME).parentFile
            val datastoreDir = File(context.filesDir, "datastore")
            
            dbDir?.mkdirs()
            datastoreDir.mkdirs()

            // 3. Unzip files from inputUri
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val fileName = entry.name
                        val targetFile = when {
                            fileName.startsWith(DB_NAME) -> File(dbDir, fileName)
                            fileName == PREFS_FILE_NAME -> File(datastoreDir, fileName)
                            else -> null
                        }

                        if (targetFile != null) {
                            FileOutputStream(targetFile).use { fos ->
                                zipIn.copyTo(fos)
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            } ?: return@withContext false

            // 4. Trigger App Restart
            restartApp(context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun restartApp(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        exitProcess(0)
    }
}
