package com.code.w.admin.repository

import com.code.w.common.model.DatabaseKeys
import com.code.w.common.model.RequestStatus
import com.code.w.common.model.SupportRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AdminRepository {
    private val db = FirebaseDatabase.getInstance()
    private val requestsRef = db.getReference(DatabaseKeys.NODE_REQUESTS)
    private val auth = FirebaseAuth.getInstance()

    /**
     * الاستماع لجميع الطلبات مع كشف تلقائي للسندات المكررة عبر مقارنة بصمة SHA-256
     */
    fun observeAllRequests(): Flow<List<SupportRequest>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rawList = snapshot.children.mapNotNull { it.getValue(SupportRequest::class.java) }

                // حساب تكرارات بصمة السند SHA-256 لكشف السندات المرفوعة أكثر من مرة
                val hashCounts = rawList
                    .filter { it.receiptSha256.isNotBlank() }
                    .groupingBy { it.receiptSha256 }
                    .eachCount()

                val enrichedList = rawList.map { req ->
                    val isDuplicate = req.receiptSha256.isNotBlank() && (hashCounts[req.receiptSha256] ?: 0) > 1
                    req.copy(isDuplicateReceipt = isDuplicate)
                }.sortedByDescending { it.createdAt }

                trySend(enrichedList)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        requestsRef.addValueEventListener(listener)
        awaitClose { requestsRef.removeEventListener(listener) }
    }

    /**
     * تحديث حالة الطلب بأمان عبر Enum، وتسجيل ServerValue.TIMESTAMP للعداد، وتوثيق هوية الأدمن في Audit Log
     */
    suspend fun updateRequestStatus(
        request: SupportRequest,
        newStatus: RequestStatus,
        timerDurationMinutes: Long? = null,
        note: String = ""
    ): Result<Unit> {
        return try {
            val currentStatus = request.statusEnum
            if (!currentStatus.canTransitionTo(newStatus)) {
                return Result.failure(
                    IllegalStateException("لا يمكن الانتقال من حالة (${currentStatus.arabicLabel}) إلى (${newStatus.arabicLabel})")
                )
            }

            val adminUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("يجب تسجيل دخول الأدمن أولاً"))

            val requestRef = requestsRef.child(request.id)
            val logId = requestRef.child(DatabaseKeys.FIELD_AUDIT_LOGS).push().key ?: System.currentTimeMillis().toString()

            val auditEntryMap = mapOf(
                "logId" to logId,
                "adminUid" to adminUser.uid,
                "adminIdentifier" to (adminUser.email ?: adminUser.phoneNumber ?: adminUser.uid),
                "fromStatus" to currentStatus.name,
                "toStatus" to newStatus.name,
                "note" to note,
                "timestamp" to ServerValue.TIMESTAMP
            )

            val updates = mutableMapOf<String, Any>(
                DatabaseKeys.FIELD_STATUS to newStatus.name,
                DatabaseKeys.FIELD_UPDATED_AT to ServerValue.TIMESTAMP,
                "${DatabaseKeys.FIELD_AUDIT_LOGS}/$logId" to auditEntryMap
            )

            // إذا تم تحديد عداد زمني، نستخدم وقت خادم Firebase كوقت بداية وليس ساعة جهاز الأدمن
            if (timerDurationMinutes != null && timerDurationMinutes > 0) {
                updates[DatabaseKeys.FIELD_TIMER_START_TIME] = ServerValue.TIMESTAMP
                updates[DatabaseKeys.FIELD_TIMER_DURATION_MS] = timerDurationMinutes * 60 * 1000L
            }

            // إذا اكتمل الطلب أو رُفض، نسجل وقت الإغلاق لجدولة الحذف التلقائي لاحقاً
            if (newStatus == RequestStatus.COMPLETED || newStatus == RequestStatus.REJECTED) {
                updates[DatabaseKeys.FIELD_COMPLETED_AT] = ServerValue.TIMESTAMP
            }

            requestRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * حفظ نتائج قراءة الـ OCR المقترحة في الطلب ليراها الأدمن
     */
    suspend fun saveOcrSuggestions(requestId: String, amount: String?, refNumber: String?) {
        val updates = mutableMapOf<String, Any>()
        if (!amount.isNullOrBlank()) updates[DatabaseKeys.FIELD_OCR_AMOUNT] = amount
        if (!refNumber.isNullOrBlank()) updates[DatabaseKeys.FIELD_OCR_REF] = refNumber
        if (updates.isNotEmpty()) {
            requestsRef.child(requestId).updateChildren(updates).await()
        }
    }
}
