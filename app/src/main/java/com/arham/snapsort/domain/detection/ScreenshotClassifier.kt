package com.arham.snapsort.domain.detection

import java.util.Locale

/**
 * Result of rule-based classification on a screenshot.
 */
data class ClassificationResult(
    val detectedApp: String,
    val category: AppCategory,
    val confidence: Float,
    val chatName: String?,
    val classificationStatus: String, // "classified", "unclassified", "unsupported_language_or_no_text"
    val matchedKeywords: List<String>
)

object ScreenshotClassifier {

    const val STATUS_CLASSIFIED = "classified"
    const val STATUS_UNCLASSIFIED = "unclassified"
    const val STATUS_UNSUPPORTED_LANGUAGE = "unsupported_language_or_no_text"

    /**
     * Classifies screenshot based on OCR text evidence and available metadata.
     * Explainable and completely offline.
     */
    fun classify(
        ocrText: String,
        displayName: String = "",
        relativePath: String = ""
    ): ClassificationResult {
        val trimmed = ocrText.trim()
        val metaString = "${displayName.lowercase(Locale.ROOT)} ${relativePath.lowercase(Locale.ROOT)}"

        // Check metadata first (package name hints in filename or folder path)
        var metadataRule: AppRule? = null
        for (rule in AppDetectionRules.RULES) {
            for (hint in rule.packageHints) {
                if (hint.isNotBlank() && metaString.contains(hint.lowercase(Locale.ROOT))) {
                    metadataRule = rule
                    break
                }
            }
            if (metadataRule != null) break
        }

        // Handle empty or very short text (e.g. Urdu, Arabic, graphics, memes)
        if (trimmed.length < 5) {
            return if (metadataRule != null) {
                // Known app from metadata, but text is empty / non-Latin
                ClassificationResult(
                    detectedApp = metadataRule.appName,
                    category = metadataRule.category,
                    confidence = 4.0f,
                    chatName = null,
                    classificationStatus = STATUS_UNSUPPORTED_LANGUAGE,
                    matchedKeywords = listOf("metadata:${metadataRule.appName}")
                )
            } else {
                ClassificationResult(
                    detectedApp = "Other",
                    category = AppCategory.OTHER,
                    confidence = 0f,
                    chatName = null,
                    classificationStatus = STATUS_UNSUPPORTED_LANGUAGE,
                    matchedKeywords = emptyList()
                )
            }
        }

        val lowerText = trimmed.lowercase(Locale.ROOT)

        var bestRule: AppRule? = null
        var highestScore = 0f
        var bestMatchedKeywords = emptyList<String>()

        for (rule in AppDetectionRules.RULES) {
            var score = 0f
            val matched = mutableListOf<String>()

            // Check metadata hints for this rule
            for (hint in rule.packageHints) {
                if (hint.isNotBlank() && metaString.contains(hint.lowercase(Locale.ROOT))) {
                    score += 4.0f
                    matched.add("metadata:$hint")
                    break
                }
            }

            // Check OCR text keywords
            for (kw in rule.keywords) {
                val found = if (kw.isRegex) {
                    val regex = Regex("""(?i)${kw.keyword}""")
                    regex.containsMatchIn(lowerText)
                } else if (kw.exactWordMatch) {
                    val regex = Regex("""(?i)\b${Regex.escape(kw.keyword)}\b""")
                    regex.containsMatchIn(lowerText)
                } else {
                    lowerText.contains(kw.keyword.lowercase(Locale.ROOT))
                }

                if (found) {
                    score += kw.weight
                    matched.add(kw.keyword)
                }
            }

            if (score > highestScore) {
                highestScore = score
                bestRule = rule
                bestMatchedKeywords = matched
            }
        }

        // Check if highest score meets confidence threshold
        if (bestRule != null && highestScore >= bestRule.minimumConfidence) {
            val chatName = if (bestRule.isChatApp) {
                ChatNameExtractor.extractChatName(ocrText, isChatApp = true)
            } else {
                null
            }

            return ClassificationResult(
                detectedApp = bestRule.appName,
                category = bestRule.category,
                confidence = highestScore,
                chatName = chatName,
                classificationStatus = STATUS_CLASSIFIED,
                matchedKeywords = bestMatchedKeywords
            )
        }

        // Fallback to Unclassified / Other
        return ClassificationResult(
            detectedApp = "Other",
            category = AppCategory.OTHER,
            confidence = highestScore,
            chatName = null,
            classificationStatus = STATUS_UNCLASSIFIED,
            matchedKeywords = bestMatchedKeywords
        )
    }
}
