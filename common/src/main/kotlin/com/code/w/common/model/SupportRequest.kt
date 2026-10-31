package com.code.w.common.model

data class AuditLogEntry(
    val logId: String = "",
    val adminUid: String = "",
    val adminIdentifier: String = "",
    val fromStatus: String = "",
    val toStatus: String = "",
    val note: String = "",
    val timestamp: Long = 0L
)

data class SupportRequest(
    val id: String = "",
    val ownerUid: String = "",
    val phoneNumber: String = "",
    val status: String = RequestStatus.SUBMITTED.name,
    val receiptUrl: String = "",
    val receiptStoragePath: String = "",
    val receiptSha256: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val completedAt: Long = 0L,
    val timerStartTime: Long = 0L,
    val timerDurationMs: Long = 0L,
    val ocrSuggestedAmount: String = "",
    val ocrSuggestedRefNumber: String = "",
    val isDuplicateReceipt: Boolean = false,
    val auditLogs: Map<String, AuditLogEntry> = emptyMap()
) {
    val statusEnum: RequestStatus
        get() = RequestStatus.fromStringOrDefault(status)
}
