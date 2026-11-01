package com.code.w.admin

import com.code.w.admin.service.AdminMonitorService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.code.w.admin.repository.AdminSupportRequest
import com.code.w.admin.util.ImageDecoderUtil
import com.code.w.admin.viewmodel.AdminSupportViewModel
import com.code.w.common.model.DatabaseKeys
import com.code.w.common.model.RequestStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL
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

data class OcrSuggestion(
    val suggestedAmount: String = "",
    val suggestedRefNumber: String = "",
    val rawText: String = ""
)

data class AuditLogUiItem(
    val adminUid: String = "",
    val toStatus: String = "",
    val note: String = "",
    val timestamp: Long = 0L
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 102)
        }

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener {
                AdminMonitorService.start(this)
            }
        } else {
            AdminMonitorService.start(this)
        }

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

private fun copyTextToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, "تم نسخ $label: $value", Toast.LENGTH_SHORT).show()
}

private fun shareReceiptDetails(
    context: Context,
    request: AdminSupportRequest,
    ocrSuggestion: OcrSuggestion?
) {
    val shareBody = buildString {
        appendLine("بيانات سند الإيداع - رقم الهاتف: ${request.phoneNumber}")
        appendLine("معرف الطلب: ${request.id}")
        if (request.bankDetails.isNotBlank()) {
            appendLine("المبلغ المطلوب: ${request.bankDetails}")
        }
        if (!ocrSuggestion?.suggestedAmount.isNullOrBlank()) {
            appendLine("المبلغ المستخرج (OCR): ${ocrSuggestion?.suggestedAmount}")
        }
        if (!ocrSuggestion?.suggestedRefNumber.isNullOrBlank()) {
            appendLine("رقم العملية المستخرج (OCR): ${ocrSuggestion?.suggestedRefNumber}")
        }
        if (request.receiptImageUrl.startsWith("http", ignoreCase = true)) {
            appendLine("رابط السند في Storage:")
            appendLine(request.receiptImageUrl)
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "سند إيداع للرقم ${request.phoneNumber}")
        putExtra(Intent.EXTRA_TEXT, shareBody)
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة بيانات السند عبر"))
}

private suspend fun runReceiptOcr(bitmap: Bitmap): OcrSuggestion {
    return try {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizer.process(image).await()
        val fullText = visionText.text

        val amountRegex = Regex(
            """(?:Amount|المبلغ|مبلغ|ريال|YER|SAR)[\s:]*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]{3,10})""",
            RegexOption.IGNORE_CASE
        )
        val refRegex = Regex(
            """(?:Ref|Reference|رقم العملية|رقم السند|المرجع|اشعار|إشعار)[\s:#-]*([0-9A-Z]{6,18})""",
            RegexOption.IGNORE_CASE
        )
        val fallbackLongDigits = Regex("""\b([0-9]{7,16})\b""")

        val amount = amountRegex.find(fullText)?.groupValues?.getOrNull(1).orEmpty()
        val ref = refRegex.find(fullText)?.groupValues?.getOrNull(1)
            ?: fallbackLongDigits.find(fullText)?.groupValues?.getOrNull(1).orEmpty()

        OcrSuggestion(
            suggestedAmount = amount,
            suggestedRefNumber = ref,
            rawText = fullText
        )
    } catch (_: Exception) {
        OcrSuggestion()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen() {
    val viewModel: AdminSupportViewModel = viewModel()
    val requests by viewModel.requests.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    // كشف السندات المكررة عبر بصمة SHA-256 أو تطابق محتوى السند
    val duplicateReceiptIds = remember(requests) {
        val byHash = requests
            .filter { it.receiptSha256.isNotBlank() }
            .groupBy { it.receiptSha256 }
            .filterValues { it.size > 1 }
            .values.flatten().map { it.id }

        val byUrl = requests
            .filter { it.receiptImageUrl.isNotBlank() }
            .groupBy { it.receiptImageUrl }
            .filterValues { it.size > 1 }
            .values.flatten().map { it.id }

        (byHash + byUrl).toSet()
    }

    val newCount = requests.count { it.status == RequestStatus.SUBMITTED.name }
    val preApprovedCount = requests.count { it.status == RequestStatus.PRE_APPROVED.name }
    val approvedCount = requests.count { it.status == RequestStatus.APPROVED.name }
    val receiptSubmittedCount = requests.count { it.status == RequestStatus.RECEIPT_SUBMITTED.name }
    val processingCount = requests.count { it.status == RequestStatus.PROCESSING.name }
    val completedCount = requests.count { it.status == RequestStatus.COMPLETED.name }
    val rejectedCount = requests.count { it.status == RequestStatus.REJECTED.name }
    val duplicatesCount = requests.count { it.isDuplicateReceipt || it.id in duplicateReceiptIds }

    val filteredRequests = remember(requests, searchQuery, selectedFilter, duplicateReceiptIds) {
        requests.filter { req ->
            val q = searchQuery.trim()
            val matchesSearch = q.isBlank() ||
                req.phoneNumber.contains(q, ignoreCase = true) ||
                req.id.contains(q, ignoreCase = true) ||
                req.bankDetails.contains(q, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "NEW" -> req.status == RequestStatus.SUBMITTED.name
                "REVIEW_DEPOSIT" -> req.status == RequestStatus.PRE_APPROVED.name || req.status == RequestStatus.APPROVED.name
                "RECEIPT_WAIT" -> req.status == RequestStatus.RECEIPT_SUBMITTED.name
                "PROCESSING" -> req.status == RequestStatus.PROCESSING.name
                "DONE" -> req.status == RequestStatus.COMPLETED.name || req.status == RequestStatus.REJECTED.name
                "DUPLICATES" -> req.isDuplicateReceipt || req.id in duplicateReceiptIds
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

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
                                text = "الإجمالي: ${requests.size} | جديد: $newCount | بانتظار تأكيد السند: $receiptSubmittedCount | معالجة: $processingCount | مكتمل: $completedCount",
                                fontSize = 11.sp,
                                color = AdminGreenBright
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث برقم الهاتف (7xxxxxxxx) أو معرف الطلب أو المبلغ...", fontSize = 13.sp) },
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
                            "NEW" to "جديد ($newCount)",
                            "REVIEW_DEPOSIT" to "مراجعة/إيداع (${preApprovedCount + approvedCount})",
                            "RECEIPT_WAIT" to "سندات بانتظار التأكيد ($receiptSubmittedCount)",
                            "PROCESSING" to "قيد المعالجة ($processingCount)",
                            "DONE" to "مكتمل/مرفوض (${completedCount + rejectedCount})",
                            "DUPLICATES" to "سندات مكررة ($duplicatesCount)"
                        )
                        filters.forEach { (key, label) ->
                            val isDupChip = key == "DUPLICATES" && duplicatesCount > 0
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { selectedFilter = key },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isDupChip) AdminRed else AdminGreenPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = AdminSurfaceElevated,
                                    labelColor = if (isDupChip) AdminRed else AdminTextSecondary
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
                    val isDuplicate = request.isDuplicateReceipt || request.id in duplicateReceiptIds
                    AdminRequestItem(
                        request = request.copy(isDuplicateReceipt = isDuplicate),
                        viewModel = viewModel
                    )
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
    var showAuditLogs by remember { mutableStateOf(false) }

    var decodedBitmap by remember(request.receiptImageUrl) { mutableStateOf<Bitmap?>(null) }
    var isLoadingReceipt by remember(request.receiptImageUrl) { mutableStateOf(false) }
    var ocrSuggestion by remember(request.receiptImageUrl) { mutableStateOf<OcrSuggestion?>(null) }
    var isRunningOcr by remember(request.receiptImageUrl) { mutableStateOf(false) }

    // تحميل السند سواء كان رابط Firebase Storage (https://) أو نص Base64، ثم تشغيل قراءة الـ OCR تلقائياً
    LaunchedEffect(request.receiptImageUrl) {
        if (request.receiptImageUrl.isBlank()) {
            decodedBitmap = null
            ocrSuggestion = null
            return@LaunchedEffect
        }
        isLoadingReceipt = true
        val bmp = withContext(Dispatchers.IO) {
            if (request.receiptImageUrl.startsWith("http", ignoreCase = true)) {
                runCatching {
                    URL(request.receiptImageUrl).openStream().use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }.getOrNull()
            } else {
                ImageDecoderUtil.decodeBase64ToBitmap(request.receiptImageUrl, context)
            }
        }
        decodedBitmap = bmp
        isLoadingReceipt = false

        if (bmp != null) {
            isRunningOcr = true
            ocrSuggestion = runReceiptOcr(bmp)
            isRunningOcr = false
        }
    }

    val statusInfo = when (request.status) {
        RequestStatus.SUBMITTED.name -> Triple("طلب جديد - بانتظار الموافقة أو الرفض", AdminAmber, Color(0xFF451A03))
        RequestStatus.REJECTED.name -> Triple("مرفوض", AdminRed, Color(0xFF450A0A))
        RequestStatus.PRE_APPROVED.name -> Triple("مقبول - جاري عداد المراجعة", AdminPurple, Color(0xFF3B0764))
        RequestStatus.APPROVED.name -> Triple("بانتظار الإيداع من المستخدم", AdminBlue, Color(0xFF082F49))
        RequestStatus.RECEIPT_SUBMITTED.name -> Triple("تم إرفاق إشعار الإيداع - بانتظار تأكيدك", AdminAmber, Color(0xFF451A03))
        RequestStatus.PROCESSING.name -> Triple("تم تأكيد الإيداع - جاري معالجة المشكلة", AdminGreenBright, AdminGreenDeep)
        RequestStatus.COMPLETED.name -> Triple("تم حل المشكلة بنجاح ✓", AdminGreenBright, AdminGreenDeep)
        else -> Triple(request.status, AdminTextSecondary, AdminSurfaceElevated)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    width = if (request.isDuplicateReceipt) 2.dp else 1.dp,
                    color = if (request.isDuplicateReceipt) AdminRed else statusInfo.second.copy(alpha = 0.4f)
                ),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AdminSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // تنبيه السند المكرر إن وجد
            if (request.isDuplicateReceipt) {
                Surface(
                    color = AdminRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AdminRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AdminRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "تحذير أمني: هذا السند مكرر (تم رفعه في طلب آخر بنفس البصمة SHA-256)!",
                            color = AdminRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // رأس البطاقة: رقم الهاتف + زر نسخ + زر سجل الإجراءات + زر حذف
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
                                onClick = { copyTextToClipboard(context, "الرقم", request.phoneNumber) },
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showAuditLogs = !showAuditLogs }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "سجل الإجراءات",
                            tint = if (showAuditLogs) AdminGreenBright else AdminTextSecondary
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الطلب",
                            tint = AdminRed.copy(alpha = 0.8f)
                        )
                    }
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
                    val sdf = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.US) }
                    Text(
                        text = sdf.format(Date(request.createdAt)),
                        fontSize = 11.sp,
                        color = AdminTextSecondary
                    )
                }
            }

            // عرض سجل الإجراءات (Audit Logs) عند الضغط على أيقونة السجل
            if (showAuditLogs) {
                Spacer(modifier = Modifier.height(8.dp))
                RequestAuditLogsBox(requestId = request.id)
            }

            if (request.status == RequestStatus.PRE_APPROVED.name || request.status == RequestStatus.PROCESSING.name) {
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
                RequestStatus.SUBMITTED.name -> {
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

                RequestStatus.PRE_APPROVED.name -> {
                    Text(
                        text = "طلب الإيداع وإظهار واجهة الإيداع الخضراء للمستخدم:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminGreenBright
                    )
                    Spacer(modifier = Modifier.height(8.dp))

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

                RequestStatus.APPROVED.name -> {
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

                RequestStatus.RECEIPT_SUBMITTED.name, RequestStatus.PROCESSING.name -> {
                    // عرض ملف الـ PDF أو صورة الإشعار المرفقة من المستخدم + نتائج قراءة الـ OCR
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
                                    IconButton(
                                        onClick = { shareReceiptDetails(context, request, ocrSuggestion) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "مشاركة السند",
                                            tint = AdminGreenBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // صندوق نتائج القراءة التلقائية للسند عبر ML Kit OCR
                                if (isRunningOcr) {
                                    Text(
                                        text = "⏳ جاري قراءة بيانات السند تلقائياً (OCR)...",
                                        fontSize = 11.sp,
                                        color = AdminAmber,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else if (ocrSuggestion != null &&
                                    (ocrSuggestion!!.suggestedAmount.isNotBlank() || ocrSuggestion!!.suggestedRefNumber.isNotBlank())
                                ) {
                                    Surface(
                                        color = AdminGreenDeep.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, AdminGreenBright.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "🤖 قراءة السند التلقائية (OCR للمقارنة):",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AdminGreenBright
                                                )
                                                Text(
                                                    text = "المبلغ المقترح: ${ocrSuggestion!!.suggestedAmount.ifBlank { "غير واضح" }} | رقم العملية: ${ocrSuggestion!!.suggestedRefNumber.ifBlank { "غير واضح" }}",
                                                    fontSize = 11.sp,
                                                    color = AdminTextPrimary
                                                )
                                            }
                                            if (ocrSuggestion!!.suggestedRefNumber.isNotBlank()) {
                                                TextButton(
                                                    onClick = {
                                                        copyTextToClipboard(
                                                            context,
                                                            "رقم العملية",
                                                            ocrSuggestion!!.suggestedRefNumber
                                                        )
                                                    }
                                                ) {
                                                    Text("نسخ المرجع", fontSize = 10.sp, color = AdminGreenBright)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                when {
                                    isLoadingReceipt -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = AdminGreenBright)
                                        }
                                    }
                                    decodedBitmap != null -> {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(190.dp)
                                                .clickable { showImageDialog = true },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Image(
                                                bitmap = decodedBitmap!!.asImageBitmap(),
                                                contentDescription = "Receipt Preview",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Text(
                                            text = "🔍 اضغط على السند لتكبيره باللمس (Pinch-to-Zoom) أو مشاركته",
                                            fontSize = 11.sp,
                                            color = AdminTextSecondary,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "⚠️ تعذر عرض معاينة المرفق",
                                            fontSize = 12.sp,
                                            color = AdminRed
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (request.status == RequestStatus.RECEIPT_SUBMITTED.name) {
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

                    Text(
                        text = "إتمام الحل النهائي وإرفاق الملاحظات للمستخدم:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminGreenBright
                    )

                    Spacer(modifier = Modifier.height(6.dp))

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

                RequestStatus.COMPLETED.name -> {
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

                RequestStatus.REJECTED.name -> {
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

    // نافذة معاينة السند مع التكبير باللمس (Pinch-to-Zoom) والمشاركة وتنبيه التكرار
    if (showImageDialog && decodedBitmap != null) {
        ZoomableReceiptPreviewDialog(
            bitmap = decodedBitmap!!,
            request = request,
            ocrSuggestion = ocrSuggestion,
            onDismiss = { showImageDialog = false }
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

/**
 * نافذة معاينة السند بالتكبير (Pinch-to-Zoom & Pan) مع زر المشاركة وبيانات الـ OCR
 */
@Composable
fun ZoomableReceiptPreviewDialog(
    bitmap: Bitmap,
    request: AdminSupportRequest,
    ocrSuggestion: OcrSuggestion?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(18.dp),
            color = AdminSurface,
            border = BorderStroke(1.dp, AdminGreenBright.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "معاينة السند (${request.phoneNumber})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminTextPrimary
                    )
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "إعادة ضبط التكبير",
                            tint = AdminGreenBright
                        )
                    }
                }

                if (request.isDuplicateReceipt) {
                    Surface(
                        color = AdminRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AdminRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "⚠️ تنبيه: بصمة هذا السند مكررة في طلب آخر!",
                            color = AdminRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (ocrSuggestion != null &&
                    (ocrSuggestion.suggestedAmount.isNotBlank() || ocrSuggestion.suggestedRefNumber.isNotBlank())
                ) {
                    Text(
                        text = "قراءة OCR: المبلغ (${ocrSuggestion.suggestedAmount.ifBlank { "غير واضح" }}) | العملية (${ocrSuggestion.suggestedRefNumber.ifBlank { "غير واضح" }})",
                        fontSize = 11.sp,
                        color = AdminGreenBright,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full Zoomable Receipt",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { shareReceiptDetails(context, request, ocrSuggestion) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AdminGreenBright)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = AdminGreenBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة السند", color = AdminGreenBright, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminGreenPrimary)
                    ) {
                        Text("إغلاق المعاينة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * مكون عرض سجل الإجراءات (Audit Logs: من أكد الإيداع أو عدّل الحالة ومتى)
 */
@Composable
fun RequestAuditLogsBox(requestId: String) {
    var logs by remember(requestId) { mutableStateOf<List<AuditLogUiItem>>(emptyList()) }

    LaunchedEffect(requestId) {
        val ref = FirebaseDatabase.getInstance()
            .getReference(DatabaseKeys.NODE_REQUESTS)
            .child(requestId)
            .child(DatabaseKeys.FIELD_AUDIT_LOGS)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val parsed = snapshot.children.mapNotNull { child ->
                    val adminUid = child.child("adminUid").getValue(String::class.java) ?: "admin"
                    val toStatus = child.child("toStatus").getValue(String::class.java).orEmpty()
                    val note = child.child("note").getValue(String::class.java).orEmpty()
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    if (toStatus.isBlank()) null else AuditLogUiItem(adminUid, toStatus, note, timestamp)
                }.sortedByDescending { it.timestamp }
                logs = parsed
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    val sdf = remember { SimpleDateFormat("MM/dd hh:mm a", Locale.US) }

    Surface(
        color = AdminSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "📋 سجل إجراءات الأدمن (Audit Log):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AdminGreenBright
            )
            if (logs.isEmpty()) {
                Text(
                    text = "لا توجد إجراءات مسجلة بعد لهذا الطلب.",
                    fontSize = 11.sp,
                    color = AdminTextSecondary
                )
            } else {
                logs.forEach { log ->
                    val statusAr = RequestStatus.fromStringOrDefault(log.toStatus).arabicLabel
                    val timeFormatted = if (log.timestamp > 0L) sdf.format(Date(log.timestamp)) else ""
                    val shortAdmin = if (log.adminUid.length > 6) log.adminUid.take(6) else log.adminUid
                    Text(
                        text = "• [$shortAdmin] حوّل الحالة إلى ($statusAr) - $timeFormatted",
                        fontSize = 11.sp,
                        color = AdminTextPrimary
                    )
                }
            }
        }
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
    var serverTimeOffset by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val offsetRef = FirebaseDatabase.getInstance().getReference(DatabaseKeys.SERVER_TIME_OFFSET)
        offsetRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                serverTimeOffset = snapshot.getValue(Long::class.java) ?: 0L
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    LaunchedEffect(endTime, serverTimeOffset) {
        while (true) {
            val estimatedServerNow = System.currentTimeMillis() + serverTimeOffset
            val diff = endTime - estimatedServerNow
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
                text = "⏱️ العداد التنازلي الظاهر للمستخدم الآن (بتوقيت الخادم):",
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
