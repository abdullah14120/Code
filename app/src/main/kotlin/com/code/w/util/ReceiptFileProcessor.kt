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

data class ProcessedReceipt(
    val previewBitmap: Bitmap,
    val base64Data: String,
    val fileType: String, // "PDF" or "IMAGE"
    val fileName: String
)

object ReceiptFileProcessor {

    fun processUri(context: Context, uri: Uri): ProcessedReceipt? {
        return try {
            val contentResolver = context.contentResolver
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
        val tempFile = File(context.cacheDir, "shared_receipt_${System.currentTimeMillis()}.pdf")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            if (renderer.pageCount <= 0) {
                renderer.close()
                pfd.close()
                return null
            }

            // تحويل الصفحة الأولى (أو صفحتين كحد أقصى) من ملف الـ PDF إلى صورة عالية الوضوح بخلفية بيضاء
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

            val base64 = encodeBitmapToBase64(finalBitmap, quality = 85)
            return ProcessedReceipt(
                previewBitmap = finalBitmap,
                base64Data = base64,
                fileType = "PDF",
                fileName = fileName
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun processImageUri(context: Context, uri: Uri, fileName: String): ProcessedReceipt? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
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

        val base64 = encodeBitmapToBase64(whiteBgBitmap, quality = 82)
        return ProcessedReceipt(
            previewBitmap = whiteBgBitmap,
            base64Data = base64,
            fileType = "IMAGE",
            fileName = fileName
        )
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
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
