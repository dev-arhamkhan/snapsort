package com.arham.ssorganizer.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arham.ssorganizer.data.local.AppDatabase
import com.arham.ssorganizer.data.local.dao.AppCount
import com.arham.ssorganizer.data.local.dao.ChatCount
import com.arham.ssorganizer.data.local.entity.ScreenshotEntity
import com.arham.ssorganizer.domain.detection.ScreenshotClassifier
import com.arham.ssorganizer.domain.model.ScreenshotItem
import com.arham.ssorganizer.worker.ScanWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Coordinates MediaStore queries, Room database caching, rule-based classification,
 * and WorkManager scan triggering.
 */
class ScreenshotRepository(private val context: Context) {

    companion object {
        const val TAG = "ScreenshotRepo"
    }

    private val db = AppDatabase.getInstance(context)
    val dao = db.screenshotDao()
    private val workManager = WorkManager.getInstance(context)

    val allScreenshotsFlow: Flow<List<ScreenshotEntity>> = dao.getAllScreenshotsFlow()
    val totalCountFlow: Flow<Int> = dao.getTotalCountFlow()
    val doneCountFlow: Flow<Int> = dao.getDoneCountFlow()
    val pendingCountFlow: Flow<Int> = dao.getPendingCountFlow()
    val failedCountFlow: Flow<Int> = dao.getFailedCountFlow()
    val lastScanTimeFlow: Flow<Long?> = dao.getLastScanTimeFlow()
    val appCountsFlow: Flow<List<AppCount>> = dao.getAppCountsFlow()

    val scanWorkInfoFlow: Flow<WorkInfo?> =
        workManager.getWorkInfosForUniqueWorkFlow(ScanWorker.WORK_NAME)
            .map { list -> list.firstOrNull() }

    fun getScreenshotsByAppFlow(appName: String): Flow<List<ScreenshotEntity>> =
        dao.getScreenshotsByAppFlow(appName)

    fun getScreenshotsByChatFlow(appName: String, chatName: String?): Flow<List<ScreenshotEntity>> {
        return if (chatName.isNullOrBlank()) {
            dao.getScreenshotsWithoutChatFlow(appName)
        } else {
            dao.getScreenshotsByChatFlow(appName, chatName)
        }
    }

    fun getChatCountsFlow(appName: String): Flow<List<ChatCount>> =
        dao.getChatCountsFlow(appName)

    fun searchScreenshotsFlow(
        query: String,
        appName: String?,
        chatName: String?
    ): Flow<List<ScreenshotEntity>> = dao.searchScreenshotsFlow(query.trim(), appName, chatName)

    suspend fun searchScreenshots(
        query: String,
        appName: String?,
        chatName: String?
    ): List<ScreenshotEntity> = dao.searchScreenshots(query.trim(), appName, chatName)

    /**
     * Start or resume background scan worker.
     */
    fun startScan() {
        Log.d(TAG, "Requesting ScanWorker enqueue...")
        val scanRequest = OneTimeWorkRequestBuilder<ScanWorker>()
            .addTag(ScanWorker.WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            ScanWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            scanRequest
        )
        Log.d(TAG, "ScanWorker enqueued with ExistingWorkPolicy.KEEP")
    }

    /**
     * Cancel any currently running scan worker.
     */
    fun cancelScan() {
        Log.d(TAG, "Cancelling unique work: ${ScanWorker.WORK_NAME}")
        workManager.cancelUniqueWork(ScanWorker.WORK_NAME)
    }

    /**
     * Re-runs rule classification on already scanned screenshots in Room.
     * Takes only a few milliseconds for hundreds of items and requires NO image rescanning.
     */
    suspend fun classifyAllPersistedScreenshots() = withContext(Dispatchers.IO) {
        val unclassifiedList = dao.getScreenshotsForAutoClassification()
        Log.d(TAG, "Running offline rule classification on ${unclassifiedList.size} persisted screenshots...")

        var classifiedCount = 0
        for (item in unclassifiedList) {
            val result = ScreenshotClassifier.classify(item.ocrText, item.displayName, item.relativePath)
            dao.updateClassification(
                mediaStoreId = item.mediaStoreId,
                detectedApp = result.detectedApp,
                category = result.category.name,
                chatName = result.chatName,
                appConfidence = result.confidence,
                classificationStatus = result.classificationStatus,
                matchedKeywords = result.matchedKeywords.joinToString(", ")
            )
            if (result.classificationStatus == ScreenshotClassifier.STATUS_CLASSIFIED) {
                classifiedCount++
            }
        }
        Log.d(TAG, "Classification finished: $classifiedCount of ${unclassifiedList.size} classified into known apps.")
    }

    /**
     * Save user's manual correction for a screenshot.
     */
    suspend fun updateManualCorrection(mediaStoreId: Long, detectedApp: String, chatName: String?) =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Saving manual correction for $mediaStoreId -> App: $detectedApp, Chat: $chatName")
            dao.updateManualCorrection(mediaStoreId, detectedApp.trim(), chatName?.trim()?.ifBlank { null })
        }

    /**
     * Synchronize Room database with current MediaStore state.
     */
    suspend fun syncWithMediaStore(extraPaths: Set<String> = emptySet()) = withContext(Dispatchers.IO) {
        Log.d(TAG, "syncWithMediaStore starting...")
        val mediaStoreScreenshots = getScreenshots(extraPaths)
        Log.d(TAG, "MediaStore returned ${mediaStoreScreenshots.size} items")

        val activeIds = mediaStoreScreenshots.map { it.id }.toSet()
        val existingIds = dao.getAllMediaStoreIds().toSet()

        // 1. Delete removed screenshots
        val toDelete = existingIds - activeIds
        if (toDelete.isNotEmpty()) {
            Log.d(TAG, "Deleting ${toDelete.size} removed screenshots from DB")
            toDelete.chunked(500).forEach { chunk ->
                dao.deleteByMediaStoreIds(chunk)
            }
        }

        // 2. Insert new screenshots
        val toInsertIds = activeIds - existingIds
        if (toInsertIds.isNotEmpty()) {
            val toInsertItems = mediaStoreScreenshots.filter { it.id in toInsertIds }
            Log.d(TAG, "Inserting ${toInsertItems.size} new screenshots into DB")
            toInsertItems.chunked(250).forEach { chunk ->
                val entities = chunk.map { item ->
                    ScreenshotEntity(
                        mediaStoreId = item.id,
                        uri = item.uri.toString(),
                        displayName = item.displayName,
                        relativePath = item.relativePath,
                        dateAdded = item.dateAdded,
                        status = ScreenshotEntity.STATUS_PENDING
                    )
                }
                dao.insertAll(entities)
            }
        }
        Log.d(TAG, "syncWithMediaStore completed. Total in DB: ${dao.getAllMediaStoreIds().size}")
    }

    /**
     * Queries MediaStore for all screenshots.
     */
    suspend fun getScreenshots(extraPaths: Set<String> = emptySet()): List<ScreenshotItem> =
        withContext(Dispatchers.IO) {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.RELATIVE_PATH
            )

            val conditions = mutableListOf<String>()
            val args = mutableListOf<String>()

            // Default: matches anything containing "screenshots"
            conditions.add("LOWER(${MediaStore.Images.Media.RELATIVE_PATH}) LIKE ?")
            args.add("%screenshots%")

            for (path in extraPaths) {
                if (path.isNotBlank()) {
                    conditions.add("LOWER(${MediaStore.Images.Media.RELATIVE_PATH}) LIKE ?")
                    args.add("%${path.lowercase()}%")
                }
            }

            val selection = conditions.joinToString(" OR ")
            val selectionArgs = args.toTypedArray()
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val results = mutableListOf<ScreenshotItem>()

            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    results.add(
                        ScreenshotItem(
                            id = id,
                            uri = uri,
                            displayName = cursor.getString(nameCol) ?: "",
                            dateAdded = cursor.getLong(dateCol),
                            relativePath = cursor.getString(pathCol) ?: ""
                        )
                    )
                }
            }

            results
        }
}
