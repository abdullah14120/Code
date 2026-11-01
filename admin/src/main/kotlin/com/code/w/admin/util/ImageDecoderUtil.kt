package com.code.w.admin.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
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
            val height = (width * (page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)))
                .toInt()
                .coerceIn(400, 2400)
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

data class OcrExtractionResult(
    val rawText: String,
    val suggestedAmount: String?,
    val suggestedReferenceNumber: String?
)

object ReceiptOcrHelper {
    private val AMOUNT_REGEX = Regex(
        """(?:Amount|المبلغ|مبلغ|ريال|YER|SAR)[\s:]*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val REF_NUMBER_REGEX = Regex(
        """(?:Ref|Reference|رقم العملية|رقم السند|المرجع|اشعار|إشعار)[\s:#-]*([0-9A-Z]{6,16})""",
        RegexOption.IGNORE_CASE
    )
    private val FALLBACK_LONG_DIGITS = Regex("""\b([0-9]{8,15})\b""")

    suspend fun extractReceiptInfo(bitmap: Bitmap): OcrExtractionResult {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizer.process(image).await()
        val fullText = visionText.text

        val amountMatch = AMOUNT_REGEX.find(fullText)?.groupValues?.getOrNull(1)
        val refMatch = REF_NUMBER_REGEX.find(fullText)?.groupValues?.getOrNull(1)
            ?: FALLBACK_LONG_DIGITS.find(fullText)?.groupValues?.getOrNull(1)

        return OcrExtractionResult(
            rawText = fullText,
            suggestedAmount = amountMatch,
            suggestedReferenceNumber = refMatch
        )
    }
}
