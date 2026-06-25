package com.code.w.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.code.w.model.SupportRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.InputStream

class SupportRepository {
    private val database = FirebaseDatabase.getInstance().getReference("support_requests")

    fun createRequest(phoneNumber: String, issueType: String, onResult: (String?) -> Unit) {
        val requestId = database.push().key ?: return onResult(null)
        val request = SupportRequest(id = requestId, phoneNumber = phoneNumber, issueType = issueType)
        
        database.child(requestId).setValue(request).addOnCompleteListener { task ->
            if (task.isSuccessful) onResult(requestId) else onResult(null)
        }
    }

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

    // الطريقة البديلة: تحويل الصورة إلى Base64 ورفعها كـ String إلى الـ Database مباشرة
    fun uploadReceiptAsBase64(context: Context, requestId: String, imageUri: Uri, onResult: (Boolean) -> Unit) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                // تشفير مصفوفة البايتات إلى نص String مضغوط
                val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
                
                val updates = mapOf(
                    "receiptImageUrl" to base64Image, // سيخزن النص المشفر في نفس حقل الرابط مسبقاً
                    "status" to "RECEIPT_SUBMITTED"
                )
                
                database.child(requestId).updateChildren(updates).addOnCompleteListener { task ->
                    onResult(task.isSuccessful)
                }
            } else {
                onResult(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(false)
        }
    }
}
