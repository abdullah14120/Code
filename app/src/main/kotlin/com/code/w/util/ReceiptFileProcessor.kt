package com.code.w.util

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

data class ProcessedReceipt(
    val previewBitmap: Bitmap,
    val base64Data: String,
    val fileType: String, // "PDF" or "IMAGE"
    val fileName: String,
    val localFilePath: String = "",
    val mimeType: String = "image/jpeg",
    val sha256Hash: String = ""
)

object ReceiptFileProcessor {
    private const val MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024 // 5 MB

    fun processUri(context: Context, uri: Uri): ProcessedReceipt? {
        return try {
            val contentResolver = context.contentResolver
            val fileSize = getFileSize(context, uri)
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return null
            }

            val mimeType = contentResolver.getType(uri) ?: ""
            val fileName = getFileName(context, uri)
            val isPdf = mimeType.contains("pdf", ignoreCase = true) ||
                fileName.endsWith(".pdf", ignoreCase = true)

            if (isPdf) {
                processPdfUri(context, uri, fileName.ifBlank { "إشعار_إيداع.pdf" })
            } else {
                processImageUri(context, uri, fileName.ifBlank { "إشعار_إيداع.jpg" })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun processPdfUri(context: Context, uri: Uri, fileName: String): ProcessedReceipt? {
        val pendingDir = File(context.filesDir, "pending_receipts").apply { mkdirs() }
        val savedFile = File(pendingDir, "receipt_${System.currentTimeMillis()}.pdf")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(savedFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return null

            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
            val pfd = ParcelFileDescriptor.open(savedFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            if (renderer.pageCount <= 0) {
                renderer.close()
                pfd.close()
                savedFile.delete()
                return null
            }

            val pagesToRender = minOf(renderer.pageCount, 2)
            val renderedPages = mutableListOf<Bitmap>()
            val targetWidth = 1080

            for (i in 0 until pagesToRender) {
                val page = renderer.openPage(i)
                val aspectRatio = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                val targetHeight = (targetWidth * aspectRatio).toInt().coerceIn(400, 2400)

                val pageBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(pageBitmap)
                canvas.drawColor(Color.WHITE)
                page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderedPages.add(pageBitmap)
            }

            renderer.close()
            pfd.close()

            val finalBitmap = if (renderedPages.size == 1) {
                renderedPages[0]
            } else {
                val totalHeight = renderedPages.sumOf { it.height }
                val combined = Bitmap.createBitmap(targetWidth, totalHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(combined)
                canvas.drawColor(Color.WHITE)
                var currentTop = 0f
                for (bmp in renderedPages) {
                    canvas.drawBitmap(bmp, 0f, currentTop, null)
                    currentTop += bmp.height
                }
                combined
            }

            // نحفظ نسخة صورة مصغرة أيضاً للرفع السريع والعرض في الأدمن
            val previewImageFile = File(pendingDir, "receipt_img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(previewImageFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            savedFile.delete()

            return ProcessedReceipt(
                previewBitmap = finalBitmap,
                base64Data = encodeBitmapToBase64(finalBitmap, quality = 75),
                fileType = "PDF",
                fileName = fileName,
                localFilePath = previewImageFile.absolutePath,
                mimeType = "image/jpeg",
                sha256Hash = sha256
            )
        } catch (e: Exception) {
            e.printStackTrace()
            savedFile.delete()
            return null
        }
    }

    private fun processImageUri(context: Context, uri: Uri, fileName: String): ProcessedReceipt? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val sha256 = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

        val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val maxDimension = 1280
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scale = if (width > maxDimension || height > maxDimension) {
            maxDimension.toFloat() / maxOf(width, height).toFloat()
        } else {
            1f
        }

        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)

        val scaledBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
        } else {
            originalBitmap
        }

        val whiteBgBitmap = Bitmap.createBitmap(scaledBitmap.width, scaledBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(whiteBgBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(scaledBitmap, 0f, 0f, null)

        val pendingDir = File(context.filesDir, "pending_receipts").apply { mkdirs() }
        val localFile = File(pendingDir, "receipt_${System.currentTimeMillis()}.jpg")
        FileOutputStream(localFile).use { out ->
            whiteBgBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        return ProcessedReceipt(
            previewBitmap = whiteBgBitmap,
            base64Data = encodeBitmapToBase64(whiteBgBitmap, quality = 75),
            fileType = "IMAGE",
            fileName = fileName,
            localFilePath = localFile.absolutePath,
            mimeType = "image/jpeg",
            sha256Hash = sha256
        )
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && !cursor.isNull(idx)) return cursor.getLong(idx)
            }
        }
        return 0L
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = ""
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index) ?: ""
                    }
                }
            }
        }
        if (name.isBlank()) {
            name = uri.lastPathSegment ?: "receipt_file"
        }
        return name
    }
}
