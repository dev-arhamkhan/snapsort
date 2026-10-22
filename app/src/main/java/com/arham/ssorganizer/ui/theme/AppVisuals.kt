package com.arham.ssorganizer.ui.theme

import androidx.compose.ui.graphics.Color

object AppVisuals {

    fun getAppColor(appName: String): Color {
        return when (appName.lowercase()) {
            "whatsapp" -> Color(0xFF25D366)
            "youtube" -> Color(0xFFE53935)
            "instagram" -> Color(0xFFE1306C)
            "telegram" -> Color(0xFF0088CC)
            "messenger" -> Color(0xFF0084FF)
            "sms / messages", "messages", "sms" -> Color(0xFF1E88E5)
            "snapchat" -> Color(0xFFE6AF00)
            "x / twitter", "twitter", "x" -> Color(0xFF212121)
            "careem" -> Color(0xFF00A368)
            "indrive" -> Color(0xFF00897B)
            "uber" -> Color(0xFF212121)
            "easypaisa" -> Color(0xFF00A651)
            "jazzcash" -> Color(0xFFFF6600)
            "banking" -> Color(0xFF3F51B5)
            "chrome" -> Color(0xFF4285F4)
            "settings" -> Color(0xFF607D8B)
            "other" -> Color(0xFF78909C)
            else -> Color(0xFF5C6BC0)
        }
    }

    fun getAppBadgeText(appName: String): String {
        return when (appName.lowercase()) {
            "whatsapp" -> "WA"
            "youtube" -> "YT"
            "instagram" -> "IG"
            "telegram" -> "TG"
            "messenger" -> "FB"
            "sms / messages", "messages", "sms" -> "SMS"
            "snapchat" -> "SC"
            "x / twitter", "twitter", "x" -> "X"
            "indrive" -> "IN"
            "careem" -> "CR"
            "uber" -> "UB"
            "easypaisa" -> "EP"
            "jazzcash" -> "JC"
            "banking" -> "BK"
            "chrome" -> "WEB"
            "settings" -> "SET"
            "other" -> "OTH"
            else -> appName.take(2).uppercase()
        }
    }
}
