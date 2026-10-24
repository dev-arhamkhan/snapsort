package com.arham.snapsort

import com.arham.snapsort.data.local.entity.ScreenshotEntity
import com.arham.snapsort.domain.search.SearchSnippetExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchAndFilterTest {

    // --- SearchSnippetExtractor Tests ---

    @Test
    fun testSnippetExtractionMiddleOfText() {
        val ocr = "Welcome to your bank. Your OTP code for transaction 9876 is 445566. Please do not share it."
        val snippet = SearchSnippetExtractor.extractSnippet(ocr, "445566", maxContextChars = 20)

        assertNotNull(snippet)
        assertTrue("Snippet should contain the matched query", snippet!!.contains("445566"))
        assertTrue("Snippet should start with ellipsis when there is preceding text", snippet.startsWith("..."))
        assertTrue("Snippet should end with ellipsis when there is succeeding text", snippet.endsWith("..."))
    }

    @Test
    fun testSnippetExtractionCaseInsensitive() {
        val ocr = "Thank you for riding with INdrive. Total fare: 450 PKR."
        val snippet = SearchSnippetExtractor.extractSnippet(ocr, "indrive")

        assertNotNull(snippet)
        assertTrue(snippet!!.contains("INdrive"))
    }

    @Test
    fun testSnippetExtractionNormalizesNewlines() {
        val ocr = """
            Transfer Details
            Sender: John
            Receiver: Alice
            Amount: $100
            Status: SUCCESS
        """.trimIndent()

        val snippet = SearchSnippetExtractor.extractSnippet(ocr, "Amount")
        assertNotNull(snippet)
        assertFalse("Snippet should not contain newline characters", snippet!!.contains("\n"))
        assertFalse("Snippet should not contain carriage returns", snippet.contains("\r"))
        assertTrue(snippet.contains("Receiver: Alice Amount: $100"))
    }

    @Test
    fun testSnippetExtractionStartOfText() {
        val ocr = "Receipt #12345 from Green Grocers. Items: milk, bread, eggs."
        val snippet = SearchSnippetExtractor.extractSnippet(ocr, "Receipt", maxContextChars = 15)

        assertNotNull(snippet)
        assertFalse("Snippet should not have leading ellipsis when match is at the very beginning", snippet!!.startsWith("..."))
        assertTrue("Snippet should have trailing ellipsis", snippet.endsWith("..."))
    }

    @Test
    fun testSnippetExtractionEndOfText() {
        val ocr = "Please review our privacy terms at company.com/privacy"
        val snippet = SearchSnippetExtractor.extractSnippet(ocr, "privacy", maxContextChars = 15)

        assertNotNull(snippet)
        assertTrue(snippet!!.contains("privacy"))
    }

    @Test
    fun testSnippetExtractionNotFound() {
        val ocr = "Regular chat message without sensitive data."
        val snippet = SearchSnippetExtractor.extractSnippet(ocr, "passport")
        assertNull(snippet)
    }

    @Test
    fun testSnippetExtractionEmptyQuery() {
        val ocr = "Any text here"
        val snippet = SearchSnippetExtractor.extractSnippet(ocr, "   ")
        assertNull(snippet)
    }

    // --- Search & Filtering Criteria Matching Tests ---

    private fun matchesSearch(
        screenshot: ScreenshotEntity,
        query: String,
        appName: String?,
        chatName: String?
    ): Boolean {
        val q = query.trim().lowercase()

        // Match query against OCR text, displayName, detectedApp, or chatName
        val queryMatches = q.isEmpty() ||
            screenshot.ocrText.lowercase().contains(q) ||
            screenshot.displayName.lowercase().contains(q) ||
            screenshot.detectedApp.lowercase().contains(q) ||
            (screenshot.chatName?.lowercase()?.contains(q) == true)

        // Match app filter
        val appMatches = appName == null || screenshot.detectedApp.equals(appName, ignoreCase = true)

        // Match chat filter
        val chatMatches = when (chatName) {
            null -> true
            "__unspecified__" -> screenshot.chatName.isNullOrBlank()
            else -> screenshot.chatName.equals(chatName, ignoreCase = true)
        }

        return queryMatches && appMatches && chatMatches
    }

    @Test
    fun testFilterByOcrText() {
        val item1 = ScreenshotEntity(
            mediaStoreId = 1,
            uri = "content://1",
            displayName = "Screenshot_1.png",
            relativePath = "Pictures/Screenshots",
            dateAdded = 1000,
            ocrText = "Your verification code is 889900",
            detectedApp = "SMS / Messages"
        )
        val item2 = ScreenshotEntity(
            mediaStoreId = 2,
            uri = "content://2",
            displayName = "Screenshot_2.png",
            relativePath = "Pictures/Screenshots",
            dateAdded = 1001,
            ocrText = "Let's meet at 5pm",
            detectedApp = "WhatsApp"
        )

        assertTrue(matchesSearch(item1, "verification code", null, null))
        assertFalse(matchesSearch(item2, "verification code", null, null))
    }

    @Test
    fun testFilterCombinedWithApp() {
        val item1 = ScreenshotEntity(
            mediaStoreId = 1,
            uri = "content://1",
            displayName = "Screenshot_1.png",
            relativePath = "Pictures/Screenshots",
            dateAdded = 1000,
            ocrText = "Payment received $50",
            detectedApp = "EasyPaisa"
        )
        val item2 = ScreenshotEntity(
            mediaStoreId = 2,
            uri = "content://2",
            displayName = "Screenshot_2.png",
            relativePath = "Pictures/Screenshots",
            dateAdded = 1001,
            ocrText = "Payment received $50",
            detectedApp = "JazzCash"
        )

        // Query matches both, but app filter narrows down to EasyPaisa
        assertTrue(matchesSearch(item1, "Payment", "EasyPaisa", null))
        assertFalse(matchesSearch(item2, "Payment", "EasyPaisa", null))
    }

    @Test
    fun testFilterCombinedWithChatName() {
        val item1 = ScreenshotEntity(
            mediaStoreId = 1,
            uri = "content://1",
            displayName = "Screenshot_1.png",
            relativePath = "Pictures/Screenshots",
            dateAdded = 1000,
            ocrText = "Project meeting at 3",
            detectedApp = "WhatsApp",
            chatName = "Work Group"
        )
        val item2 = ScreenshotEntity(
            mediaStoreId = 2,
            uri = "content://2",
            displayName = "Screenshot_2.png",
            relativePath = "Pictures/Screenshots",
            dateAdded = 1001,
            ocrText = "Project meeting at 3",
            detectedApp = "WhatsApp",
            chatName = "Alice"
        )

        assertTrue(matchesSearch(item1, "Project", "WhatsApp", "Work Group"))
        assertFalse(matchesSearch(item2, "Project", "WhatsApp", "Work Group"))
    }

    @Test
    fun testFilterUnspecifiedChat() {
        val withChat = ScreenshotEntity(
            mediaStoreId = 1,
            uri = "content://1",
            displayName = "Screenshot_1.png",
            relativePath = "Pictures/Screenshots",
            dateAdded = 1000,
            ocrText = "Hey there",
            detectedApp = "WhatsApp",
            chatName = "Bob"
        )
        val withoutChat = ScreenshotEntity(
            mediaStoreId = 2,
            uri = "content://2",
            displayName = "Screenshot_2.png",
            relativePath = "Pictures/Screenshots",
            dateAdded = 1001,
            ocrText = "General screenshot",
            detectedApp = "WhatsApp",
            chatName = null
        )

        assertTrue(matchesSearch(withoutChat, "", "WhatsApp", "__unspecified__"))
        assertFalse(matchesSearch(withChat, "", "WhatsApp", "__unspecified__"))
    }

    @Test
    fun testClearSearchAndFiltersRestoresAll() {
        val items = listOf(
            ScreenshotEntity(mediaStoreId = 1, uri = "content://1", displayName = "1.png", relativePath = "", dateAdded = 1, detectedApp = "YouTube"),
            ScreenshotEntity(mediaStoreId = 2, uri = "content://2", displayName = "2.png", relativePath = "", dateAdded = 2, detectedApp = "WhatsApp"),
            ScreenshotEntity(mediaStoreId = 3, uri = "content://3", displayName = "3.png", relativePath = "", dateAdded = 3, detectedApp = "inDrive")
        )

        // When query is "" and filters are null, all items match
        val matched = items.filter { matchesSearch(it, "", null, null) }
        assertEquals(3, matched.size)
    }
}
