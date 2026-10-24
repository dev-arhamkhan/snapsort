package com.arham.snapsort.domain.detection

import java.util.Locale

/**
 * Extracts chat or contact names from chat screenshots based on header layout evidence.
 * Strict filtering prevents timestamps, message bodies, status-bar text, and UI words
 * from being mistaken for contact names.
 */
object ChatNameExtractor {

    // Noise patterns to reject
    private val TIME_REGEX = Regex("""^\d{1,2}:\d{2}(\s*(am|pm|AM|PM))?$""")
    private val STATUS_BAR_REGEX = Regex("""(?i)^\s*(\d{1,2}:\d{2}|\d{1,3}%|4g|5g|lte|volte|wi-fi|kb/s|mb/s)\s*$""")
    private val PURE_DATE_REGEX = Regex("""^\d{1,2}[/-]\d{1,2}([/-]\d{2,4})?$""")

    // Obvious UI words and action headers to ignore
    private val IGNORED_UI_WORDS = setOf(
        "whatsapp", "telegram", "messenger", "messages", "instagram", "snapchat",
        "online", "typing...", "typing", "last seen", "active now", "seen",
        "back", "call", "video", "search", "menu", "more options", "info",
        "group info", "contact info", "disappearing messages", "end-to-end encrypted",
        "type a message", "message...", "send", "status", "chats", "calls", "updates",
        "today", "yesterday", "new chat", "unread messages", "camera", "gallery"
    )

    /**
     * Extracts a contact or group name if there is reliable evidence.
     * Returns null if evidence is weak, absent, or if the app is not a chat app.
     */
    fun extractChatName(
        ocrText: String,
        isChatApp: Boolean
    ): String? {
        if (!isChatApp || ocrText.isBlank()) return null

        val lines = ocrText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return null

        // Consider candidates from the top 10 lines of the recognized text
        val topCandidates = lines.take(10)

        for (rawLine in topCandidates) {
            val line = cleanCandidate(rawLine)
            if (isValidChatName(line)) {
                return line
            }
        }

        return null
    }

    private fun cleanCandidate(text: String): String {
        return text
            .replace(Regex("""^[<>\-•\s]+"""), "") // Remove leading arrows/bullets
            .replace(Regex("""[<>\-•\s]+$"""), "") // Remove trailing arrows/bullets
            .trim()
    }

    private fun isValidChatName(candidate: String): Boolean {
        if (candidate.length < 2 || candidate.length > 40) return false

        val lower = candidate.lowercase(Locale.ROOT)

        // Ignore status bar text
        if (STATUS_BAR_REGEX.matches(lower)) return false
        if (TIME_REGEX.matches(lower)) return false
        if (PURE_DATE_REGEX.matches(lower)) return false

        // Ignore common UI header boilerplate
        if (IGNORED_UI_WORDS.contains(lower)) return false
        if (lower.startsWith("last seen ") || lower.startsWith("typing...")) return false
        if (lower.startsWith("tap here for ")) return false
        if (lower.contains("end-to-end encrypted")) return false

        // If it looks like a message preview (e.g. starts with "You: " or is a complete sentence)
        if (lower.startsWith("you: ") || lower.startsWith("photo") || lower.startsWith("sticker")) return false

        // Must contain at least one letter or be a valid international phone number
        val hasLetter = candidate.any { it.isLetter() }
        val isPhoneNumber = candidate.matches(Regex("""^\+?[\d\s\-()]{7,20}$"""))

        return hasLetter || isPhoneNumber
    }
}
