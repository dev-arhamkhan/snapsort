package com.arham.ssorganizer.domain.search

object SearchSnippetExtractor {

    /**
     * Extracts a concise 1-line snippet around the first occurrence of [query] in [ocrText].
     * Normalizes line breaks and whitespace so it displays cleanly in UI cards.
     * Returns null if [query] is empty or not found in [ocrText].
     */
    fun extractSnippet(ocrText: String, query: String, maxContextChars: Int = 30): String? {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty() || ocrText.isBlank()) return null

        val lowerText = ocrText.lowercase()
        val lowerQuery = trimmedQuery.lowercase()
        val index = lowerText.indexOf(lowerQuery)
        if (index < 0) return null

        val start = (index - maxContextChars).coerceAtLeast(0)
        val end = (index + trimmedQuery.length + maxContextChars).coerceAtMost(ocrText.length)

        var snippet = ocrText.substring(start, end)
            .replace("\r\n", " ")
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace("\\s+".toRegex(), " ")
            .trim()

        if (start > 0) {
            snippet = "...$snippet"
        }
        if (end < ocrText.length) {
            snippet = "$snippet..."
        }

        return snippet
    }
}
