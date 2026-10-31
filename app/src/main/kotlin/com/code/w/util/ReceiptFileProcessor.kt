package com.code.w.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

sealed class ReceiptValidationResult {
    data class Valid(
        val localFile: File,
        val mimeType: String,
        val sha256Hash: String,
        val sizeBytes: Long
    ) : ReceiptValidationResult()

    data class Invalid(val errorMessageAr: String) : ReceiptValidationResult()
}

object ReceiptFileProcessor {
    private const val MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024 // 5 MB
    private val ALLOWED_MIME_TYPES = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "application/pdf"
    )

    fun validateAndPrepareReceipt(context: Context, uri: Uri): ReceiptValidationResult {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
            ?: return ReceiptValidationResult.Invalid("تعذر تحديد نوع الملف. يرجى اختيار صورة (JPG/PNG) أو ملف PDF.")

        if (mimeType !in ALLOWED_MIME_TYPES) {
            return ReceiptValidationResult.Invalid("نوع الملف غير مدعوم ($mimeType). الأنواع المسموحة: صور (JPG, PNG, WEBP) أو PDF فقط.")
        }

        val fileSize = getFileSize(context, uri)
        if (fileSize <= 0L) {
            return ReceiptValidationResult.Invalid("الملف المختار فارغ أو تالف.")
        }
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            val sizeMb = String.format("%.2f", fileSize / (1024.0 * 1024.0))
            return ReceiptValidationResult.Invalid("حجم الملف ($sizeMb ميجابايت) يتجاوز الحد الأقصى المسموح به (5 ميجابايت).")
        }

        // نسخ الملف إلى تخزين التطبيق الداخلي وحساب بصمة SHA-256 في نفس الدورة
        val pendingDir = File(context.filesDir, "pending_receipts").apply { mkdirs() }
        val extension = when (mimeType) {
            "application/pdf" -> "pdf"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val localFile = File(pendingDir, "receipt_${System.currentTimeMillis()}.$extension")

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                        output.write(buffer, 0, bytesRead)
                    }
                }
            } ?: return ReceiptValidationResult.Invalid("تعذر قراءة الملف من الجهاز.")

            val sha256Hex = digest.digest().joinToString("") { "%02x".format(it) }
            ReceiptValidationResult.Valid(
                localFile = localFile,
                mimeType = mimeType,
                sha256Hash = sha256Hex,
                sizeBytes = fileSize
            )
        } catch (e: Exception) {
            localFile.delete()
            ReceiptValidationResult.Invalid("حدث خطأ أثناء معالجة الملف: ${e.localizedMessage}")
        }
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex)
                }
            }
        }
        return -1L
    }
}
