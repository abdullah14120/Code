package com.code.w.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.code.w.model.SupportRequest
import com.code.w.repository.SupportRepository
import com.code.w.util.ProcessedReceipt
import com.code.w.util.ReceiptFileProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserSupportViewModel : ViewModel() {
    private val repository = SupportRepository()
    private var observeJob: Job? = null

    private val _currentRequest = MutableStateFlow<SupportRequest?>(null)
    val currentRequest: StateFlow<SupportRequest?> = _currentRequest.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _isProcessingFile = MutableStateFlow(false)
    val isProcessingFile: StateFlow<Boolean> = _isProcessingFile.asStateFlow()

    private val _attachedReceipt = MutableStateFlow<ProcessedReceipt?>(null)
    val attachedReceipt: StateFlow<ProcessedReceipt?> = _attachedReceipt.asStateFlow()

    private val _isSharedFromExternalApp = MutableStateFlow(false)
    val isSharedFromExternalApp: StateFlow<Boolean> = _isSharedFromExternalApp.asStateFlow()

    fun sendSupportRequest(
        phoneNumber: String,
        onSuccess: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        _isSubmitting.value = true
        repository.createRequest(phoneNumber, SupportRequest.DEFAULT_ISSUE_TYPE) { requestId: String? ->
            _isSubmitting.value = false
            if (requestId != null) onSuccess(requestId) else onFailure()
        }
    }

    fun startObservingRequest(requestId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeRequest(requestId).collect { request: SupportRequest? ->
                _currentRequest.value = request
            }
        }
    }

    fun processReceiptUri(
        context: Context,
        uri: Uri,
        fromExternalShare: Boolean = false,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isProcessingFile.value = true
            val processed = withContext(Dispatchers.IO) {
                ReceiptFileProcessor.processUri(context, uri)
            }
            _isProcessingFile.value = false
            if (processed != null) {
                _attachedReceipt.value = processed
                _isSharedFromExternalApp.value = fromExternalShare
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun clearAttachedReceipt() {
        _attachedReceipt.value = null
        _isSharedFromExternalApp.value = false
    }

    fun confirmAndUploadReceipt(requestId: String, onComplete: (Boolean) -> Unit) {
        val receipt = _attachedReceipt.value ?: return onComplete(false)
        _isUploading.value = true
        repository.uploadProcessedReceipt(requestId, receipt) { success: Boolean ->
            _isUploading.value = false
            if (success) {
                _attachedReceipt.value = null
                _isSharedFromExternalApp.value = false
            }
            onComplete(success)
        }
    }

    fun resetSessionState() {
        observeJob?.cancel()
        _currentRequest.value = null
        _attachedReceipt.value = null
        _isSharedFromExternalApp.value = false
    }
}
