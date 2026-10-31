package com.code.w.admin.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

data class OcrExtractionResult(
    val rawText: String,
    val suggestedAmount: String?,
    val suggestedReferenceNumber: String?
)

object ReceiptOcrHelper {
    // يلتقط المبالغ المالية مثل 1,500 أو 2500.00
    private val AMOUNT_REGEX = Regex("""(?:Amount|المبلغ|مبلغ|ريال|YER|SAR)[\s:]*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    // يلتقط أرقام العمليات المرجعية المكونة من 6 إلى 16 رقماً
    private val REF_NUMBER_REGEX = Regex("""(?:Ref|Reference|رقم العملية|رقم السند|المرجع|اشعار)[\s:#-]*([0-9A-Z]{6,16})""", RegexOption.IGNORE_CASE)
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
