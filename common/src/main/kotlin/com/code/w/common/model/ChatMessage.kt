package com.code.w.common.model

data class ChatMessage(
    val id: String = "",
    val sender: String = SENDER_USER, // "USER" أو "ADMIN"
    val text: String = "",
    val imageData: String = "",       // رابط الصورة أو Base64 المضغوط
    val timestamp: Long = 0L,
    val isRead: Boolean = false
) {
    companion object {
        const val SENDER_USER = "USER"
        const val SENDER_ADMIN = "ADMIN"
    }
}
