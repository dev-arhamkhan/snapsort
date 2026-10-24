package com.arham.snapsort.domain.model

import android.net.Uri

/**
 * Lightweight model representing one screenshot found via MediaStore.
 * Used in Phase 1 for display; will be backed by a Room entity in Phase 2.
 */
data class ScreenshotItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,        // epoch seconds (from MediaStore)
    val relativePath: String    // e.g. "Pictures/Screenshots/"
)
