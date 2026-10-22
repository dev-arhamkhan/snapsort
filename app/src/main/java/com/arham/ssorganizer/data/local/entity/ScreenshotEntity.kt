package com.arham.ssorganizer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an indexed screenshot, its OCR result, and rule-based classification.
 */
@Entity(
    tableName = "screenshots",
    indices = [
        Index(value = ["mediaStoreId"], unique = true),
        Index(value = ["status"]),
        Index(value = ["detectedApp"]),
        Index(value = ["category"]),
        Index(value = ["chatName"])
    ]
)
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaStoreId: Long,
    val uri: String,
    val displayName: String,
    val relativePath: String,
    val dateAdded: Long,            // epoch seconds
    val width: Int = 0,
    val height: Int = 0,
    val ocrText: String = "",
    val detectedApp: String = "Pending",
    val chatName: String? = null,
    val status: String = STATUS_PENDING, // "pending", "done", "failed"
    val processedAt: Long? = null,  // epoch milliseconds when OCR was executed

    // Phase 3 Classification metadata
    val appConfidence: Float = 0f,
    val classificationSource: String = SOURCE_AUTO, // "auto", "manual"
    val category: String = "Other",
    val classificationStatus: String = STATUS_UNCLASSIFIED, // "classified", "unclassified", "unsupported_language_or_no_text"
    val matchedKeywords: String = ""
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_DONE = "done"
        const val STATUS_FAILED = "failed"

        const val SOURCE_AUTO = "auto"
        const val SOURCE_MANUAL = "manual"

        const val STATUS_CLASSIFIED = "classified"
        const val STATUS_UNCLASSIFIED = "unclassified"
        const val STATUS_UNSUPPORTED_LANGUAGE = "unsupported_language_or_no_text"
    }
}
