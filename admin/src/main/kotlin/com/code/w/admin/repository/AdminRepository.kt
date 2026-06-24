package com.code.w.admin.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// نقوم بتعريف كائن البيانات محلياً داخل حزمة الأدمن لضمان استقلالية الموديل إذا لم يتم دمج الموديلات
data class AdminSupportRequest(
    val id: String = "",
    val phoneNumber: String = "",
    val issueType: String = "",
    val status: String = "SUBMITTED",
    val adminNotes: String = "",
    val bankDetails: String = "",
    val receiptImageUrl: String = ""
)

class AdminRepository {
    private val database = FirebaseDatabase.getInstance().getReference("support_requests")

    // جلب جميع الطلبات المفتوحة والاستماع اللحظي للتغيرات
    fun observeAllRequests(): Flow<List<AdminSupportRequest>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = snapshot.children.mapNotNull { it.getValue(AdminSupportRequest::class.java) }
                trySend(requests)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        database.addValueEventListener(listener)
        awaitClose { database.removeEventListener(listener) }
    }

    // الزر الأول: تحديث الحالة إلى APPROVED وإرسال بيانات البنك للمستخدم
    fun approveWithBankDetails(requestId: String, bankDetails: String, onResult: (Boolean) -> Unit) {
        val updates = mapOf(
            "bankDetails" to bankDetails,
            "status" to "APPROVED"
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener { 
            onResult(it.isSuccessful)
        }
    }

    // الزر الثاني: تحديث الحالة إلى COMPLETED وإرسال التعليمات النهائية للمستخدم وإغلاق الطلب
    fun completeRequestWithNotes(requestId: String, notes: String, onResult: (Boolean) -> Unit) {
        val updates = mapOf(
            "adminNotes" to notes,
            "status" to "COMPLETED"
        )
        database.child(requestId).updateChildren(updates).addOnCompleteListener { 
            onResult(it.isSuccessful)
        }
    }
}
