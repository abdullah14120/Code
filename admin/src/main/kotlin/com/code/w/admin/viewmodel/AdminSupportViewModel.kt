package com.code.w.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.code.w.admin.repository.AdminRepository
import com.code.w.admin.repository.AdminSupportRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminSupportViewModel : ViewModel() {
    private val repository = AdminRepository()

    private val _requests = MutableStateFlow<List<AdminSupportRequest>>(emptyList())
    val requests: StateFlow<List<AdminSupportRequest>> = _requests.asStateFlow()

    init {
        fetchAllRequests()
    }

    private fun fetchAllRequests() {
        viewModelScope.launch {
            repository.observeAllRequests().collect { list ->
                _requests.value = list
            }
        }
    }

    // الدالة المضافة حديثاً لمنح الموافقة المبدئية وتفعيل العداد التنازلي لدى المستخدم
    fun preApproveRequest(requestId: String, endTime: Long, onComplete: (Boolean) -> Unit) {
        repository.preApproveWithTimer(requestId, endTime, onComplete)
    }

    fun approveRequest(requestId: String, bankDetails: String, onComplete: (Boolean) -> Unit) {
        repository.approveWithBankDetails(requestId, bankDetails, onComplete)
    }

    fun completeRequest(requestId: String, notes: String, onComplete: (Boolean) -> Unit) {
        repository.completeRequestWithNotes(requestId, notes, onComplete)
    }
}
