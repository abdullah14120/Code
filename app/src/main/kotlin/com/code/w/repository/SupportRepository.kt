package com.code.w.repository

import android.net.Uri
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.code.w.model.SupportRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class SupportRepository {
    private val database = FirebaseDatabase.getInstance().getReference("support_requests")
    private val storage = FirebaseStorage.getInstance().getReference("receipts")

    // إنشاء طلب جديد في الفايربيس
    fun createRequest(phoneNumber: String, issueType: String, onResult: (String?) -> Unit) {
        val requestId = database.push().key ?: return onResult(null)
        val request = SupportRequest(id = requestId, phoneNumber = phoneNumber, issueType = issueType)
        
        database.child(requestId).setValue(request).addOnCompleteListener { task ->
            if (task.isSuccessful) onResult(requestId) else onResult(null)
        }
    }

    // الاستماع اللحظي لتحديثات الطلب وتحويلها إلى Flow
    fun observeRequest(requestId: String): Flow<SupportRequest?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val request = snapshot.getValue(SupportRequest::class.java)
                trySend(request)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        database.child(requestId).addValueEventListener(listener)
        awaitClose { database.child(requestId).removeEventListener(listener) }
    }

    // رفع الصورة إلى الفايربيس ستوريدج وتحديث حالة الطلب
    fun uploadReceiptAndSubmit(requestId: String, imageUri: Uri, onResult: (Boolean) -> Unit) {
        val fileName = "receipt_${UUID.randomUUID()}.jpg"
        val fileRef = storage.child("$requestId/$fileName")

        fileRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                fileRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUrl = task.result.toString()
                    val updates = mapOf(
                        "receiptImageUrl" to downloadUrl,
                        "status" to SupportRequest.Status.RECEIPT_SUBMITTED
                    )
                    database.child(requestId).updateChildren(updates)
                        .addOnCompleteListener { updateTask ->
                            onResult(updateTask.isSuccessful)
                        }
                } else {
                    onResult(false)
                }
            }
    }
}
