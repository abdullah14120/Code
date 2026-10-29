package com.code.w.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.code.w.admin.repository.AdminSupportRequest
import com.code.w.admin.util.ImageDecoderUtil
import com.code.w.admin.viewmodel.AdminSupportViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AdminDarkBg = Color(0xFF0B141A)
private val AdminSurface = Color(0xFF111B21)
private val AdminSurfaceElevated = Color(0xFF1F2C34)
private val AdminGreenPrimary = Color(0xFF00A884)
private val AdminGreenBright = Color(0xFF10B981)
private val AdminGreenDeep = Color(0xFF064E3B)
private val AdminAmber = Color(0xFFF59E0B)
private val AdminBlue = Color(0xFF0EA5E9)
private val AdminPurple = Color(0xFFA855F7)
private val AdminRed = Color(0xFFEF4444)
private val AdminTextPrimary = Color(0xFFF9FAFB)
private val AdminTextSecondary = Color(0xFF9CA3AF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val colorScheme = darkColorScheme(
                    background = AdminDarkBg,
                    surface = AdminSurface,
                    primary = AdminGreenPrimary,
                    secondary = AdminGreenBright,
                    onBackground = AdminTextPrimary,
                    onSurface = AdminTextPrimary,
                    surfaceVariant = AdminSurfaceElevated
                )
                MaterialTheme(colorScheme = colorScheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AdminDashboardScreen()
                    }
                }
            }
        }
    }
}

private fun copyPhoneToClipboard(context: Context, phone: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Phone", phone))
    Toast.makeText(context, "تم نسخ الرقم: $phone", Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen() {
    val viewModel: AdminSupportViewModel = viewModel()
    val requests by viewModel.requests.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredRequests = remember(requests, searchQuery, selectedFilter) {
        requests.filter { req ->
            val matchesSearch = searchQuery.isBlank() || req.phoneNumber.contains(searchQuery.trim())
            val matchesFilter = when (selectedFilter) {
                "NEW" -> req.status == "SUBMITTED"
                "REVIEW_DEPOSIT" -> req.status == "PRE_APPROVED" || req.status == "APPROVED"
                "PROCESSING" -> req.status == "RECEIPT_SUBMITTED" || req.status == "PROCESSING"
                "DONE" -> req.status == "COMPLETED" || req.status == "REJECTED"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val newCount = requests.count { it.status == "SUBMITTED" }
    val processingCount = requests.count { it.status == "RECEIPT_SUBMITTED" || it.status == "PROCESSING" }

    Scaffold(
        containerColor = AdminDarkBg,
        topBar = {
            Surface(
                color = AdminSurface,
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "لوحة تحكم الإدارة - معالجة كود التحقق",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminTextPrimary
                            )
                            Text(
                                text = "إجمالي الطلبات: ${requests.size} | جديد: $newCount | قيد المعالجة: $processingCount",
                                fontSize = 12.sp,
                                color = AdminGreenBright
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث برقم الهاتف (7xxxxxxxx)...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = AdminGreenBright
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = AdminTextSecondary
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdminGreenBright,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = AdminSurfaceElevated,
                            unfocusedContainerColor = AdminSurfaceElevated
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf(
                            "ALL" to "الكل (${requests.size})",
                            "NEW" to "طلبات جديدة ($newCount)",
                            "REVIEW_DEPOSIT" to "مراجعة وإيداع (${requests.count { it.status == "PRE_APPROVED" || it.status == "APPROVED" }})",
                            "PROCESSING" to "قيد المعالجة ($processingCount)",
                            "DONE" to "مكتمل/مرفوض (${requests.count { it.status == "COMPLETED" || it.status == "REJECTED" }})"
                        )
                        filters.forEach { (key, label) ->
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { selectedFilter = key },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AdminGreenPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = AdminSurfaceElevated,
                                    labelColor = AdminTextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (filteredRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد طلبات مطابقة للعرض حالياً",
                    fontSize = 15.sp,
                    color = AdminTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredRequests, key = { it.id }) { request ->
                    AdminRequestItem(request = request, viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRequestItem(request: AdminSupportRequest, viewModel: AdminSupportViewModel) {
    val context = LocalContext.current
    var requestedAmount by remember(request.bankDetails) { mutableStateOf(request.bankDetails) }
    var adminNotes by remember { mutableStateOf("") }
    var rejectionNote by remember { mutableStateOf("") }
    var showRejectSection by remember { mutableStateOf(false) }
    var depositNote by remember { mutableStateOf("") }
    var showDepositNotConfirmed by remember { mutableStateOf(false) }
    var selectedReviewMins by remember { mutableIntStateOf(30) }
    var selectedProcessingMins by remember { mutableIntStateOf(20) }

    var showImageDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val decodedBitmap = remember(request.receiptImageUrl) {
        ImageDecoderUtil.decodeBase64ToBitmap(request.receiptImageUrl, context)
    }

    val statusInfo = when (request.status) {
        "SUBMITTED" -> Triple("طلب جديد - بانتظار الموافقة أو الرفض", AdminAmber, Color(0xFF451A03))
        "REJECTED" -> Triple("مرفوض", AdminRed, Color(0xFF450A0A))
        "PRE_APPROVED" -> Triple("مقبول - جاري عداد المراجعة", AdminPurple, Color(0xFF3B0764))
        "APPROVED" -> Triple("بانتظار الإيداع من المستخدم", AdminBlue, Color(0xFF082F49))
        "RECEIPT_SUBMITTED" -> Triple("تم إرفاق إشعار الإيداع - بانتظار تأكيدك", AdminAmber, Color(0xFF451A03))
        "PROCESSING" -> Triple("تم تأكيد الإيداع - جاري معالجة المشكلة", AdminGreenBright, AdminGreenDeep)
        "COMPLETED" -> Triple("تم حل المشكلة بنجاح ✓", AdminGreenBright, AdminGreenDeep)
        else -> Triple(request.status, AdminTextSecondary, AdminSurfaceElevated)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, statusInfo.second.copy(alpha = 0.4f)),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AdminSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // رأس البطاقة: رقم الهاتف + زر نسخ + زر حذف
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(AdminGreenBright.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = AdminGreenBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = request.phoneNumber,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = AdminTextPrimary
                            )
                            OutlinedButton(
                                onClick = { copyPhoneToClipboard(context, request.phoneNumber) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, AdminGreenBright.copy(alpha = 0.4f))
                            ) {
                                Text("نسخ", fontSize = 10.sp, color = AdminGreenBright)
                            }
                        }
                        Text(
                            text = request.issueType,
                            fontSize = 12.sp,
                            color = AdminTextSecondary
                        )
                    }
                }

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف الطلب",
                        tint = AdminRed.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // شارة الحالة والوقت المتبقي إن وجد
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = statusInfo.third,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusInfo.second.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = statusInfo.first,
                        color = statusInfo.second,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                if (request.createdAt > 0L) {
                    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.US) }
                    Text(
                        text = sdf.format(Date(request.createdAt)),
                        fontSize = 11.sp,
                        color = AdminTextSecondary
                    )
                }
            }

            if (request.status == "PRE_APPROVED" || request.status == "PROCESSING") {
                if (request.timerEndTime > 0L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AdminLiveTimerBadge(endTime = request.timerEndTime)
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // التحكم حسب المرحلة الحالية للطلب
            when (request.status) {
                "SUBMITTED" -> {
                    Text(
                        text = "1. تحديد مدة عداد المراجعة وقبول الطلب:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 30, 45).forEach { mins ->
                            FilterChip(
                                selected = selectedReviewMins == mins,
                                onClick = { selectedReviewMins = mins },
                                label = { Text("$mins د", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AdminPurple,
                                    selectedLabelColor = Color.White,
                                    containerColor = AdminSurfaceElevated,
                                    labelColor = AdminTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.preApproveRequest(request.id, selectedReviewMins) { success ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "تم قبول الطلب وتشغيل عداد المراجعة ($selectedReviewMins دقيقة)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminGreenPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("قبول الطلب وبدء عداد المراجعة ($selectedReviewMins دقيقة)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // خيار طلب إيداع مباشر أو رفض الطلب مع ملاحظة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showRejectSection = !showRejectSection }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = AdminRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showRejectSection) "إخفاء خيار الرفض" else "رفض الطلب مع كتابة ملاحظة",
                                color = AdminRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (showRejectSection) {
                        RejectRequestSection(
                            rejectionNote = rejectionNote,
                            onNoteChange = { rejectionNote = it },
                            onConfirmReject = {
                                val noteToSend = rejectionNote.trim().ifBlank { "تم رفض الطلب لعدم استيفاء البيانات المطلوبة." }
                                viewModel.rejectRequest(request.id, noteToSend) { success ->
                                    if (success) {
                                        Toast.makeText(context, "تم رفض الطلب وإرسال الملاحظة للمستخدم", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }

                "PRE_APPROVED" -> {
                    Text(
                        text = "طلب الإيداع وإظهار واجهة الإيداع الخضراء للمستخدم:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminGreenBright
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // أزرار سريعة لاختيار المبلغ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("3,000 ريال يمني", "5,000 ريال يمني", "10,000 ريال يمني", "50 ريال سعودي").forEach { preset ->
                            OutlinedButton(
                                onClick = { requestedAmount = preset },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, AdminGreenBright.copy(alpha = 0.4f))
                            ) {
                                Text(preset, fontSize = 11.sp, color = AdminGreenBright)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = requestedAmount,
                        onValueChange = { requestedAmount = it },
                        label = { Text("أدخل المبلغ المطلوب إيداعه (مثال: 5,000 ريال)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdminGreenBright,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = AdminSurfaceElevated,
                            unfocusedContainerColor = AdminSurfaceElevated
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (requestedAmount.isNotBlank()) {
                                viewModel.approveRequest(request.id, requestedAmount.trim()) { success ->
                                    if (success) {
                                        Toast.makeText(context, "تم إظهار واجهة الإيداع للمستخدم", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "يرجى إدخال المبلغ المطلوب أولاً", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminGreenPrimary)
                    ) {
                        Text("إظهار واجهة الإيداع للمستخدم بهذا المبلغ", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(onClick = { showRejectSection = !showRejectSection }) {
                        Text(
                            text = if (showRejectSection) "إغلاق خيار الرفض" else "أو رفض الطلب مع ذكر السبب",
                            color = AdminRed,
                            fontSize = 12.sp
                        )
                    }

                    if (showRejectSection) {
                        RejectRequestSection(
                            rejectionNote = rejectionNote,
                            onNoteChange = { rejectionNote = it },
                            onConfirmReject = {
                                val noteToSend = rejectionNote.trim().ifBlank { "تم رفض الطلب بعد الفحص الفني." }
                                viewModel.rejectRequest(request.id, noteToSend) { success ->
                                    if (success) {
                                        Toast.makeText(context, "تم رفض الطلب وإشعار المستخدم", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }

                "APPROVED" -> {
                    Surface(
                        color = AdminSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "المبلغ المطلوب من المستخدم: ${request.bankDetails}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminGreenBright
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "المستخدم الآن في واجهة الإيداع الخضراء؛ بانتظار مشاركة ملف PDF/صورة الإيداع أو تأكيدك المباشر أدناه:",
                                fontSize = 12.sp,
                                color = AdminTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "زر الأدمن: تأكيد أن الإيداع قد تم وتحويل المستخدم لواجهة المعالجة:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 20, 30).forEach { mins ->
                            FilterChip(
                                selected = selectedProcessingMins == mins,
                                onClick = { selectedProcessingMins = mins },
                                label = { Text("$mins د", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AdminGreenPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = AdminSurfaceElevated,
                                    labelColor = AdminTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.confirmDepositReceived(request.id, selectedProcessingMins) { success ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "تم تأكيد الإيداع وتحويل المستخدم لواجهة جاري معالجة المشكلة",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminGreenPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تأكيد أن الإيداع قد تم (بدء عداد المعالجة $selectedProcessingMins د)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                "RECEIPT_SUBMITTED", "PROCESSING" -> {
                    // عرض ملف الـ PDF أو صورة الإشعار المرفقة من المستخدم إن وجدت
                    if (request.receiptImageUrl.isNotBlank()) {
                        Surface(
                            color = AdminSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AdminGreenBright.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (request.receiptFileType == "PDF") {
                                            "📄 ملف إشعار PDF مرفق من المستخدم"
                                        } else {
                                            "🖼️ صورة إشعار إيداع مرفقة"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = AdminGreenBright
                                    )
                                    if (request.receiptFileName.isNotBlank()) {
                                        Text(
                                            text = request.receiptFileName,
                                            fontSize = 11.sp,
                                            color = AdminTextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (decodedBitmap != null) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(190.dp)
                                            .clickable { showImageDialog = true },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Image(
                                            bitmap = decodedBitmap.asImageBitmap(),
                                            contentDescription = "Receipt Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Text(
                                        text = "🔍 اضغط على الإشعار لتكبيره وقراءة كافة التفاصيل بوضوح",
                                        fontSize = 11.sp,
                                        color = AdminTextSecondary,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                } else {
                                    Text(
                                        text = "⚠️ تعذر عرض معاينة المرفق",
                                        fontSize = 12.sp,
                                        color = AdminRed
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // المستخدم الآن في واجهة انتظار؛ لن تبدأ المعالجة إلا بعد ضغط الأدمن على تأكيد الإيداع
                    if (request.status == "RECEIPT_SUBMITTED") {
                        OutlinedButton(
                            onClick = {
                                viewModel.confirmDepositReceived(request.id, selectedProcessingMins) { success ->
                                    if (success) {
                                        Toast.makeText(context, "تم تأكيد الإيداع وبدء معالجة المشكلة للمستخدم", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AdminGreenBright)
                        ) {
                            Text(
                                text = "تأكيد صحة الإيداع وبدء المعالجة ($selectedProcessingMins د)",
                                color = AdminGreenBright,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showDepositNotConfirmed = !showDepositNotConfirmed },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AdminRed)
                        ) {
                            Text(
                                text = if (showDepositNotConfirmed) "إخفاء" else "لم يتم تأكيد الإيداع (تنبيه المستخدم)",
                                color = AdminRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (showDepositNotConfirmed) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = depositNote,
                                onValueChange = { depositNote = it },
                                label = { Text("ملاحظة تظهر للمستخدم (اختياري)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AdminRed,
                                    unfocusedBorderColor = AdminRed.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val note = depositNote.trim()
                                        .ifBlank { "لم نتمكن من تأكيد وصول الإيداع. تأكد من إتمام التحويل ثم أرفق الإشعار مرة أخرى." }
                                    viewModel.rejectDeposit(request.id, note) { success ->
                                        if (success) {
                                            depositNote = ""
                                            showDepositNotConfirmed = false
                                            Toast.makeText(context, "تم تنبيه المستخدم بعدم تأكيد الإيداع", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdminRed)
                            ) {
                                Text("إرسال تنبيه: لم يتم تأكيد الإيداع", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // قسم إتمام الحل وإرسال علامة الصح والملاحظات المرفقة
                    Text(
                        text = "إتمام الحل النهائي وإرفاق الملاحظات للمستخدم:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminGreenBright
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // قوالب ملاحظات جاهزة لتسهيل عمل الأدمن
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickNotes = listOf(
                            "تم حل المشكلة بنجاح، يرجى الآن طلب الكود عبر رسالة نصية SMS وعدم طلب مكالمة.",
                            "تم تنشيط استقبال المكالمات والرسائل لرقمك بنجاح، يمكنك التسجيل الآن.",
                            "تم فك تعليق الكود لرقم هاتفك بنجاح، يرجى الانتظار 5 دقائق ثم طلب الكود."
                        )
                        quickNotes.forEachIndexed { idx, presetNote ->
                            OutlinedButton(
                                onClick = { adminNotes = presetNote },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, AdminGreenBright.copy(alpha = 0.35f))
                            ) {
                                Text("قالب جاهز ${idx + 1}", fontSize = 11.sp, color = AdminGreenBright)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = adminNotes,
                        onValueChange = { adminNotes = it },
                        label = { Text("اكتب الملاحظات والتعليمات النهائية التي ستظهر مع علامة الصح") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdminGreenBright,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = AdminSurfaceElevated,
                            unfocusedContainerColor = AdminSurfaceElevated
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val finalNotes = adminNotes.trim().ifBlank {
                                "تم حل مشكلة كود التحقق والمكالمة الفائتة لرقم هاتفك بنجاح، يمكنك الآن تفعيل حسابك."
                            }
                            viewModel.completeRequest(request.id, finalNotes) { success ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "تم إظهار علامة الصح والملاحظات للمستخدم بنجاح",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminGreenPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تم حل المشكلة بنجاح (إرسال علامة الصح والملاحظات)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                "COMPLETED" -> {
                    Surface(
                        color = AdminGreenDeep.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AdminGreenBright.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "✔ تم حل المشكلة وإغلاق الطلب بنجاح",
                                color = AdminGreenBright,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "الملاحظات المرسلة للمستخدم: ${request.adminNotes}",
                                fontSize = 13.sp,
                                color = AdminTextPrimary
                            )
                        }
                    }
                }

                "REJECTED" -> {
                    Surface(
                        color = AdminRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AdminRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "✖ تم رفض هذا الطلب",
                                color = AdminRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ملاحظة الرفض: ${request.rejectionReason.ifBlank { request.adminNotes }}",
                                fontSize = 13.sp,
                                color = AdminTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // نافذة تكبير ومعاينة إشعار الـ PDF / الصورة
    if (showImageDialog && decodedBitmap != null) {
        AlertDialog(
            onDismissRequest = { showImageDialog = false },
            confirmButton = {
                TextButton(onClick = { showImageDialog = false }) {
                    Text("إغلاق المعاينة", color = AdminGreenBright, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = "معاينة إشعار الإيداع (${request.phoneNumber})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                ) {
                    Image(
                        bitmap = decodedBitmap.asImageBitmap(),
                        contentDescription = "Full Receipt View",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            containerColor = AdminSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // نافذة تأكيد حذف الطلب
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteRequest(request.id) {
                            Toast.makeText(context, "تم حذف الطلب", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminRed)
                ) {
                    Text("حذف نهائي", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء", color = AdminTextSecondary)
                }
            },
            title = { Text("تأكيد حذف الطلب", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("هل أنت متأكد من حذف طلب الرقم (${request.phoneNumber})؟") },
            containerColor = AdminSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun RejectRequestSection(
    rejectionNote: String,
    onNoteChange: (String) -> Unit,
    onConfirmReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AdminRed.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, AdminRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "تحديد أو كتابة سبب رفض الطلب:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AdminRed
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presetReasons = listOf(
                "الرقم المدخل غير صحيح أو غير مكتمل.",
                "الرقم محظور ويتطلب فك حظر أولاً قبل طلب الكود.",
                "لا توجد مشكلة معلقة على هذا الرقم حالياً."
            )
            presetReasons.forEach { reason ->
                OutlinedButton(
                    onClick = { onNoteChange(reason) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, AdminRed.copy(alpha = 0.4f))
                ) {
                    Text(reason, fontSize = 10.sp, color = AdminTextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = rejectionNote,
            onValueChange = onNoteChange,
            label = { Text("ملاحظة الرفض التي ستظهر للمستخدم") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AdminRed,
                unfocusedBorderColor = AdminRed.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onConfirmReject,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AdminRed)
        ) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("تأكيد رفض الطلب وإرسال الملاحظة", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun AdminLiveTimerBadge(endTime: Long) {
    var timeLeft by remember(endTime) { mutableLongStateOf(0L) }
    LaunchedEffect(endTime) {
        while (true) {
            val diff = endTime - System.currentTimeMillis()
            timeLeft = if (diff > 0) diff / 1000L else 0L
            if (timeLeft <= 0L) break
            delay(1000L)
        }
    }
    val mins = timeLeft / 60
    val secs = timeLeft % 60
    val formatted = String.format(Locale.US, "%02d:%02d", mins, secs)

    Surface(
        color = AdminSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, AdminGreenBright.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "⏱️ العداد التنازلي الظاهر للمستخدم الآن:",
                fontSize = 12.sp,
                color = AdminTextSecondary
            )
            Text(
                text = if (timeLeft > 0) formatted else "انتهى الوقت (00:00)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (timeLeft > 0) AdminGreenBright else AdminAmber
            )
        }
    }
}
