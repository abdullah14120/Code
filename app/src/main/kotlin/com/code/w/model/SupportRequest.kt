package com.code.w.model

data class SupportRequest(
    val id: String = "",
    val phoneNumber: String = "",
    val issueType: String = "",
    val status: String = "SUBMITTED",
    val adminNotes: String = "",
    val bankDetails: String = "",
    val receiptImageUrl: String = ""
) {
    // كائن يمثل الحالات المختلفة للطلب لمنع الأخطاء الإملائية أثناء الفحص
    object Status {
        const val SUBMITTED = "SUBMITTED"
        const val APPROVED = "APPROVED"
        const val RECEIPT_SUBMITTED = "RECEIPT_SUBMITTED"
        const val COMPLETED = "COMPLETED"
    }
}
