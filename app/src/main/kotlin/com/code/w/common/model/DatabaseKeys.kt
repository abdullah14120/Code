package com.code.w.common.model

object DatabaseKeys {
    const val NODE_REQUESTS = "requests"
    const val NODE_USERS_FCM = "users_fcm"
    const val SERVER_TIME_OFFSET = ".info/serverTimeOffset"

    // حقول الطلب
    const val FIELD_ID = "id"
    const val FIELD_OWNER_UID = "ownerUid"
    const val FIELD_PHONE_NUMBER = "phoneNumber"
    const val FIELD_STATUS = "status"
    const val FIELD_RECEIPT_URL = "receiptUrl"
    const val FIELD_RECEIPT_STORAGE_PATH = "receiptStoragePath"
    const val FIELD_RECEIPT_SHA256 = "receiptSha256"
    const val FIELD_TIMER_START_TIME = "timerStartTime"
    const val FIELD_TIMER_DURATION_MS = "timerDurationMs"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_UPDATED_AT = "updatedAt"
    const val FIELD_COMPLETED_AT = "completedAt"
    const val FIELD_AUDIT_LOGS = "auditLogs"
    const val FIELD_OCR_AMOUNT = "ocrSuggestedAmount"
    const val FIELD_OCR_REF = "ocrSuggestedRefNumber"
    const val FIELD_IS_DUPLICATE_RECEIPT = "isDuplicateReceipt"

    // مفاتيح Remote Config
    const val RC_BANK_NAME = "deposit_bank_name"
    const val RC_ACCOUNT_NAME = "deposit_account_name"
    const val RC_ACCOUNT_NUMBER = "deposit_account_number"
    const val RC_DEFAULT_TIMER_MINUTES = "default_timer_minutes"
    const val RC_SUPPORT_WHATSAPP = "support_whatsapp_number"
}
