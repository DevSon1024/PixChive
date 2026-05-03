package com.devson.pixchive.workers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.devson.pixchive.data.local.AppDatabase
import com.devson.pixchive.utils.FolderScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FolderSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val folderId = inputData.getString(KEY_FOLDER_ID)
        val folderUriString = inputData.getString(KEY_FOLDER_URI)
        val showHidden = inputData.getBoolean(KEY_SHOW_HIDDEN, false)
        // Allow callers to pass the last scan timestamp for delta-scan optimisation.
        // Defaults to 0L (full scan) if omitted.
        val lastScanTime = inputData.getLong(KEY_LAST_SCAN_TIME, 0L)

        if (folderId == null || folderUriString == null) {
            Log.e(TAG, "Missing folderId or folderUri")
            return@withContext Result.failure()
        }

        try {
            val uri = Uri.parse(folderUriString)
            val dao = AppDatabase.getDatabase(applicationContext).imageDao()

            Log.i(TAG, "Starting scan for folder $folderId (lastScanTime=$lastScanTime)")

            FolderScanner.scanAndInsert(
                context = applicationContext,
                folderUri = uri,
                folderId = folderId,
                imageDao = dao,
                showHidden = showHidden,
                lastScanTime = lastScanTime
            )

            Log.i(TAG, "Finished scan for $folderId")
            Result.success(workDataOf(KEY_LAST_SCAN_TIME to System.currentTimeMillis()))
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing folder", e)
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to e.localizedMessage))
        }
    }

    companion object {
        const val TAG = "FolderSyncWorker"
        const val KEY_FOLDER_ID = "folder_id"
        const val KEY_FOLDER_URI = "folder_uri"
        const val KEY_SHOW_HIDDEN = "show_hidden"
        const val KEY_LAST_SCAN_TIME = "last_scan_time"
        const val KEY_ERROR_MESSAGE = "error_message"
    }
}
