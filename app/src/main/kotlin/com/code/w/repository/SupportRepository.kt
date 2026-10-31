package com.code.w.repository

import android.content.Context
import com.code.w.common.model.DatabaseKeys
import com.code.w.common.model.RequestStatus
import com.code.w.model.SupportRequest
import com.code.w.util.ProcessedReceipt
import com.code.w.worker.ReceiptUploadWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SupportRepository(
    private val context: Context = FirebaseApp.getInstance().applicationContext
) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference(DatabaseKeys.NODE_REQUESTS)

    private fun ensureAnonymousAuth(onReady: (String) -> Unit) {
        val current = auth.currentUser
        if (current != null) {
            registerFcmToken(current.uid)
            onReady(current.uid)
        } else {
            auth.signInAnonymously().addOnCompleteListener { task ->
                val uid = task.result?.user?.uid ?: "anonymous"
                if (uid != "anonymous") registerFcmToken(uid)
                onReady(uid)
            }
        }
    }

    private fun registerFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (!token.isNullOrBlank()) {
                FirebaseDatabase.getInstance()
                    .getReference(DatabaseKeys.NODE_USERS_FCM)
                    .child(uid)
                    .setValue(token)
            }
        }
    }

    fun createRequest(
        phoneNumber: String,
        issueType: String = SupportRequest.DEFAULT_ISSUE_TYPE,
        onResult: (String?) -> Unit
    ) {
        val cleanPhone = phoneNumber.trim()
        ensureAnonymousAuth { uid ->
            // منع إنشاء طلب مكرر إذا كان لنفس الرقم طلب نشط
            database.orderByChild(DatabaseKeys.FIELD_PHONE_NUMBER)
                .equalTo(cleanPhone)
                .get()
                .addOnCompleteListener { checkTask ->
                    if (checkTask.isSuccessful) {
                        val activeExisting = checkTask.result?.children?.firstOrNull { snap ->
                            val statusStr = snap.child(DatabaseKeys.FIELD_STATUS).getValue(String::class.java)
                            RequestStatus.fromStringOrDefault(statusStr).isActive()
                        }
                        if (activeExisting != null) {
                            // إذا كان هناك طلب نشط لنفس الرقم نفتح الطلب النشط نفسه بدلاً من تكراره
                            onResult(activeExisting.key)
                            return@addOnCompleteListener
                        }
                    }

                    val requestId = database.push().key ?: return@addOnCompleteListener onResult(null)
                    val initialData = mapOf<String, Any>(
                        DatabaseKeys.FIELD_ID to requestId,
                        DatabaseKeys.FIELD_OWNER_UID to uid,
                        DatabaseKeys.FIELD_PHONE_NUMBER to cleanPhone,
                        DatabaseKeys.FIELD_ISSUE_TYPE to issueType,
                        DatabaseKeys.FIELD_STATUS to SupportRequest.Status.SUBMITTED.name,
                        DatabaseKeys.FIELD_CREATED_AT to ServerValue.TIMESTAMP,
                        DatabaseKeys.FIELD_UPDATED_AT to ServerValue.TIMESTAMP
                    )

                    database.child(requestId).setValue(initialData).addOnCompleteListener { task ->
                        if (task.isSuccessful) onResult(requestId) else onResult(null)
                    }
                }
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
        val id = snapshot.child(DatabaseKeys.FIELD_ID).getValue(String::class.java) ?: snapshot.key.orEmpty()
        val phoneNumber = snapshot.child(DatabaseKeys.FIELD_PHONE_NUMBER).getValue(String::class.java).orEmpty()
        val issueType = snapshot.child(DatabaseKeys.FIELD_ISSUE_TYPE).getValue(String::class.java)
            ?: SupportRequest.DEFAULT_ISSUE_TYPE
        val statusStr = snapshot.child(DatabaseKeys.FIELD_STATUS).getValue(String::class.java) ?: "SUBMITTED"
        val status = try {
            SupportRequest.Status.valueOf(statusStr)
        } catch (_: Exception) {
            SupportRequest.Status.SUBMITTED
        }
        val bankDetails = snapshot.child(DatabaseKeys.FIELD_BANK_DETAILS).getValue(String::class.java).orEmpty()
        val adminNotes = snapshot.child(DatabaseKeys.FIELD_ADMIN_NOTES).getValue(String::class.java).orEmpty()
        val rejectionReason = snapshot.child(DatabaseKeys.FIELD_REJECTION_REASON).getValue(String::class.java).orEmpty()
        val receiptImageUrl = snapshot.child(DatabaseKeys.FIELD_RECEIPT_URL).getValue(String::class.java).orEmpty()
        val receiptFileType = snapshot.child(DatabaseKeys.FIELD_RECEIPT_FILE_TYPE).getValue(String::class.java).orEmpty()
        val receiptFileName = snapshot.child(DatabaseKeys.FIELD_RECEIPT_FILE_NAME).getValue(String::class.java).orEmpty()
        val timerEndTime = snapshot.child(DatabaseKeys.FIELD_TIMER_END_TIME).getValue(Long::class.java) ?: 0L
        val timerTotalSeconds = snapshot.child(DatabaseKeys.FIELD_TIMER_TOTAL_SECONDS).getValue(Long::class.java) ?: 1800L
        val createdAt = snapshot.child(DatabaseKeys.FIELD_CREATED_AT).getValue(Long::class.java) ?: 0L

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

    fun uploadProcessedReceipt(
        requestId: String,
        receipt: ProcessedReceipt,
        onResult: (Boolean) -> Unit
    ) {
        ensureAnonymousAuth { uid ->
            if (receipt.localFilePath.isNotBlank()) {
                ReceiptUploadWorker.enqueueUpload(
                    context = context,
                    requestId = requestId,
                    ownerUid = uid,
                    localFilePath = receipt.localFilePath,
                    mimeType = receipt.mimeType,
                    sha256 = receipt.sha256Hash,
                    fileType = receipt.fileType,
                    fileName = receipt.fileName
                )
            }

            val updates = mapOf<String, Any>(
                DatabaseKeys.FIELD_RECEIPT_URL to receipt.base64Data,
                DatabaseKeys.FIELD_RECEIPT_SHA256 to receipt.sha256Hash,
                DatabaseKeys.FIELD_RECEIPT_FILE_TYPE to receipt.fileType,
                DatabaseKeys.FIELD_RECEIPT_FILE_NAME to receipt.fileName,
                DatabaseKeys.FIELD_STATUS to SupportRequest.Status.RECEIPT_SUBMITTED.name,
                DatabaseKeys.FIELD_REJECTION_REASON to "",
                DatabaseKeys.FIELD_UPDATED_AT to ServerValue.TIMESTAMP
            )

            database.child(requestId).updateChildren(updates).addOnCompleteListener { task ->
                onResult(task.isSuccessful)
            }
        }
    }

    fun deleteRequest(requestId: String, onResult: (Boolean) -> Unit = {}) {
        database.child(requestId).removeValue().addOnCompleteListener { task ->
            onResult(task.isSuccessful)
        }
    }
}
