package com.code.w.admin.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

object ImageDecoderUtil {
    /**
     * فك تشفير سلسلة Base64 النصية وتحويلها إلى كائن Bitmap صالح للعرض الرسومي
     * يدعم كلاً من الصور المباشرة وملفات الـ PDF الخام في حال وجودها
     */
    fun decodeBase64ToBitmap(base64String: String?, context: Context? = null): Bitmap? {
        if (base64String.isNullOrBlank()) return null
        return try {
            val cleanedString = base64String.substringAfter(",")
            val decodedBytes = Base64.decode(cleanedString, Base64.DEFAULT)

            // التحقق مما إذا كانت البايتات تمثل ملف PDF خام يبدأ بـ %PDF
            if (decodedBytes.size > 4 &&
                decodedBytes[0] == 0x25.toByte() && // %
                decodedBytes[1] == 0x50.toByte() && // P
                decodedBytes[2] == 0x44.toByte() && // D
                decodedBytes[3] == 0x46.toByte()    // F
            ) {
                return renderRawPdfBytes(decodedBytes, context)
            }

            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun renderRawPdfBytes(pdfBytes: ByteArray, context: Context?): Bitmap? {
        val tempFile = if (context != null) {
            File(context.cacheDir, "admin_pdf_${System.currentTimeMillis()}.pdf")
        } else {
            File.createTempFile("admin_pdf_", ".pdf")
        }
        return try {
            FileOutputStream(tempFile).use { it.write(pdfBytes) }
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount <= 0) {
                renderer.close()
                pfd.close()
                return null
            }
            val page = renderer.openPage(0)
            val width = 1080
            val height = (width * (page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f))).toInt().coerceIn(400, 2400)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }
}
