package com.arham.snapsort.domain.detection

/**
 * High-level content category for extensible classification.
 */
enum class AppCategory(val displayName: String) {
    CHAT("Chat & Messaging"),
    SOCIAL("Social Media"),
    FINANCE("Payments & Banking"),
    TRANSPORT("Rides & Travel"),
    MEDIA("Media & Video"),
    UTILITY("Browser & System"),
    OTHER("Other & Unclassified")
}

/**
 * Definition of an app detection rule with explainable keywords, phrases, weights,
 * and optional package name hints for metadata evidence.
 */
data class AppRule(
    val appName: String,
    val category: AppCategory,
    val isChatApp: Boolean,
    val keywords: List<WeightedKeyword>,
    val packageHints: List<String> = emptyList(),
    val minimumConfidence: Float = 2.5f
)

data class WeightedKeyword(
    val keyword: String,
    val weight: Float = 1.0f,
    val exactWordMatch: Boolean = false, // If true, matches \bword\b
    val isRegex: Boolean = false         // If true, matches regex pattern
)

/**
 * Central repository of detection rules for recognizable Android apps.
 * Easily editable and extensible for future categories.
 */
object AppDetectionRules {

    val RULES: List<AppRule> = listOf(
        // WhatsApp
        AppRule(
            appName = "WhatsApp",
            category = AppCategory.CHAT,
            isChatApp = true,
            minimumConfidence = 2.5f,
            packageHints = listOf("com.whatsapp", "whatsapp"),
            keywords = listOf(
                WeightedKeyword("whatsapp", 3.5f),
                WeightedKeyword("type a message", 3.5f),
                WeightedKeyword("message yourself", 3.5f),
                WeightedKeyword("disappearing messages", 3.0f),
                WeightedKeyword("end-to-end encrypted", 3.0f),
                WeightedKeyword("this message was deleted", 3.5f),
                WeightedKeyword("you deleted this message", 3.5f),
                WeightedKeyword("forwarded", 2.0f),
                WeightedKeyword("forwarded many times", 3.0f),
                WeightedKeyword("last seen", 2.5f),
                WeightedKeyword("online", 2.0f, exactWordMatch = true),
                WeightedKeyword("typing...", 2.5f),
                WeightedKeyword("delete for everyone", 2.5f),
                WeightedKeyword("delete for me", 2.0f),
                WeightedKeyword("view once", 2.0f),
                WeightedKeyword("tap here for contact info", 3.0f),
                WeightedKeyword("waiting for this message", 3.0f),
                WeightedKeyword("custom notifications", 2.5f),
                WeightedKeyword("media, links, and docs", 3.0f),
                WeightedKeyword("voice message", 2.0f),
                WeightedKeyword("missed voice call", 3.0f),
                WeightedKeyword("missed video call", 3.0f),
                WeightedKeyword("status", 1.0f, exactWordMatch = true),
                WeightedKeyword("chats", 1.0f, exactWordMatch = true),
                WeightedKeyword("updates", 1.0f, exactWordMatch = true),
                WeightedKeyword("communities", 1.5f, exactWordMatch = true)
            )
        ),

        // Instagram
        AppRule(
            appName = "Instagram",
            category = AppCategory.SOCIAL,
            isChatApp = true,
            minimumConfidence = 2.5f,
            packageHints = listOf("com.instagram.android", "instagram"),
            keywords = listOf(
                WeightedKeyword("instagram", 3.5f),
                WeightedKeyword("message...", 3.0f),
                WeightedKeyword("send message", 2.5f),
                WeightedKeyword("followers", 2.5f),
                WeightedKeyword("following", 2.0f),
                WeightedKeyword("posts", 1.5f, exactWordMatch = true),
                WeightedKeyword("reels", 2.0f, exactWordMatch = true),
                WeightedKeyword("story", 1.5f, exactWordMatch = true),
                WeightedKeyword("stories", 1.5f, exactWordMatch = true),
                WeightedKeyword("liked by", 2.0f),
                WeightedKeyword("view insights", 2.5f),
                WeightedKeyword("edit profile", 2.0f),
                WeightedKeyword("threads", 1.5f, exactWordMatch = true),
                WeightedKeyword("seen", 1.0f, exactWordMatch = true),
                WeightedKeyword("active now", 1.5f),
                WeightedKeyword("add to story", 2.5f),
                WeightedKeyword("close friends", 2.5f)
            )
        ),

        // YouTube
        AppRule(
            appName = "YouTube",
            category = AppCategory.MEDIA,
            isChatApp = false,
            minimumConfidence = 2.5f,
            packageHints = listOf("com.google.android.youtube", "youtube"),
            keywords = listOf(
                WeightedKeyword("youtube", 3.5f),
                WeightedKeyword("search youtube", 3.5f),
                WeightedKeyword("youtube music", 3.5f),
                WeightedKeyword("youtube premium", 3.5f),
                // Specific view count pattern: e.g. "120K views", "1.4M views", "45 views", "1,250 views"
                WeightedKeyword("""\d+[\d,.]*\s*[kmb]?\s+views\b""", 2.5f, isRegex = true),
                // Specific subscriber count pattern: e.g. "1.2M subscribers", "450K subscribers"
                WeightedKeyword("""\d+[\d,.]*\s*[kmb]?\s+subscribers\b""", 3.5f, isRegex = true),
                // Relative time ago pattern: e.g. "3 days ago", "2 months ago", "1 year ago"
                WeightedKeyword("""\b\d+\s*(second|minute|hour|hr|day|week|month|year)s?\s+ago\b""", 1.5f, isRegex = true),
                WeightedKeyword("subscriptions", 3.0f, exactWordMatch = true),
                WeightedKeyword("subscribers", 3.0f, exactWordMatch = true),
                WeightedKeyword("subscribe", 2.5f, exactWordMatch = true),
                WeightedKeyword("subscribed", 2.5f, exactWordMatch = true),
                WeightedKeyword("shorts", 2.5f, exactWordMatch = true),
                WeightedKeyword("dislike", 2.5f, exactWordMatch = true),
                WeightedKeyword("remix", 2.5f, exactWordMatch = true),
                WeightedKeyword("add a comment", 3.0f),
                WeightedKeyword("top comments", 2.5f),
                WeightedKeyword("pinned by", 2.5f),
                WeightedKeyword("up next", 2.5f),
                WeightedKeyword("watch later", 2.5f),
                WeightedKeyword("watching now", 2.5f),
                WeightedKeyword("super thanks", 3.0f),
                WeightedKeyword("live chat", 2.5f),
                WeightedKeyword("top chat", 2.5f),
                WeightedKeyword("playlist", 2.0f),
                WeightedKeyword("playlists", 2.0f),
                WeightedKeyword("premiere", 2.0f),
                WeightedKeyword("streamed", 2.0f),
                WeightedKeyword("autoplay", 2.0f),
                WeightedKeyword("your videos", 2.5f),
                WeightedKeyword("download", 1.5f, exactWordMatch = true),
                WeightedKeyword("comments", 1.5f, exactWordMatch = true)
            )
        ),

        // Careem (Ride-sharing)
        AppRule(
            appName = "Careem",
            category = AppCategory.TRANSPORT,
            isChatApp = false,
            minimumConfidence = 2.5f,
            packageHints = listOf("com.careem", "careem"),
            keywords = listOf(
                WeightedKeyword("careem", 4.0f),
                WeightedKeyword("captain", 3.5f), // Careem's signature driver term
                WeightedKeyword("careem pay", 3.5f),
                WeightedKeyword("careem plus", 3.5f),
                WeightedKeyword("yalla", 2.5f),
                WeightedKeyword("go mini", 3.0f),
                WeightedKeyword("go prime", 3.0f),
                WeightedKeyword("hala taxi", 3.0f),
                WeightedKeyword("looking for captain", 3.5f),
                WeightedKeyword("captain is arriving", 3.5f),
                WeightedKeyword("captain is on the way", 3.5f),
                WeightedKeyword("ride details", 2.5f)
            )
        ),

        // inDrive (Ride-sharing)
        AppRule(
            appName = "inDrive",
            category = AppCategory.TRANSPORT,
            isChatApp = false,
            minimumConfidence = 2.5f,
            packageHints = listOf("sinet.startup.inDriver", "indrive", "indriver"),
            keywords = listOf(
                WeightedKeyword("indrive", 4.0f),
                WeightedKeyword("indriver", 4.0f),
                WeightedKeyword("offer your fare", 3.5f),
                WeightedKeyword("set your fare", 3.5f),
                WeightedKeyword("driver's offer", 3.5f),
                WeightedKeyword("your offer", 3.0f),
                WeightedKeyword("accept offer", 3.0f),
                WeightedKeyword("raise fare", 3.0f),
                WeightedKeyword("find a driver", 3.0f),
                WeightedKeyword("bidding", 2.5f)
            )
        ),

        // Uber / Yango (Ride-sharing)
        AppRule(
            appName = "Uber",
            category = AppCategory.TRANSPORT,
            isChatApp = false,
            minimumConfidence = 2.5f,
            packageHints = listOf("com.ubercab", "uber", "yango"),
            keywords = listOf(
                WeightedKeyword("uber", 3.5f, exactWordMatch = true),
                WeightedKeyword("uberx", 3.5f),
                WeightedKeyword("uber auto", 3.5f),
                WeightedKeyword("uber moto", 3.5f),
                WeightedKeyword("uber go", 3.5f),
                WeightedKeyword("confirm uber", 3.5f),
                WeightedKeyword("uber reserve", 3.5f),
                WeightedKeyword("yango", 4.0f),
                WeightedKeyword("yango ride", 3.5f)
            )
        ),

        // Telegram
        AppRule(
            appName = "Telegram",
            category = AppCategory.CHAT,
            isChatApp = true,
            minimumConfidence = 2.5f,
            packageHints = listOf("org.telegram.messenger", "telegram"),
            keywords = listOf(
                WeightedKeyword("telegram", 3.5f),
                WeightedKeyword("saved messages", 3.5f),
                WeightedKeyword("last seen recently", 3.0f),
                WeightedKeyword("last seen at", 2.5f),
                WeightedKeyword("bot", 2.0f, exactWordMatch = true),
                WeightedKeyword("mute notifications", 2.0f),
                WeightedKeyword("leave channel", 2.5f),
                WeightedKeyword("join channel", 2.5f),
                WeightedKeyword("channel", 1.5f, exactWordMatch = true)
            )
        ),

        // Facebook Messenger
        AppRule(
            appName = "Messenger",
            category = AppCategory.CHAT,
            isChatApp = true,
            minimumConfidence = 2.5f,
            packageHints = listOf("com.facebook.orca", "messenger"),
            keywords = listOf(
                WeightedKeyword("messenger", 3.5f),
                WeightedKeyword("active now", 2.5f),
                WeightedKeyword("wave to", 3.0f),
                WeightedKeyword("you're friends on facebook", 3.5f),
                WeightedKeyword("write a message", 2.0f)
            )
        ),

        // SMS / Google Messages
        AppRule(
            appName = "Messages",
            category = AppCategory.CHAT,
            isChatApp = true,
            minimumConfidence = 2.0f,
            packageHints = listOf("com.google.android.apps.messaging"),
            keywords = listOf(
                WeightedKeyword("text message", 3.0f),
                WeightedKeyword("rcs chat", 3.5f),
                WeightedKeyword("sms", 2.0f, exactWordMatch = true),
                WeightedKeyword("mms", 2.0f, exactWordMatch = true),
                WeightedKeyword("sim 1", 2.0f),
                WeightedKeyword("sim 2", 2.0f)
            )
        ),

        // Snapchat
        AppRule(
            appName = "Snapchat",
            category = AppCategory.CHAT,
            isChatApp = true,
            minimumConfidence = 2.5f,
            packageHints = listOf("com.snapchat.android", "snapchat"),
            keywords = listOf(
                WeightedKeyword("snapchat", 3.5f),
                WeightedKeyword("streak", 2.5f, exactWordMatch = true),
                WeightedKeyword("streaks", 2.5f),
                WeightedKeyword("bitmoji", 3.0f),
                WeightedKeyword("spotlight", 2.0f, exactWordMatch = true),
                WeightedKeyword("send to", 2.0f),
                WeightedKeyword("new snap", 3.0f),
                WeightedKeyword("received", 1.5f, exactWordMatch = true)
            )
        ),

        // X / Twitter
        AppRule(
            appName = "X",
            category = AppCategory.SOCIAL,
            isChatApp = false,
            minimumConfidence = 2.5f,
            packageHints = listOf("com.twitter.android"),
            keywords = listOf(
                WeightedKeyword("retweet", 3.0f),
                WeightedKeyword("repost", 2.5f),
                WeightedKeyword("reposts", 2.5f),
                WeightedKeyword("quote", 1.5f, exactWordMatch = true),
                WeightedKeyword("trends for you", 3.0f),
                WeightedKeyword("what's happening", 3.0f),
                WeightedKeyword("bookmark", 1.5f, exactWordMatch = true),
                WeightedKeyword("bookmarks", 1.5f),
                WeightedKeyword("twitter", 3.0f),
                WeightedKeyword("x.com", 3.0f)
            )
        ),

        // Easypaisa
        AppRule(
            appName = "Easypaisa",
            category = AppCategory.FINANCE,
            isChatApp = false,
            minimumConfidence = 2.5f,
            packageHints = listOf("pk.com.telenor.phoenix", "easypaisa"),
            keywords = listOf(
                WeightedKeyword("easypaisa", 4.0f),
                WeightedKeyword("telenor microfinance", 4.0f),
                WeightedKeyword("send money", 2.5f),
                WeightedKeyword("easyload", 3.5f),
                WeightedKeyword("mobile load", 2.5f),
                WeightedKeyword("bill payment", 2.5f),
                WeightedKeyword("trx id", 3.0f),
                WeightedKeyword("transaction successful", 3.0f),
                WeightedKeyword("fee: rs", 3.0f)
            )
        ),

        // JazzCash
        AppRule(
            appName = "JazzCash",
            category = AppCategory.FINANCE,
            isChatApp = false,
            minimumConfidence = 2.5f,
            packageHints = listOf("com.techlogix.mobilinkcustomer", "jazzcash"),
            keywords = listOf(
                WeightedKeyword("jazzcash", 4.0f),
                WeightedKeyword("mobilink microfinance", 4.0f),
                WeightedKeyword("money transfer", 2.5f),
                WeightedKeyword("mpin", 3.0f),
                WeightedKeyword("tid:", 3.0f)
            )
        ),

        // General Banking / Payments
        AppRule(
            appName = "Banking",
            category = AppCategory.FINANCE,
            isChatApp = false,
            minimumConfidence = 2.5f,
            keywords = listOf(
                WeightedKeyword("account balance", 3.0f),
                WeightedKeyword("available balance", 3.0f),
                WeightedKeyword("fund transfer", 3.0f),
                WeightedKeyword("transfer successful", 3.0f),
                WeightedKeyword("iban", 3.0f, exactWordMatch = true),
                WeightedKeyword("otp", 2.5f, exactWordMatch = true),
                WeightedKeyword("debit card", 2.5f),
                WeightedKeyword("credit card", 2.5f),
                WeightedKeyword("hbl", 3.0f, exactWordMatch = true),
                WeightedKeyword("meezan", 3.0f),
                WeightedKeyword("mcb", 3.0f, exactWordMatch = true),
                WeightedKeyword("ubl", 3.0f, exactWordMatch = true),
                WeightedKeyword("faysal bank", 3.5f),
                WeightedKeyword("nayapay", 4.0f),
                WeightedKeyword("sadapay", 4.0f)
            )
        ),

        // Chrome / Web Browser
        AppRule(
            appName = "Chrome",
            category = AppCategory.UTILITY,
            isChatApp = false,
            minimumConfidence = 2.5f,
            packageHints = listOf("com.android.chrome"),
            keywords = listOf(
                WeightedKeyword("chrome", 3.0f),
                WeightedKeyword("https://", 3.0f),
                WeightedKeyword("http://", 2.5f),
                WeightedKeyword("incognito", 3.0f),
                WeightedKeyword(".com", 1.5f),
                WeightedKeyword(".org", 1.5f),
                WeightedKeyword("search or type url", 3.5f),
                WeightedKeyword("search or type web address", 3.5f)
            )
        ),

        // Settings / System
        AppRule(
            appName = "Settings",
            category = AppCategory.UTILITY,
            isChatApp = false,
            minimumConfidence = 2.5f,
            packageHints = listOf("com.android.settings"),
            keywords = listOf(
                WeightedKeyword("settings", 2.5f),
                WeightedKeyword("wi-fi", 2.0f),
                WeightedKeyword("bluetooth", 2.0f),
                WeightedKeyword("apps & notifications", 3.0f),
                WeightedKeyword("battery saver", 2.5f),
                WeightedKeyword("about phone", 3.0f),
                WeightedKeyword("miui version", 3.5f),
                WeightedKeyword("internal storage", 2.5f),
                WeightedKeyword("system update", 3.0f)
            )
        )
    )

    /** Returns true if the app is configured as a chat/messaging application */
    fun isChatApp(appName: String): Boolean {
        return RULES.find { it.appName.equals(appName, ignoreCase = true) }?.isChatApp == true
    }
}
