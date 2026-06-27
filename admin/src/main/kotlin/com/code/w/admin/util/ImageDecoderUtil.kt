package com.code.w.admin.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

object ImageDecoderUtil {
    /**
     * فك تشفير سلسلة Base64 النصية وتحويلها إلى كائن Bitmap صالح للعرض الرسومي
     */
    fun decodeBase64ToBitmap(base64String: String?): Bitmap? {
        if (base64String.isNullOrBlank()) return null
        return try {
            val cleanedString = base64String.substringAfter(",") // إزالة الملحقات البرمجية إن وجدت
            val decodedBytes = Base64.decode(cleanedString, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
