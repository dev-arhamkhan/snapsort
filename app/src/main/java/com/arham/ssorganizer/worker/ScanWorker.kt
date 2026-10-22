package com.arham.ssorganizer.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arham.ssorganizer.data.local.AppDatabase
import com.arham.ssorganizer.data.local.entity.ScreenshotEntity
import com.arham.ssorganizer.data.ocr.OcrManager
import com.arham.ssorganizer.data.preferences.UserPreferences
import com.arham.ssorganizer.data.repository.ScreenshotRepository
import com.arham.ssorganizer.domain.detection.ScreenshotClassifier
import kotlinx.coroutines.flow.first

class ScanWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "ScanWorker"
        const val WORK_NAME = "screenshot_scan_work"
        const val CHANNEL_ID = "scan_progress_channel"
        const val NOTIFICATION_ID = 2001

        const val KEY_CURRENT = "key_current"
        const val KEY_TOTAL = "key_total"
        const val KEY_STATUS = "key_status"
        const val KEY_CURRENT_NAME = "key_current_name"
        const val KEY_ERROR_MESSAGE = "key_error_message"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val db = AppDatabase.getInstance(context)
    private val dao = db.screenshotDao()
    private val repository = ScreenshotRepository(context)
    private val prefs = UserPreferences(context)
    private val ocrManager = OcrManager(context)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        return createForegroundInfo(0, 0, "Initializing screenshot scan...")
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "=== ScanWorker started (id: $id) ===")
        createNotificationChannel()

        try {
            setForeground(createForegroundInfo(0, 0, "Discovering screenshots..."))
        } catch (e: Exception) {
            Log.w(TAG, "setForeground initial call warning: ${e.message}")
        }

        try {
            // Step 1: Query MediaStore for current screenshots
            val extraFolders = prefs.extraFolders.first()
            val mediaStoreItems = repository.getScreenshots(extraFolders)
            Log.d(TAG, "Discovered ${mediaStoreItems.size} screenshots in MediaStore")

            val activeMediaStoreIds = mediaStoreItems.map { it.id }.toSet()
            val existingDbIds = dao.getAllMediaStoreIds().toSet()

            // Step 2: Handle deleted screenshots (safe chunking)
            val deletedIds = existingDbIds - activeMediaStoreIds
            if (deletedIds.isNotEmpty()) {
                Log.d(TAG, "Pruning ${deletedIds.size} deleted screenshots from database")
                deletedIds.chunked(500).forEach { chunk ->
                    dao.deleteByMediaStoreIds(chunk)
                }
            }

            // Step 3: Insert new screenshots (chunked)
            val newIds = activeMediaStoreIds - existingDbIds
            if (newIds.isNotEmpty()) {
                val newItems = mediaStoreItems.filter { it.id in newIds }
                Log.d(TAG, "Inserting ${newItems.size} new screenshots as PENDING into database")
                newItems.chunked(250).forEach { chunk ->
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

            // Step 4: Query unprocessed (pending/failed) items
            val pendingScreenshots = dao.getUnprocessedScreenshots()
            val total = pendingScreenshots.size
            Log.d(TAG, "Total pending screenshots to scan with OCR: $total")

            if (total == 0) {
                Log.d(TAG, "No pending screenshots. Scan complete.")
                setProgress(
                    workDataOf(
                        KEY_CURRENT to 0,
                        KEY_TOTAL to 0,
                        KEY_STATUS to "All screenshots are indexed."
                    )
                )
                showFinishedNotification("Scan finished: all screenshots are already indexed.")
                return Result.success()
            }

            // Step 5: Process each screenshot sequentially
            for ((index, item) in pendingScreenshots.withIndex()) {
                if (isStopped) {
                    Log.d(TAG, "ScanWorker stopped/cancelled at item $index of $total. Progress is saved in DB.")
                    return Result.success()
                }

                val current = index + 1
                val progressText = "Scanning $current of $total"

                try {
                    setForeground(createForegroundInfo(current, total, progressText))
                } catch (e: Exception) {
                    Log.w(TAG, "Foreground notification update warning: ${e.message}")
                }

                setProgress(
                    workDataOf(
                        KEY_CURRENT to current,
                        KEY_TOTAL to total,
                        KEY_STATUS to progressText,
                        KEY_CURRENT_NAME to item.displayName
                    )
                )

                val startTime = System.currentTimeMillis()
                try {
                    val uri = Uri.parse(item.uri)
                    Log.d(TAG, "[$current/$total] Processing OCR for: ${item.displayName}")
                    val ocrResult = ocrManager.processImage(uri)
                    val duration = System.currentTimeMillis() - startTime

                    // Phase 3: Rule-based classification & chat name extraction
                    val classification = ScreenshotClassifier.classify(ocrResult.text, item.displayName, item.relativePath)

                    Log.d(
                        TAG,
                        "[$current/$total] OCR SUCCESS (${duration}ms) for ${item.displayName}: " +
                                "${ocrResult.text.length} chars -> App: ${classification.detectedApp} (${classification.confidence}), Chat: ${classification.chatName}"
                    )

                    dao.updateOcrResult(
                        mediaStoreId = item.mediaStoreId,
                        status = ScreenshotEntity.STATUS_DONE,
                        ocrText = ocrResult.text,
                        width = ocrResult.width,
                        height = ocrResult.height,
                        processedAt = System.currentTimeMillis()
                    )

                    dao.updateClassification(
                        mediaStoreId = item.mediaStoreId,
                        detectedApp = classification.detectedApp,
                        category = classification.category.name,
                        chatName = classification.chatName,
                        appConfidence = classification.confidence,
                        classificationStatus = classification.classificationStatus,
                        matchedKeywords = classification.matchedKeywords.joinToString(", ")
                    )
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    Log.e(
                        TAG,
                        "[$current/$total] OCR FAILED (${duration}ms) for ${item.displayName}: ${e.javaClass.simpleName} - ${e.message}"
                    )
                    dao.updateOcrResult(
                        mediaStoreId = item.mediaStoreId,
                        status = ScreenshotEntity.STATUS_FAILED,
                        ocrText = "",
                        width = 0,
                        height = 0,
                        processedAt = System.currentTimeMillis()
                    )
                    dao.updateClassification(
                        mediaStoreId = item.mediaStoreId,
                        detectedApp = "Other",
                        category = "OTHER",
                        chatName = null,
                        appConfidence = 0f,
                        classificationStatus = ScreenshotEntity.STATUS_UNCLASSIFIED,
                        matchedKeywords = ""
                    )
                }
            }

            Log.d(TAG, "=== ScanWorker completed successfully: processed $total screenshots ===")
            showFinishedNotification("Scan complete: indexed $total screenshots.")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "ScanWorker fatal error in doWork", e)
            return Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Unknown scanning error"))
            )
        }
    }

    private fun createForegroundInfo(current: Int, total: Int, message: String): ForegroundInfo {
        val cancelIntent = WorkManager.getInstance(context).createCancelPendingIntent(id)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("SnapSort")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)

        if (total > 0) {
            builder.setProgress(total, current, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, builder.build())
        }
    }

    private fun showFinishedNotification(summary: String) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("SnapSort: Scan Complete")
                .setContentText(summary)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID + 1, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Could not show completion notification: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screenshot Scan Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live OCR progress while scanning screenshots"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
