package com.code.w.model

data class SupportRequest(
    val id: String = "",
    val phoneNumber: String = "",
    val issueType: String = DEFAULT_ISSUE_TYPE,
    val status: Status = Status.SUBMITTED,
    val bankDetails: String = "",
    val adminNotes: String = "",
    val rejectionReason: String = "",
    val receiptImageUrl: String = "",
    val receiptFileType: String = "",
    val receiptFileName: String = "",
    val timerEndTime: Long = 0L,
    val timerTotalSeconds: Long = 1800L,
    val createdAt: Long = 0L
) {
    enum class Status {
        SUBMITTED,          // في انتظار موافقة أو رفض الأدمن
        REJECTED,           // تم رفض الطلب مع ذكر ملاحظة الرفض
        PRE_APPROVED,       // تم قبول الطلب - جاري مراجعة الطلب مع عداد انتظار توقيت
        APPROVED,           // طلب إيداع المبلغ - واجهة الإيداع باللون الأخضر الرسمي
        RECEIPT_SUBMITTED,  // تم إرفاق ملف PDF/صورة الإيداع - واجهة جاري معالجة المشكلة مع عداد احترافي
        PROCESSING,         // تم تأكيد الإيداع من الأدمن - واجهة جاري معالجة المشكلة مع عداد احترافي
        COMPLETED           // تم حل المشكلة بنجاح + علامة صح + ملاحظات الأدمن + إنهاء الجلسة
    }

    companion object {
        const val DEFAULT_ISSUE_TYPE = "مشكلة كود التحقق أو مكالمة فائتة"
    }
}
