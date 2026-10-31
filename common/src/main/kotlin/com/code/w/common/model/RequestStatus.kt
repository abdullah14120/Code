package com.code.w.common.model

enum class RequestStatus(val arabicLabel: String) {
    PENDING_UPLOAD("بانتظار توفر الإنترنت للرفع"),
    SUBMITTED("تم إرسال الطلب"),
    PRE_APPROVED("موافقة مبدئية - بانتظار الإيداع"),
    APPROVED("تم تأكيد الإيداع"),
    PROCESSING("قيد التنفيذ"),
    COMPLETED("مكتمل"),
    REJECTED("مرفوض");

    /**
     * يحدد ما إذا كان الطلب لا يزال نشطاً لمنع تكرار الطلبات لنفس الرقم
     */
    fun isActive(): Boolean = this != COMPLETED && this != REJECTED

    /**
     * التحقق الصارم من صحة انتقال الحالة لمنع الانتقالات العشوائية
     */
    fun canTransitionTo(next: RequestStatus): Boolean {
        if (this == next) return false
        return when (this) {
            PENDING_UPLOAD -> next == SUBMITTED || next == REJECTED
            SUBMITTED -> next in setOf(PRE_APPROVED, APPROVED, REJECTED)
            PRE_APPROVED -> next in setOf(APPROVED, PROCESSING, REJECTED)
            APPROVED -> next in setOf(PROCESSING, COMPLETED, REJECTED)
            PROCESSING -> next in setOf(COMPLETED, REJECTED)
            COMPLETED, REJECTED -> false // الحالات النهائية لا تنتقل لحالة أخرى
        }
    }

    companion object {
        fun fromString(value: String?): RequestStatus {
            if (value.isNullOrBlank()) return SUBMITTED
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown RequestStatus: $value")
        }

        fun fromStringOrDefault(value: String?, default: RequestStatus = SUBMITTED): RequestStatus {
            return entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: default
        }
    }
}
