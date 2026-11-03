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
    val targetAppName: String = "Red",
    val targetAppPackage: String = "com.target.app",
    val activationZipUrl: String = "",
    val timerEndTime: Long = 0L,
    val timerTotalSeconds: Long = 1800L,
    val createdAt: Long = 0L
) {
    enum class Status {
        SUBMITTED,          // في انتظار موافقة أو رفض الأدمن
        REJECTED,           // تم رفض الطلب
        PRE_APPROVED,       // تم قبول الطلب (أسئلة التفعيل أو عداد المراجعة)
        APPROVED,           // واجهة الإيداع أو جاهزية ملف الـ ZIP للتفعيل
        RECEIPT_SUBMITTED,  // تم إرفاق السند
        PROCESSING,         // جاري المعالجة / انتظار إرسال ملف الـ ZIP من الأدمن
        COMPLETED           // تم الحل بنجاح
    }

    companion object {
        const val DEFAULT_ISSUE_TYPE = "مشكلة كود التحقق أو مكالمة فائتة"
    }
}
