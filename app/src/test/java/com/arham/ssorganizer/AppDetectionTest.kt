package com.arham.ssorganizer

import com.arham.ssorganizer.domain.detection.AppCategory
import com.arham.ssorganizer.domain.detection.ChatNameExtractor
import com.arham.ssorganizer.domain.detection.ScreenshotClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDetectionTest {

    // --- YOUTUBE PATTERN TESTS (Targeted Investigation) ---

    @Test
    fun testYouTubeHomeFeed() {
        val sampleOcr = """
            10:15
            Python Full Course for Beginners
            TechWorld
            450K views • 3 weeks ago
            Home
            Shorts
            Subscriptions
            You
        """.trimIndent()

        val result = ScreenshotClassifier.classify(sampleOcr)
        assertEquals("YouTube", result.detectedApp)
        assertEquals(AppCategory.MEDIA, result.category)
        assertEquals(ScreenshotClassifier.STATUS_CLASSIFIED, result.classificationStatus)
        assertNull(result.chatName)
    }

    @Test
    fun testYouTubeShortsFeed() {
        val sampleOcr = """
            Shorts
            @crazytech
            Life hacks that actually work!
            Dislike
            Remix
            Sound: original audio
        """.trimIndent()

        val result = ScreenshotClassifier.classify(sampleOcr)
        assertEquals("YouTube", result.detectedApp)
        assertEquals(AppCategory.MEDIA, result.category)
        assertEquals(ScreenshotClassifier.STATUS_CLASSIFIED, result.classificationStatus)
        assertNull(result.chatName)
    }

    @Test
    fun testYouTubeVideoPlayback() {
        val sampleOcr = """
            Building Offline Android Apps with Room & Jetpack Compose
            Android Developers
            1.2M subscribers
            145K views • 2 days ago
            Subscribed
            Dislike
            Download
            Remix
            Comments 420
        """.trimIndent()

        val result = ScreenshotClassifier.classify(sampleOcr)
        assertEquals("YouTube", result.detectedApp)
        assertEquals(AppCategory.MEDIA, result.category)
        assertEquals(ScreenshotClassifier.STATUS_CLASSIFIED, result.classificationStatus)
        assertNull(result.chatName)
    }

    @Test
    fun testYouTubeSearchResults() {
        val sampleOcr = """
            Search YouTube
            kotlin coroutines tutorial
            All
            Shorts
            Videos
            Kotlin Coroutines in 20 Minutes
            Philipp Lackner
            210K views • 1 year ago
            Shorts
        """.trimIndent()

        val result = ScreenshotClassifier.classify(sampleOcr)
        assertEquals("YouTube", result.detectedApp)
        assertEquals(AppCategory.MEDIA, result.category)
        assertEquals(ScreenshotClassifier.STATUS_CLASSIFIED, result.classificationStatus)
        assertNull(result.chatName)
    }

    @Test
    fun testYouTubeChannelPage() {
        val sampleOcr = """
            Marques Brownlee
            @mkbhd
            18.5M subscribers • 1.6K videos
            Quality tech videos | YouTuber | Geek
            Subscribe
            Home
            Videos
            Shorts
            Playlists
            Community
        """.trimIndent()

        val result = ScreenshotClassifier.classify(sampleOcr)
        assertEquals("YouTube", result.detectedApp)
        assertEquals(AppCategory.MEDIA, result.category)
        assertEquals(ScreenshotClassifier.STATUS_CLASSIFIED, result.classificationStatus)
        assertNull(result.chatName)
    }

    @Test
    fun testYouTubeSubscriptionsTab() {
        val sampleOcr = """
            Subscriptions
            All
            Today
            Continue watching
            Unwatched
            Reviewing the Redmi 8 in 2026!
            Gadget Reviews
            89K views • 5 hours ago
            Home
            Subscriptions
        """.trimIndent()

        val result = ScreenshotClassifier.classify(sampleOcr)
        assertEquals("YouTube", result.detectedApp)
        assertEquals(AppCategory.MEDIA, result.category)
        assertEquals(ScreenshotClassifier.STATUS_CLASSIFIED, result.classificationStatus)
        assertNull(result.chatName)
    }

    @Test
    fun testYouTubeCommentsSection() {
        val sampleOcr = """
            Comments 2,845
            Top comments
            Add a comment...
            Pinned by TechLead
            Great explanation, this helped a lot with Room DB!
            45 replies
        """.trimIndent()

        val result = ScreenshotClassifier.classify(sampleOcr)
        assertEquals("YouTube", result.detectedApp)
        assertEquals(AppCategory.MEDIA, result.category)
        assertEquals(ScreenshotClassifier.STATUS_CLASSIFIED, result.classificationStatus)
        assertNull(result.chatName)
    }

    @Test
    fun testYouTubeSparseOcrVideoScreen() {
        // Very little text: just a video title and view count with relative date
        val sparseVideo = """
            Grand Theft Auto VI Trailer 1
            180M views 4 months ago
        """.trimIndent()

        val result = ScreenshotClassifier.classify(sparseVideo)
        assertEquals("YouTube", result.detectedApp)
        assertEquals(AppCategory.MEDIA, result.category)
        assertEquals(ScreenshotClassifier.STATUS_CLASSIFIED, result.classificationStatus)
    }

    @Test
    fun testYouTubeSparseOcrShortsScreen() {
        // Very little text: just video title and dislike button
        val sparseShorts = """
            Amazing sunset in northern Pakistan
            Shorts
            Dislike
        """.trimIndent()

        val result = ScreenshotClassifier.classify(sparseShorts)
        assertEquals("YouTube", result.detectedApp)
        assertEquals(AppCategory.MEDIA, result.category)
        assertEquals(ScreenshotClassifier.STATUS_CLASSIFIED, result.classificationStatus)
    }

    // --- NEGATIVE TESTS (Preventing False Positives) ---

    @Test
    fun testNegativeWhatsAppChatSharingYouTubeLink() {
        // A WhatsApp chat where someone shares a YouTube video link
        // Must be classified as WhatsApp, NOT YouTube!
        val chatSharingLink = """
            10:45 AM
            Hamza Farooq
            online
            Check out this video:
            https://www.youtube.com/watch?v=dQw4w9WgXcQ
            Type a message
        """.trimIndent()

        val result = ScreenshotClassifier.classify(chatSharingLink)
        assertEquals("WhatsApp", result.detectedApp)
        assertEquals(AppCategory.CHAT, result.category)
        assertEquals("Hamza Farooq", result.chatName)
    }

    @Test
    fun testNegativeInstagramReelNotClassifiedAsYouTube() {
        // An Instagram Reel with comments and likes - must NOT be confused with YouTube
        val instaReel = """
            Instagram
            reels
            creative_art
            Liked by explore_pakistan and 14,200 others
            View all 145 comments
            Send message
        """.trimIndent()

        val result = ScreenshotClassifier.classify(instaReel)
        assertEquals("Instagram", result.detectedApp)
        assertEquals(AppCategory.SOCIAL, result.category)
        assertNotEquals("YouTube", result.detectedApp)
    }

    @Test
    fun testNegativeArticleWithWordViewsNotClassifiedAsYouTube() {
        // An article using the word "views" (opinion) and "yesterday"
        // Must NOT match YouTube's numeric view-count pattern!
        val newsArticle = """
            Editorial: Current Perspectives
            The committee expressed diverging views regarding urban planning.
            Published yesterday in the morning edition.
            Economic growth remains steady.
        """.trimIndent()

        val result = ScreenshotClassifier.classify(newsArticle)
        assertNotEquals("YouTube", result.detectedApp)
        assertEquals("Other", result.detectedApp)
        assertEquals(ScreenshotClassifier.STATUS_UNCLASSIFIED, result.classificationStatus)
    }

    // --- OTHER APPS AND RIDE SHARING TESTS ---

    @Test
    fun testWhatsAppScrolledChatDetection() {
        val sampleOcr = """
            11:30 AM
            Ali Raza
            Forwarded
            Please review the attached invoice.
            This message was deleted
            11:31 AM
        """.trimIndent()

        val result = ScreenshotClassifier.classify(sampleOcr)
        assertEquals("WhatsApp", result.detectedApp)
        assertEquals(AppCategory.CHAT, result.category)
        assertEquals("Ali Raza", result.chatName)
    }

    @Test
    fun testCareemRideDetection() {
        val sampleOcr = """
            Careem
            Looking for captain...
            Go Mini
            Careem Pay
            Ride details
        """.trimIndent()

        val result = ScreenshotClassifier.classify(sampleOcr)
        assertEquals("Careem", result.detectedApp)
        assertEquals(AppCategory.TRANSPORT, result.category)
    }

    @Test
    fun testInDriveRideDetection() {
        val sampleOcr = """
            inDrive
            Set your fare
            Offer your fare: Rs. 450
            Driver's offer: Rs. 500
            Accept offer
        """.trimIndent()

        val result = ScreenshotClassifier.classify(sampleOcr)
        assertEquals("inDrive", result.detectedApp)
        assertEquals(AppCategory.TRANSPORT, result.category)
    }

    @Test
    fun testAmbiguousRideWordFalsePositiveRejection() {
        val genericChat = """
            I went for a bike ride this morning with my friend.
            The driver of the truck was driving very fast.
            We made a cash payment at the location.
        """.trimIndent()

        val result = ScreenshotClassifier.classify(genericChat)
        assertNotEquals("Careem", result.detectedApp)
        assertNotEquals("inDrive", result.detectedApp)
        assertEquals("Other", result.detectedApp)
    }

    @Test
    fun testChatNameFiltersOutStatusBarAndBoilerplate() {
        val noiseChat = """
            4G
            11:20 AM
            85%
            WhatsApp
            Back
            Zubair Ahmed
            online
            Call
            Video
            Type a message
        """.trimIndent()

        val chatName = ChatNameExtractor.extractChatName(noiseChat, isChatApp = true)
        assertEquals("Zubair Ahmed", chatName)
    }
}
