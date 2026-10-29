package com.code.w.admin.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class AdminSupportRequest(
    val id: String = "",
    val phoneNumber: String = "",
    val issueType: String = "مشكلة كود التحقق أو مكالمة فائتة",
    val status: String = "SUBMITTED",
    val adminNotes: String = "",
    val rejectionReason: String = "",
    val bankDetails: String = "",
    val receiptImageUrl: String = "",
    val receiptFileType: String = "",
    val receiptFileName: String = "",
    val timerEndTime: Long = 0L,
    val timerTotalSeconds: Long = 1800L,
    val createdAt: Long = 0L
)

class AdminRepository {
    private val database = FirebaseDatabase.getInstance().getReference("support_requests")

    fun observeAllRequests(): Flow<List<AdminSupportRequest>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = snapshot.children.mapNotNull { child ->
                    parseAdminRequestSafely(child)
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
        val id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: return null
        val phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java).orEmpty()
        if (phoneNumber.isBlank()) return null

        val issueType = snapshot.child("issueType").getValue(String::class.java) ?: "مشكلة كود التحقق أو مكالمة فائتة"
        val status = snapshot.child("status").getValue(String::class.java) ?: "SUBMITTED"
        val adminNotes = snapshot.child("adminNotes").getValue(String::class.java).orEmpty()
        val rejectionReason = snapshot.child("rejectionReason").getValue(String::class.java).orEmpty()
        val bankDetails = snapshot.child("bankDetails").getValue(String::class.java).orEmpty()
        val receiptImageUrl = snapshot.child("receiptImageUrl").getValue(String::class.java).orEmpty()
        val receiptFileType = snapshot.child("receiptFileType").getValue(String::class.java).orEmpty()
        val receiptFileName = snapshot.child("receiptFileName").getValue(String::class.java).orEmpty()
        val timerEndTime = snapshot.child("timerEndTime").getValue(Long::class.java) ?: 0L
        val timerTotalSeconds = snapshot.child("timerTotalSeconds").getValue(Long::class.java) ?: 1800L
        val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L

        return AdminSupportRequest(
            id = id,
            phoneNumber = phoneNumber,
            issueType = issueType,
            status = status,
            adminNotes = adminNotes,
            rejectionReason = rejectionReason,
            bankDetails = bankDetails,
            receiptImageUrl = receiptImageUrl,
            receiptFileType = receiptFileType,
            receiptFileName = receiptFileName,
            timerEndTime = timerEndTime,
            timerTotalSeconds = timerTotalSeconds,
            createdAt = createdAt
        )
    }

    // 1. قبول الطلب وبدء عداد انتظار مراجعة الطلب
    fun preApproveWithTimer(requestId: String, durationMinutes: Int, onResult: (Boolean) -> Unit) {
        val totalSeconds = durationMinutes * 60L
        val endTime = System.currentTimeMillis() + (totalSeconds * 1000L)
        val updates = mapOf(
            "status" to "PRE_APPROVED",
            "timerEndTime" to endTime,
            "timerTotalSeconds" to totalSeconds
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }

    // 2. رفض الطلب مع ذكر ملاحظة الرفض
    fun rejectRequestWithNote(requestId: String, rejectionNote: String, onResult: (Boolean) -> Unit) {
        val updates = mapOf(
            "status" to "REJECTED",
            "rejectionReason" to rejectionNote,
            "adminNotes" to rejectionNote
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }

    // 3. طلب إيداع المبلغ وإظهار واجهة الإيداع الخضراء للمستخدم
    fun approveWithBankDetails(requestId: String, bankDetails: String, onResult: (Boolean) -> Unit) {
        val updates = mapOf(
            "bankDetails" to bankDetails,
            "status" to "APPROVED"
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }

    // 4. زر الأدمن لتأكيد أن الإيداع قد تم وتحويل المستخدم لواجهة "جاري معالجة المشكلة" مع عداد احترافي
    fun confirmDepositAndStartProcessing(requestId: String, durationMinutes: Int, onResult: (Boolean) -> Unit) {
        val totalSeconds = durationMinutes * 60L
        val endTime = System.currentTimeMillis() + (totalSeconds * 1000L)
        val updates = mapOf(
            "status" to "PROCESSING",
            "timerEndTime" to endTime,
            "timerTotalSeconds" to totalSeconds
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }

    // 4.5 الأدمن لم يجد الإيداع: إعادة المستخدم لواجهة الإيداع مع تنبيه ومسح السند السابق
    fun rejectDepositNotConfirmed(requestId: String, note: String, onResult: (Boolean) -> Unit) {
        val updates = mapOf(
            "status" to "APPROVED",
            "rejectionReason" to note,
            "receiptImageUrl" to "",
            "receiptFileType" to "",
            "receiptFileName" to ""
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }

    // 5. تأكيد حل المشكلة بنجاح وإظهار علامة الصح والملاحظات للمستخدم
    fun completeRequestWithNotes(requestId: String, notes: String, onResult: (Boolean) -> Unit) {
        val updates = mapOf(
            "adminNotes" to notes,
            "status" to "COMPLETED"
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }

    // 6. حذف الطلب من قاعدة البيانات
    fun deleteRequest(requestId: String, onResult: (Boolean) -> Unit) {
        database.child(requestId).removeValue().addOnCompleteListener {
            onResult(it.isSuccessful)
        }
    }
}
