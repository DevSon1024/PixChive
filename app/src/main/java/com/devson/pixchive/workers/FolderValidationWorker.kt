package com.devson.pixchive.workers

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devson.pixchive.PixChiveApplication
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class FolderValidationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting folder validation background work...")
        val preferencesManager = PreferencesManager(applicationContext)
        val database = (applicationContext as PixChiveApplication).database
        val imageDao = database.imageDao()
        val historyDao = database.historyDao()

        val currentFolders = preferencesManager.foldersFlow.first()
        if (currentFolders.isEmpty()) {
            return@withContext Result.success()
        }

        val validFolders = mutableListOf<ComicFolder>()
        var changed = false

        for (folder in currentFolders) {
            val uri = Uri.parse(folder.uri)
            val hasPermission = applicationContext.contentResolver.persistedUriPermissions.any { it.uri == uri }

            val exists = if (hasPermission) {
                try {
                    val documentId = DocumentsContract.getTreeDocumentId(uri)
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
                    applicationContext.contentResolver.query(
                        docUri,
                        arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        cursor.moveToFirst()
                    } ?: false
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }

            if (exists) {
                validFolders.add(folder)
            } else {
                changed = true
                Log.w(TAG, "Folder not found or no permission: ${folder.name} (${folder.uri}). Cleaning up from database.")
                imageDao.deleteFolderContent(folder.id)
                historyDao.deleteHistoryForFolder(folder.id)
            }
        }

        if (changed) {
            preferencesManager.saveFolders(validFolders)
            Log.i(TAG, "Folder list modified. Saved updated folder preferences.")
        }

        Log.i(TAG, "Folder validation background work completed successfully.")
        Result.success()
    }

    companion object {
        private const val TAG = "FolderValidationWorker"
    }
}
