package com.code.w.repository

import com.code.w.model.SupportRequest
import com.code.w.util.ProcessedReceipt
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SupportRepository {
    private val database = FirebaseDatabase.getInstance().getReference("support_requests")

    fun createRequest(phoneNumber: String, issueType: String = SupportRequest.DEFAULT_ISSUE_TYPE, onResult: (String?) -> Unit) {
        val requestId = database.push().key ?: return onResult(null)
        val request = SupportRequest(
            id = requestId,
            phoneNumber = phoneNumber,
            issueType = issueType,
            status = SupportRequest.Status.SUBMITTED,
            createdAt = System.currentTimeMillis()
        )

        database.child(requestId).setValue(request).addOnCompleteListener { task ->
            if (task.isSuccessful) onResult(requestId) else onResult(null)
        }
    }

    fun observeRequest(requestId: String): Flow<SupportRequest?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }
                val request = parseSupportRequestSafely(snapshot)
                trySend(request)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        database.child(requestId).addValueEventListener(listener)
        awaitClose { database.child(requestId).removeEventListener(listener) }
    }

    private fun parseSupportRequestSafely(snapshot: DataSnapshot): SupportRequest {
        val id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key.orEmpty()
        val phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java).orEmpty()
        val issueType = snapshot.child("issueType").getValue(String::class.java) ?: SupportRequest.DEFAULT_ISSUE_TYPE
        val statusStr = snapshot.child("status").getValue(String::class.java) ?: "SUBMITTED"
        val status = try {
            SupportRequest.Status.valueOf(statusStr)
        } catch (_: Exception) {
            SupportRequest.Status.SUBMITTED
        }
        val bankDetails = snapshot.child("bankDetails").getValue(String::class.java).orEmpty()
        val adminNotes = snapshot.child("adminNotes").getValue(String::class.java).orEmpty()
        val rejectionReason = snapshot.child("rejectionReason").getValue(String::class.java).orEmpty()
        val receiptImageUrl = snapshot.child("receiptImageUrl").getValue(String::class.java).orEmpty()
        val receiptFileType = snapshot.child("receiptFileType").getValue(String::class.java).orEmpty()
        val receiptFileName = snapshot.child("receiptFileName").getValue(String::class.java).orEmpty()
        val timerEndTime = snapshot.child("timerEndTime").getValue(Long::class.java) ?: 0L
        val timerTotalSeconds = snapshot.child("timerTotalSeconds").getValue(Long::class.java) ?: 1800L
        val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L

        return SupportRequest(
            id = id,
            phoneNumber = phoneNumber,
            issueType = issueType,
            status = status,
            bankDetails = bankDetails,
            adminNotes = adminNotes,
            rejectionReason = rejectionReason,
            receiptImageUrl = receiptImageUrl,
            receiptFileType = receiptFileType,
            receiptFileName = receiptFileName,
            timerEndTime = timerEndTime,
            timerTotalSeconds = timerTotalSeconds,
            createdAt = createdAt
        )
    }

    fun uploadProcessedReceipt(requestId: String, receipt: ProcessedReceipt, onResult: (Boolean) -> Unit) {
        val defaultProcessingSeconds = 20 * 60L // 20 دقيقة عداد معالجة افتراضي عند تأكيد الإيداع
        val timerEndTime = System.currentTimeMillis() + (defaultProcessingSeconds * 1000L)

        val updates = mapOf(
            "receiptImageUrl" to receipt.base64Data,
            "receiptFileType" to receipt.fileType,
            "receiptFileName" to receipt.fileName,
            "status" to SupportRequest.Status.RECEIPT_SUBMITTED.name,
            "timerEndTime" to timerEndTime,
            "timerTotalSeconds" to defaultProcessingSeconds
        )

        database.child(requestId).updateChildren(updates).addOnCompleteListener { task ->
            onResult(task.isSuccessful)
        }
    }

    fun deleteRequest(requestId: String, onResult: (Boolean) -> Unit = {}) {
        database.child(requestId).removeValue().addOnCompleteListener { task ->
            onResult(task.isSuccessful)
        }
    }
}
