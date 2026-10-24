package com.arham.snapsort.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Result of an OCR operation on a single screenshot.
 */
data class OcrResult(
    val text: String,
    val width: Int,
    val height: Int
)

/**
 * Handles downscaled bitmap decoding and on-device ML Kit OCR.
 *
 * Designed specifically for low-memory devices (like Redmi 8 with 4GB RAM):
 *  1. Uses inJustDecodeBounds first to get dimensions without allocating pixel memory.
 *  2. Computes inSampleSize targeting ~1600px on the longest side.
 *  3. Decodes the downscaled bitmap.
 *  4. Feeds to ML Kit TextRecognizer.
 *  5. Explicitly recycles the Bitmap in a finally block to keep memory flat.
 */
class OcrManager(private val context: Context) {

    // TextRecognizer instance reused across scans
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Decodes and runs OCR on the image at [uri].
     *
     * @return [OcrResult] containing recognized text and original dimensions.
     * @throws Exception if image decoding fails or OCR execution times out.
     */
    suspend fun processImage(uri: Uri): OcrResult = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        // Step 1: Decode bounds only
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
        } finally {
            inputStream?.close()
        }

        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight
        if (origWidth <= 0 || origHeight <= 0) {
            throw IllegalArgumentException("Invalid image dimensions ($origWidth x $origHeight) for URI: $uri")
        }

        // Step 2: Compute inSampleSize so longest side is ~1600 px
        val longestSide = max(origWidth, origHeight)
        var sampleSize = 1
        if (longestSide > 1600) {
            sampleSize = Math.round(longestSide.toFloat() / 1600f)
        }
        if (sampleSize < 1) sampleSize = 1

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        // Step 3: Decode downscaled bitmap
        var bitmap: Bitmap? = null
        try {
            inputStream = contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot re-open stream for URI: $uri")
            bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                ?: throw IllegalStateException("BitmapFactory failed to decode bitmap for: $uri")

            // Step 4: Run ML Kit OCR
            val image = InputImage.fromBitmap(bitmap, 0)
            val task = recognizer.process(image)
            // Wait up to 25 seconds per image to avoid any freeze
            val visionText = Tasks.await(task, 25, TimeUnit.SECONDS)

            OcrResult(
                text = visionText.text.trim(),
                width = origWidth,
                height = origHeight
            )
        } finally {
            inputStream?.close()
            // Step 5: Immediately recycle bitmap to keep memory flat
            bitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
        }
    }
}
