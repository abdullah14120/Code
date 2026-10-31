package com.code.w.repository

import android.content.Context
import com.code.w.common.model.DatabaseKeys
import com.code.w.common.model.RequestStatus
import com.code.w.common.model.SupportRequest
import com.code.w.util.ReceiptValidationResult
import com.code.w.worker.ReceiptUploadWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class DepositConfig(
    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val defaultTimerMinutes: Long,
    val supportWhatsappNumber: String
)

class SupportRepository(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val requestsRef = db.getReference(DatabaseKeys.NODE_REQUESTS)
    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    /**
     * تسجيل الدخول بشكل مجهول (Anonymous Auth) وتحديث توكن الإشعارات FCM
     */
    suspend fun ensureAuthenticated(): String {
        val currentUser = auth.currentUser ?: auth.signInAnonymously().await().user
        val uid = currentUser?.uid ?: throw IllegalStateException("فشل تسجيل الدخول المجهول")

        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            db.getReference(DatabaseKeys.NODE_USERS_FCM).child(uid).setValue(token)
        }
        return uid
    }

    /**
     * جلب بيانات الحساب البنكي والمدد ورقم الدعم من Firebase Remote Config
     */
    suspend fun fetchDepositConfig(): DepositConfig {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings).await()
        remoteConfig.setDefaultsAsync(
            mapOf(
                DatabaseKeys.RC_BANK_NAME to "بنك الكريمي / العمقي",
                DatabaseKeys.RC_ACCOUNT_NAME to "اسم الحساب الرسمي",
                DatabaseKeys.RC_ACCOUNT_NUMBER to "000000000",
                DatabaseKeys.RC_DEFAULT_TIMER_MINUTES to 15L,
                DatabaseKeys.RC_SUPPORT_WHATSAPP to "967770000000"
            )
        ).await()
        runCatching { remoteConfig.fetchAndActivate().await() }

        return DepositConfig(
            bankName = remoteConfig.getString(DatabaseKeys.RC_BANK_NAME),
            accountName = remoteConfig.getString(DatabaseKeys.RC_ACCOUNT_NAME),
            accountNumber = remoteConfig.getString(DatabaseKeys.RC_ACCOUNT_NUMBER),
            defaultTimerMinutes = remoteConfig.getLong(DatabaseKeys.RC_DEFAULT_TIMER_MINUTES),
            supportWhatsappNumber = remoteConfig.getString(DatabaseKeys.RC_SUPPORT_WHATSAPP)
        )
    }

    /**
     * إنشاء طلب جديد بعد التحقق من عدم وجود طلب نشط لنفس الرقم، وجدولة رفع السند عبر WorkManager
     */
    suspend fun createSupportRequest(
        phoneNumber: String,
        validatedReceipt: ReceiptValidationResult.Valid
    ): Result<String> {
        return try {
            val uid = ensureAuthenticated()
            val normalizedPhone = phoneNumber.trim()

            // التحقق من عدم وجود طلب نشط لنفس رقم الهاتف لدى المستخدم
            val existingSnapshot = requestsRef
                .orderByChild(DatabaseKeys.FIELD_OWNER_UID)
                .equalTo(uid)
                .get()
                .await()

            val hasActiveRequestForPhone = existingSnapshot.children.any { snap ->
                val req = snap.getValue(SupportRequest::class.java)
                req != null && req.phoneNumber == normalizedPhone && req.statusEnum.isActive()
            }

            if (hasActiveRequestForPhone) {
                return Result.failure(IllegalStateException("يوجد طلب نشط قيد المعالجة لهذا الرقم بالفعل. يرجى انتظار اكتماله أولاً."))
            }

            val newRequestRef = requestsRef.push()
            val requestId = newRequestRef.key ?: throw IllegalStateException("تعذر توليد معرف الطلب")

            val initialData = mapOf(
                DatabaseKeys.FIELD_ID to requestId,
                DatabaseKeys.FIELD_OWNER_UID to uid,
                DatabaseKeys.FIELD_PHONE_NUMBER to normalizedPhone,
                DatabaseKeys.FIELD_STATUS to RequestStatus.PENDING_UPLOAD.name,
                DatabaseKeys.FIELD_RECEIPT_SHA256 to validatedReceipt.sha256Hash,
                DatabaseKeys.FIELD_CREATED_AT to ServerValue.TIMESTAMP,
                DatabaseKeys.FIELD_UPDATED_AT to ServerValue.TIMESTAMP
            )

            newRequestRef.setValue(initialData).await()

            // جدولة رفع السند عبر WorkManager (يرفع فوراً إذا توفر الإنترنت أو ينتظر الاتصال)
            ReceiptUploadWorker.enqueueUpload(
                context = context,
                requestId = requestId,
                ownerUid = uid,
                localFilePath = validatedReceipt.localFile.absolutePath,
                mimeType = validatedReceipt.mimeType,
                sha256 = validatedReceipt.sha256Hash
            )

            Result.success(requestId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الاستماع لسجل طلبات المستخدم الحالي فقط (النشطة والسابقة)
     */
    fun observeUserRequests(uid: String): Flow<List<SupportRequest>> = callbackFlow {
        val query = requestsRef.orderByChild(DatabaseKeys.FIELD_OWNER_UID).equalTo(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(SupportRequest::class.java) }
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    /**
     * الاستماع لفرق توقيت السيرفر لحساب العداد بدقة
     */
    fun observeServerTimeOffset(): Flow<Long> = callbackFlow {
        val offsetRef = db.getReference(DatabaseKeys.SERVER_TIME_OFFSET)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Long::class.java) ?: 0L)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        offsetRef.addValueEventListener(listener)
        awaitClose { offsetRef.removeEventListener(listener) }
    }
}
