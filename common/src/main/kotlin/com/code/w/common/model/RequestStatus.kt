package com.code.w.common.model

enum class RequestStatus(val arabicLabel: String) {
    PENDING_UPLOAD("بانتظار توفر الإنترنت للرفع"),
    SUBMITTED("طلب جديد - بانتظار المراجعة"),
    PRE_APPROVED("مقبول مبدئياً - جاري عداد المراجعة"),
    APPROVED("بانتظار الإيداع من المستخدم"),
    RECEIPT_SUBMITTED("تم إرفاق إشعار الإيداع"),
    PROCESSING("قيد المعالجة"),
    COMPLETED("تم حل المشكلة بنجاح"),
    REJECTED("مرفوض");

    fun isActive(): Boolean = this != COMPLETED && this != REJECTED

    fun canTransitionTo(next: RequestStatus): Boolean {
        if (this == next) return false
        return when (this) {
            PENDING_UPLOAD -> next in setOf(SUBMITTED, RECEIPT_SUBMITTED, REJECTED)
            SUBMITTED -> next in setOf(PRE_APPROVED, APPROVED, REJECTED)
            PRE_APPROVED -> next in setOf(APPROVED, PROCESSING, REJECTED)
            APPROVED -> next in setOf(RECEIPT_SUBMITTED, PROCESSING, REJECTED)
            RECEIPT_SUBMITTED -> next in setOf(PROCESSING, APPROVED, COMPLETED, REJECTED)
            PROCESSING -> next in setOf(COMPLETED, REJECTED)
            COMPLETED, REJECTED -> false
        }
    }

    companion object {
        /**
         * تُستخدم للتحقق الصارم من اسم الحالة ورمي استثناء عند وجود خطأ إملائي
         */
        fun fromString(value: String?): RequestStatus {
            if (value.isNullOrBlank()) return SUBMITTED
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown RequestStatus: $value")
        }

        /**
         * تُستخدم للقراءة الآمنة مع قيمة افتراضية
         */
        fun fromStringOrDefault(value: String?, default: RequestStatus = SUBMITTED): RequestStatus {
            return entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: default
        }
    }
}
