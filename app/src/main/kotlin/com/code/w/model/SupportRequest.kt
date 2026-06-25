package com.code.w.model

data class SupportRequest(
    val id: String = "",
    val phoneNumber: String = "",
    val issueType: String = "",
    val status: Status = Status.SUBMITTED,
    val bankDetails: String = "",
    val adminNotes: String = "",
    val receiptImageUrl: String = "",
    val timerEndTime: Long = 0L // حقل الطابع الزمني لانتهاء العداد
) {
    enum Status {
        SUBMITTED,          // جاري مراجعة الطلب
        PRE_APPROVED,       // موافقة مبدئية (شاشة العداد التنازلي)
        APPROVED,           // تمت الموافقة وطلب الإيداع
        RECEIPT_SUBMITTED,  // تم رفع الإيصال
        COMPLETED           // مكتمل
    }
}
