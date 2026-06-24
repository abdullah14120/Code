package com.code.w.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.code.w.model.SupportRequest
import com.code.w.repository.SupportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserSupportViewModel : ViewModel() {
    private val repository = SupportRepository()

    private val _currentRequest = MutableStateFlow<SupportRequest?>(null)
    val currentRequest: StateFlow<SupportRequest?> = _currentRequest.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    fun sendSupportRequest(phoneNumber: String, issueType: String, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        _isSubmitting.value = true
        repository.createRequest(phoneNumber, issueType) { requestId ->
            _isSubmitting.value = false
            if (requestId != null) {
                onSuccess(requestId)
            } else {
                onFailure()
            }
        }
    }

    fun startObservingRequest(requestId: String) {
        viewModelScope.launch {
            repository.observeRequest(requestId).collect { request ->
                _currentRequest.value = request
            }
        }
    }

    fun uploadReceipt(requestId: String, imageUri: Uri, onComplete: (Boolean) -> Unit) {
        _isUploading.value = true
        repository.uploadReceiptAndSubmit(requestId, imageUri) { success ->
            _isUploading.value = false
            onComplete(success)
        }
    }
}
