package com.code.w.admin.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.code.w.admin.repository.AdminRepository
import com.code.w.admin.repository.AdminSupportRequest
import com.code.w.admin.util.AdminNotificationHelper
import com.code.w.common.model.RequestStatus
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminSupportViewModel : ViewModel() {
    private val appContext: Context = FirebaseApp.getInstance().applicationContext
    private val repository = AdminRepository()

    private val _requests = MutableStateFlow<List<AdminSupportRequest>>(emptyList())
    val requests: StateFlow<List<AdminSupportRequest>> = _requests.asStateFlow()

    // خريطة لتتبع الحالات السابقة للطلبات ومنع تكرار الإشعار لنفس الحدث
    private val knownRequestStatuses = mutableMapOf<String, String>()
    private var isInitialSnapshot = true

    init {
        fetchAllRequests()
    }

    private fun fetchAllRequests() {
        viewModelScope.launch {
            repository.observeAllRequests().collect { list ->
                if (isInitialSnapshot) {
                    // عند فتح التطبيق لأول مرة نسجل الطلبات الحالية بصمت دون إزعاج الأدمن
                    list.forEach { req ->
                        knownRequestStatuses[req.id] = req.status
                    }
                    isInitialSnapshot = false
                } else {
                    // مقارنة التحديث الجديد مع الحالات المسجلة لاكتشاف الطلبات أو السندات الجديدة
                    list.forEach { req ->
                        val oldStatus = knownRequestStatuses[req.id]

                        if (oldStatus == null && req.status == RequestStatus.SUBMITTED.name) {
                            // وصل طلب جديد كلياً
                            AdminNotificationHelper.notifyNewRequest(
                                context = appContext,
                                requestId = req.id,
                                phoneNumber = req.phoneNumber
                            )
                        } else if (oldStatus != RequestStatus.RECEIPT_SUBMITTED.name &&
                            req.status == RequestStatus.RECEIPT_SUBMITTED.name
                        ) {
                            // قام المستخدم برفع سند الإيداع الآن
                            AdminNotificationHelper.notifyReceiptUploaded(
                                context = appContext,
                                requestId = req.id,
                                phoneNumber = req.phoneNumber,
                                fileType = req.receiptFileType
                            )
                        }

                        knownRequestStatuses[req.id] = req.status
                    }

                    // تنظيف المعرفات التي تم حذفها
                    val currentIds = list.map { it.id }.toSet()
                    knownRequestStatuses.keys.retainAll(currentIds)
                }

                _requests.value = list
            }
        }
    }

    fun preApproveRequest(requestId: String, durationMinutes: Int, onComplete: (Boolean) -> Unit) {
        repository.preApproveWithTimer(requestId, durationMinutes, onComplete)
    }

    fun rejectRequest(requestId: String, rejectionNote: String, onComplete: (Boolean) -> Unit) {
        repository.rejectRequestWithNote(requestId, rejectionNote, onComplete)
    }

    fun approveRequest(requestId: String, bankDetails: String, onComplete: (Boolean) -> Unit) {
        repository.approveWithBankDetails(requestId, bankDetails, onComplete)
    }

    fun confirmDepositReceived(requestId: String, durationMinutes: Int, onComplete: (Boolean) -> Unit) {
        repository.confirmDepositAndStartProcessing(requestId, durationMinutes, onComplete)
    }

    fun rejectDeposit(requestId: String, note: String, onComplete: (Boolean) -> Unit) {
        repository.rejectDepositNotConfirmed(requestId, note, onComplete)
    }

    fun completeRequest(requestId: String, notes: String, onComplete: (Boolean) -> Unit) {
        repository.completeRequestWithNotes(requestId, notes, onComplete)
    }

    fun deleteRequest(requestId: String, onComplete: (Boolean) -> Unit) {
        repository.deleteRequest(requestId, onComplete)
    }
}
