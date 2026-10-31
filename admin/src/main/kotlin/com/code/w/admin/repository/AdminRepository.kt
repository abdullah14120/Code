package com.code.w.admin.repository

import com.code.w.common.model.DatabaseKeys
import com.code.w.common.model.RequestStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class AdminSupportRequest(
    val id: String = "",
    val ownerUid: String = "",
    val phoneNumber: String = "",
    val issueType: String = "مشكلة كود التحقق أو مكالمة فائتة",
    val status: String = RequestStatus.SUBMITTED.name,
    val adminNotes: String = "",
    val rejectionReason: String = "",
    val bankDetails: String = "",
    val receiptImageUrl: String = "",
    val receiptSha256: String = "",
    val isDuplicateReceipt: Boolean = false,
    val receiptFileType: String = "",
    val receiptFileName: String = "",
    val timerEndTime: Long = 0L,
    val timerTotalSeconds: Long = 1800L,
    val createdAt: Long = 0L
)

class AdminRepository {
    private val database = FirebaseDatabase.getInstance().getReference(DatabaseKeys.NODE_REQUESTS)
    private val auth = FirebaseAuth.getInstance()

    fun observeAllRequests(): Flow<List<AdminSupportRequest>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rawList = snapshot.children.mapNotNull { child ->
                    parseAdminRequestSafely(child)
                }
                val hashCounts = rawList
                    .filter { it.receiptSha256.isNotBlank() }
                    .groupingBy { it.receiptSha256 }
                    .eachCount()

                val requests = rawList.map { req ->
                    req.copy(
                        isDuplicateReceipt = req.receiptSha256.isNotBlank() && (hashCounts[req.receiptSha256] ?: 0) > 1
                    )
                }.sortedByDescending { it.createdAt }

                trySend(requests)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        database.addValueEventListener(listener)
        awaitClose { database.removeEventListener(listener) }
    }

    private fun parseAdminRequestSafely(snapshot: DataSnapshot): AdminSupportRequest? {
        val id = snapshot.child(DatabaseKeys.FIELD_ID).getValue(String::class.java) ?: snapshot.key ?: return null
        val phoneNumber = snapshot.child(DatabaseKeys.FIELD_PHONE_NUMBER).getValue(String::class.java).orEmpty()
        if (phoneNumber.isBlank()) return null

        val ownerUid = snapshot.child(DatabaseKeys.FIELD_OWNER_UID).getValue(String::class.java).orEmpty()
        val issueType = snapshot.child(DatabaseKeys.FIELD_ISSUE_TYPE).getValue(String::class.java)
            ?: "مشكلة كود التحقق أو مكالمة فائتة"
        val statusStr = snapshot.child(DatabaseKeys.FIELD_STATUS).getValue(String::class.java)
        val status = RequestStatus.fromStringOrDefault(statusStr).name
        val adminNotes = snapshot.child(DatabaseKeys.FIELD_ADMIN_NOTES).getValue(String::class.java).orEmpty()
        val rejectionReason = snapshot.child(DatabaseKeys.FIELD_REJECTION_REASON).getValue(String::class.java).orEmpty()
        val bankDetails = snapshot.child(DatabaseKeys.FIELD_BANK_DETAILS).getValue(String::class.java).orEmpty()
        val receiptImageUrl = snapshot.child(DatabaseKeys.FIELD_RECEIPT_URL).getValue(String::class.java).orEmpty()
        val receiptSha256 = snapshot.child(DatabaseKeys.FIELD_RECEIPT_SHA256).getValue(String::class.java).orEmpty()
        val receiptFileType = snapshot.child(DatabaseKeys.FIELD_RECEIPT_FILE_TYPE).getValue(String::class.java).orEmpty()
        val receiptFileName = snapshot.child(DatabaseKeys.FIELD_RECEIPT_FILE_NAME).getValue(String::class.java).orEmpty()
        val timerEndTime = snapshot.child(DatabaseKeys.FIELD_TIMER_END_TIME).getValue(Long::class.java) ?: 0L
        val timerTotalSeconds = snapshot.child(DatabaseKeys.FIELD_TIMER_TOTAL_SECONDS).getValue(Long::class.java) ?: 1800L
        val createdAt = snapshot.child(DatabaseKeys.FIELD_CREATED_AT).getValue(Long::class.java) ?: 0L

        return AdminSupportRequest(
            id = id,
            ownerUid = ownerUid,
            phoneNumber = phoneNumber,
            issueType = issueType,
            status = status,
            adminNotes = adminNotes,
            rejectionReason = rejectionReason,
            bankDetails = bankDetails,
            receiptImageUrl = receiptImageUrl,
            receiptSha256 = receiptSha256,
            receiptFileType = receiptFileType,
            receiptFileName = receiptFileName,
            timerEndTime = timerEndTime,
            timerTotalSeconds = timerTotalSeconds,
            createdAt = createdAt
        )
    }

    private fun buildAuditEntry(toStatus: RequestStatus, note: String = ""): Map<String, Any> {
        val adminUid = auth.currentUser?.uid ?: "admin"
        return mapOf(
            "adminUid" to adminUid,
            "toStatus" to toStatus.name,
            "note" to note,
            "timestamp" to ServerValue.TIMESTAMP
        )
    }

    fun preApproveWithTimer(requestId: String, durationMinutes: Int, onResult: (Boolean) -> Unit) {
        val totalSeconds = durationMinutes * 60L
        val endTime = System.currentTimeMillis() + (totalSeconds * 1000L)
        val logId = database.child(requestId).child(DatabaseKeys.FIELD_AUDIT_LOGS).push().key ?: "log_${System.currentTimeMillis()}"
        val updates = mapOf<String, Any>(
            DatabaseKeys.FIELD_STATUS to RequestStatus.PRE_APPROVED.name,
            DatabaseKeys.FIELD_TIMER_START_TIME to ServerValue.TIMESTAMP,
            DatabaseKeys.FIELD_TIMER_END_TIME to endTime,
            DatabaseKeys.FIELD_TIMER_TOTAL_SECONDS to totalSeconds,
            DatabaseKeys.FIELD_UPDATED_AT to ServerValue.TIMESTAMP,
            "${DatabaseKeys.FIELD_AUDIT_LOGS}/$logId" to buildAuditEntry(RequestStatus.PRE_APPROVED)
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }

    fun rejectRequestWithNote(requestId: String, rejectionNote: String, onResult: (Boolean) -> Unit) {
        val logId = database.child(requestId).child(DatabaseKeys.FIELD_AUDIT_LOGS).push().key ?: "log_${System.currentTimeMillis()}"
        val updates = mapOf<String, Any>(
            DatabaseKeys.FIELD_STATUS to RequestStatus.REJECTED.name,
            DatabaseKeys.FIELD_REJECTION_REASON to rejectionNote,
            DatabaseKeys.FIELD_ADMIN_NOTES to rejectionNote,
            DatabaseKeys.FIELD_COMPLETED_AT to ServerValue.TIMESTAMP,
            DatabaseKeys.FIELD_UPDATED_AT to ServerValue.TIMESTAMP,
            "${DatabaseKeys.FIELD_AUDIT_LOGS}/$logId" to buildAuditEntry(RequestStatus.REJECTED, rejectionNote)
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }

    fun approveWithBankDetails(requestId: String, bankDetails: String, onResult: (Boolean) -> Unit) {
        val logId = database.child(requestId).child(DatabaseKeys.FIELD_AUDIT_LOGS).push().key ?: "log_${System.currentTimeMillis()}"
        val updates = mapOf<String, Any>(
            DatabaseKeys.FIELD_BANK_DETAILS to bankDetails,
            DatabaseKeys.FIELD_STATUS to RequestStatus.APPROVED.name,
            DatabaseKeys.FIELD_UPDATED_AT to ServerValue.TIMESTAMP,
            "${DatabaseKeys.FIELD_AUDIT_LOGS}/$logId" to buildAuditEntry(RequestStatus.APPROVED, bankDetails)
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }

    fun confirmDepositAndStartProcessing(requestId: String, durationMinutes: Int, onResult: (Boolean) -> Unit) {
        val totalSeconds = durationMinutes * 60L
        val endTime = System.currentTimeMillis() + (totalSeconds * 1000L)
        val logId = database.child(requestId).child(DatabaseKeys.FIELD_AUDIT_LOGS).push().key ?: "log_${System.currentTimeMillis()}"
        val updates = mapOf<String, Any>(
            DatabaseKeys.FIELD_STATUS to RequestStatus.PROCESSING.name,
            DatabaseKeys.FIELD_TIMER_START_TIME to ServerValue.TIMESTAMP,
            DatabaseKeys.FIELD_TIMER_END_TIME to endTime,
            DatabaseKeys.FIELD_TIMER_TOTAL_SECONDS to totalSeconds,
            DatabaseKeys.FIELD_UPDATED_AT to ServerValue.TIMESTAMP,
            "${DatabaseKeys.FIELD_AUDIT_LOGS}/$logId" to buildAuditEntry(RequestStatus.PROCESSING)
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }

    fun rejectDepositNotConfirmed(requestId: String, note: String, onResult: (Boolean) -> Unit) {
        val logId = database.child(requestId).child(DatabaseKeys.FIELD_AUDIT_LOGS).push().key ?: "log_${System.currentTimeMillis()}"
        val updates = mapOf<String, Any>(
            DatabaseKeys.FIELD_STATUS to RequestStatus.APPROVED.name,
            DatabaseKeys.FIELD_REJECTION_REASON to note,
            DatabaseKeys.FIELD_RECEIPT_URL to "",
            DatabaseKeys.FIELD_RECEIPT_FILE_TYPE to "",
            DatabaseKeys.FIELD_RECEIPT_FILE_NAME to "",
            DatabaseKeys.FIELD_UPDATED_AT to ServerValue.TIMESTAMP,
            "${DatabaseKeys.FIELD_AUDIT_LOGS}/$logId" to buildAuditEntry(RequestStatus.APPROVED, note)
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }

    fun completeRequestWithNotes(requestId: String, notes: String, onResult: (Boolean) -> Unit) {
        val logId = database.child(requestId).child(DatabaseKeys.FIELD_AUDIT_LOGS).push().key ?: "log_${System.currentTimeMillis()}"
        val updates = mapOf<String, Any>(
            DatabaseKeys.FIELD_ADMIN_NOTES to notes,
            DatabaseKeys.FIELD_STATUS to RequestStatus.COMPLETED.name,
            DatabaseKeys.FIELD_COMPLETED_AT to ServerValue.TIMESTAMP,
            DatabaseKeys.FIELD_UPDATED_AT to ServerValue.TIMESTAMP,
            "${DatabaseKeys.FIELD_AUDIT_LOGS}/$logId" to buildAuditEntry(RequestStatus.COMPLETED, notes)
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }

    fun deleteRequest(requestId: String, onResult: (Boolean) -> Unit) {
        database.child(requestId).removeValue().addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }
}
