package com.code.w.model

data class SupportRequest(
    val id: String = "",
    val phoneNumber: String = "",
    val issueType: String = "",
    val status: Status = Status.SUBMITTED,
    val bankDetails: String = "",
    val adminNotes: String = "",
    val receiptImageUrl: String = "",
    val timerEndTime: Long = 0L
) {
    // تم تصحيح الخطأ بإضافة كلمة class هنا
    enum class Status {
        SUBMITTED,          
        PRE_APPROVED,       
        APPROVED,           
        RECEIPT_SUBMITTED,  
        COMPLETED           
    }
}
