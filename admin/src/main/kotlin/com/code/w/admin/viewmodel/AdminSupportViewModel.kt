package com.code.w.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.code.w.admin.repository.AdminRepository
import com.code.w.common.model.RequestStatus
import com.code.w.common.model.SupportRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val allRequests: List<SupportRequest> = emptyList(),
    val filteredRequests: List<SupportRequest> = emptyList(),
    val statusCounts: Map<RequestStatus, Int> = emptyMap(),
    val selectedStatusFilter: RequestStatus? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class AdminSupportViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _selectedStatus = MutableStateFlow<RequestStatus?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AdminDashboardUiState> = combine(
        repository.observeAllRequests(),
        _selectedStatus,
        _searchQuery,
        _errorMessage
    ) { allRequests, statusFilter, query, error ->
        // حساب العدادات لكل حالة لعرضها في الشريط العلوي
        val counts = RequestStatus.entries.associateWith { status ->
            allRequests.count { it.statusEnum == status }
        }

        // تطبيق الفلترة حسب الحالة والبحث برقم الهاتف أو رقم العملية المقترح
        val filtered = allRequests.filter { req ->
            val matchesStatus = statusFilter == null || req.statusEnum == statusFilter
            val cleanQuery = query.trim()
            val matchesQuery = cleanQuery.isEmpty() ||
                req.phoneNumber.contains(cleanQuery, ignoreCase = true) ||
                req.id.contains(cleanQuery, ignoreCase = true) ||
                req.ocrSuggestedRefNumber.contains(cleanQuery, ignoreCase = true)
            matchesStatus && matchesQuery
        }

        AdminDashboardUiState(
            allRequests = allRequests,
            filteredRequests = filtered,
            statusCounts = counts,
            selectedStatusFilter = statusFilter,
            searchQuery = query,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardUiState())

    fun onStatusFilterSelected(status: RequestStatus?) {
        _selectedStatus.value = status
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun transitionState(
        request: SupportRequest,
        newStatus: RequestStatus,
        timerMinutes: Long? = null,
        note: String = ""
    ) {
        viewModelScope.launch {
            val result = repository.updateRequestStatus(request, newStatus, timerMinutes, note)
            result.onFailure { _errorMessage.value = it.localizedMessage }
        }
    }
}
