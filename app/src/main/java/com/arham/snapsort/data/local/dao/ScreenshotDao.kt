package com.arham.snapsort.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arham.snapsort.data.local.entity.ScreenshotEntity
import kotlinx.coroutines.flow.Flow

data class AppCount(
    val detectedApp: String,
    val count: Int
)

data class ChatCount(
    val chatName: String?,
    val count: Int
)

@Dao
interface ScreenshotDao {

    @Query("SELECT * FROM screenshots ORDER BY dateAdded DESC")
    fun getAllScreenshotsFlow(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots ORDER BY dateAdded DESC")
    suspend fun getAllScreenshots(): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE status = 'done' AND detectedApp = :appName ORDER BY dateAdded DESC")
    fun getScreenshotsByAppFlow(appName: String): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE status = 'done' AND detectedApp = :appName AND chatName = :chatName ORDER BY dateAdded DESC")
    fun getScreenshotsByChatFlow(appName: String, chatName: String): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE status = 'done' AND detectedApp = :appName AND (chatName IS NULL OR chatName = '') ORDER BY dateAdded DESC")
    fun getScreenshotsWithoutChatFlow(appName: String): Flow<List<ScreenshotEntity>>

    @Query("""
        SELECT chatName, COUNT(*) as count 
        FROM screenshots 
        WHERE status = 'done' AND detectedApp = :appName 
        GROUP BY chatName 
        ORDER BY count DESC
    """)
    fun getChatCountsFlow(appName: String): Flow<List<ChatCount>>

    @Query("""
        SELECT * FROM screenshots 
        WHERE (
            :query = '' 
            OR ocrText LIKE '%' || :query || '%' 
            OR displayName LIKE '%' || :query || '%' 
            OR detectedApp LIKE '%' || :query || '%' 
            OR chatName LIKE '%' || :query || '%'
        )
        AND (:appName IS NULL OR detectedApp = :appName)
        AND (
            :chatName IS NULL 
            OR (:chatName = '__unspecified__' AND (chatName IS NULL OR chatName = '')) 
            OR chatName = :chatName
        )
        ORDER BY dateAdded DESC
    """)
    fun searchScreenshotsFlow(
        query: String,
        appName: String?,
        chatName: String?
    ): Flow<List<ScreenshotEntity>>

    @Query("""
        SELECT * FROM screenshots 
        WHERE (
            :query = '' 
            OR ocrText LIKE '%' || :query || '%' 
            OR displayName LIKE '%' || :query || '%' 
            OR detectedApp LIKE '%' || :query || '%' 
            OR chatName LIKE '%' || :query || '%'
        )
        AND (:appName IS NULL OR detectedApp = :appName)
        AND (
            :chatName IS NULL 
            OR (:chatName = '__unspecified__' AND (chatName IS NULL OR chatName = '')) 
            OR chatName = :chatName
        )
        ORDER BY dateAdded DESC
    """)
    suspend fun searchScreenshots(
        query: String,
        appName: String?,
        chatName: String?
    ): List<ScreenshotEntity>

    @Query("SELECT mediaStoreId FROM screenshots")
    suspend fun getAllMediaStoreIds(): List<Long>

    @Query("SELECT * FROM screenshots WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun getByMediaStoreId(mediaStoreId: Long): ScreenshotEntity?

    @Query("SELECT * FROM screenshots WHERE status != 'done' ORDER BY dateAdded DESC")
    suspend fun getUnprocessedScreenshots(): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE status = 'done' AND classificationSource = 'auto'")
    suspend fun getScreenshotsForAutoClassification(): List<ScreenshotEntity>

    @Query("SELECT COUNT(*) FROM screenshots")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots WHERE status = 'done'")
    fun getDoneCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots WHERE status = 'pending'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots WHERE status = 'failed'")
    fun getFailedCountFlow(): Flow<Int>

    @Query("SELECT MAX(processedAt) FROM screenshots WHERE status = 'done'")
    fun getLastScanTimeFlow(): Flow<Long?>

    @Query("SELECT detectedApp, COUNT(*) as count FROM screenshots WHERE status = 'done' GROUP BY detectedApp ORDER BY count DESC")
    fun getAppCountsFlow(): Flow<List<AppCount>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(screenshots: List<ScreenshotEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(screenshot: ScreenshotEntity): Long

    @Update
    suspend fun update(screenshot: ScreenshotEntity)

    @Query("""
        UPDATE screenshots 
        SET status = :status, 
            ocrText = :ocrText, 
            width = :width, 
            height = :height, 
            processedAt = :processedAt 
        WHERE mediaStoreId = :mediaStoreId
    """)
    suspend fun updateOcrResult(
        mediaStoreId: Long,
        status: String,
        ocrText: String,
        width: Int,
        height: Int,
        processedAt: Long
    )

    @Query("""
        UPDATE screenshots 
        SET detectedApp = :detectedApp, 
            category = :category, 
            chatName = :chatName, 
            appConfidence = :appConfidence, 
            classificationStatus = :classificationStatus, 
            matchedKeywords = :matchedKeywords, 
            classificationSource = :classificationSource
        WHERE mediaStoreId = :mediaStoreId
    """)
    suspend fun updateClassification(
        mediaStoreId: Long,
        detectedApp: String,
        category: String,
        chatName: String?,
        appConfidence: Float,
        classificationStatus: String,
        matchedKeywords: String,
        classificationSource: String = ScreenshotEntity.SOURCE_AUTO
    )

    @Query("""
        UPDATE screenshots 
        SET detectedApp = :detectedApp, 
            chatName = :chatName, 
            classificationSource = 'manual', 
            classificationStatus = 'classified'
        WHERE mediaStoreId = :mediaStoreId
    """)
    suspend fun updateManualCorrection(
        mediaStoreId: Long,
        detectedApp: String,
        chatName: String?
    )

    @Query("DELETE FROM screenshots WHERE mediaStoreId IN (:ids)")
    suspend fun deleteByMediaStoreIds(ids: List<Long>): Int

    @Query("DELETE FROM screenshots WHERE mediaStoreId = :mediaStoreId")
    suspend fun deleteByMediaStoreId(mediaStoreId: Long)

    @Query("DELETE FROM screenshots")
    suspend fun clearAll()
}
