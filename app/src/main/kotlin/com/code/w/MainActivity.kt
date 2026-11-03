package com.code.w

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.code.w.common.model.DatabaseKeys
import com.code.w.common.model.ServiceType
import com.code.w.model.SupportRequest
import com.code.w.ui.LiveChatSupportButton
import androidx.compose.runtime.rememberCoroutineScope
import com.code.w.util.PrepareZipResult
import com.code.w.util.ProcessedReceipt
import com.code.w.util.SmartDataRestoreManager
import kotlinx.coroutines.launch
import com.code.w.viewmodel.UserSupportViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// لوحة الألوان الرسمية الاحترافية (اللون الأخضر الرسمي المتناسق مع الواتساب ومصرف العمقي)
private val DarkBackground = Color(0xFF0B141A)
private val DarkSurface = Color(0xFF111B21)
private val DarkSurfaceElevated = Color(0xFF1F2C34)
private val OfficialGreenPrimary = Color(0xFF00A884)
private val OfficialGreenBright = Color(0xFF10B981)
private val OfficialGreenDark = Color(0xFF047857)
private val OfficialGreenDeep = Color(0xFF064E3B)
private val OfficialGreenSoft = Color(0xFFD1FAE5)
private val AmberWarning = Color(0xFFF59E0B)
private val DangerRed = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val SubtleBorder = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
private val OfficialGreenBorder = BorderStroke(1.2.dp, OfficialGreenBright.copy(alpha = 0.45f))

class MainActivity : ComponentActivity() {

    private val incomingSharedUri = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        handleIncomingShareIntent(intent)

        val sharedPreferences = getSharedPreferences("support_prefs", Context.MODE_PRIVATE)
        val savedRequestId = sharedPreferences.getString("last_request_id", null)

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                OfficialGreenTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val sharedUri by incomingSharedUri.collectAsState()
                        AppNavigationRouter(
                            initialRequestId = savedRequestId,
                            sharedUri = sharedUri,
                            onSharedUriConsumed = { incomingSharedUri.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val sharedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            } ?: intent.clipData?.let { clip ->
                if (clip.itemCount > 0) clip.getItemAt(0).uri else null
            }

            if (sharedUri != null) {
                incomingSharedUri.value = sharedUri
            }
        }
    }
}

@Composable
fun OfficialGreenTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        background = DarkBackground,
        surface = DarkSurface,
        primary = OfficialGreenPrimary,
        secondary = OfficialGreenBright,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        surfaceVariant = DarkSurfaceElevated
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "تم نسخ $label: $text", Toast.LENGTH_SHORT).show()
}

/**
 * زر التواصل المباشر مع الدعم الفني عبر نافذة المحادثة الفورية داخل التطبيق
 */
@Composable
fun ContactSupportWaitingButton(
    requestId: String,
    phoneNumber: String,
    statusLabel: String = ""
) {
    if (requestId.isBlank()) return
    LiveChatSupportButton(
        requestId = requestId,
        phoneNumber = phoneNumber
    )
}

@Composable
fun AppNavigationRouter(
    initialRequestId: String?,
    sharedUri: Uri?,
    onSharedUriConsumed: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: UserSupportViewModel = viewModel()
    var currentRequestId by remember { mutableStateOf(initialRequestId) }
    val requestState by viewModel.currentRequest.collectAsState()
    val attachedReceipt by viewModel.attachedReceipt.collectAsState()
    val isSharedFromExternal by viewModel.isSharedFromExternalApp.collectAsState()
    val isProcessingFile by viewModel.isProcessingFile.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    // معالجة أي ملف PDF أو صورة تمت مشاركتها من تطبيقات المصارف الخارجية
    LaunchedEffect(sharedUri) {
        sharedUri?.let { uri ->
            viewModel.processReceiptUri(context, uri, fromExternalShare = true) { success ->
                if (success) {
                    Toast.makeText(
                        context,
                        "تم استلام ملف إشعار الإيداع من تطبيق المصرف بنجاح",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "الملف غير مدعوم أو يتجاوز 5 ميجابايت. يرجى مشاركة ملف PDF أو صورة (JPG/PNG) بحجم أقل من 5MB",
                        Toast.LENGTH_LONG
                    ).show()
                }
                onSharedUriConsumed()
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(DarkBackground)) {
                TopBannerComponent()
                if (currentRequestId != null && requestState != null) {
                    StepperComponent(
                        currentStatus = requestState!!.status,
                        issueType = requestState!!.issueType
                    )
                }
            }
        },
        bottomBar = { BottomFooterComponent() },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(targetState = currentRequestId, label = "MainRouter") { id ->
                if (id == null) {
                    SubmissionScreen(
                        viewModel = viewModel,
                        onSuccess = { createdId ->
                            val prefs = context.getSharedPreferences("support_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("last_request_id", createdId).apply()
                            currentRequestId = createdId
                        }
                    )
                } else {
                    TrackingScreen(
                        requestId = id,
                        viewModel = viewModel,
                        requestState = requestState,
                        attachedReceipt = attachedReceipt,
                        isSharedFromExternal = isSharedFromExternal,
                        isProcessingFile = isProcessingFile,
                        isUploading = isUploading,
                        onClearSession = {
                            val prefs = context.getSharedPreferences("support_prefs", Context.MODE_PRIVATE)
                            prefs.edit().remove("last_request_id").apply()
                            viewModel.resetSessionState()
                            currentRequestId = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TopBannerComponent() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(OfficialGreenBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            OfficialGreenDeep.copy(alpha = 0.65f),
                            DarkSurface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(OfficialGreenBright.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, OfficialGreenBright.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Support Icon",
                        tint = OfficialGreenBright,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مركز المعالجة والتفعيل التقني عن بُعد",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "النظام الرسمي المعتمد لحل مشكلة كود التحقق وتفعيل التطبيقات التابعة لنا عن بُعد",
                        fontSize = 11.sp,
                        color = OfficialGreenSoft.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StepperComponent(
    currentStatus: SupportRequest.Status,
    issueType: String = SupportRequest.DEFAULT_ISSUE_TYPE
) {
    val serviceType = remember(issueType) { ServiceType.fromTitle(issueType) }
    val steps = serviceType.stepperSteps
    val isRejected = currentStatus == SupportRequest.Status.REJECTED

    val activeIndex = if (serviceType == ServiceType.REMOTE_APP_ACTIVATION) {
        when (currentStatus) {
            SupportRequest.Status.SUBMITTED, SupportRequest.Status.REJECTED -> 0
            SupportRequest.Status.PRE_APPROVED -> 1
            SupportRequest.Status.APPROVED, SupportRequest.Status.RECEIPT_SUBMITTED -> 2
            SupportRequest.Status.PROCESSING -> 3
            SupportRequest.Status.COMPLETED -> steps.lastIndex
        }
    } else if (serviceType.requiresDeposit) {
        when (currentStatus) {
            SupportRequest.Status.SUBMITTED -> 0
            SupportRequest.Status.REJECTED -> 0
            SupportRequest.Status.PRE_APPROVED -> 1
            SupportRequest.Status.APPROVED -> 2
            SupportRequest.Status.RECEIPT_SUBMITTED -> 2
            SupportRequest.Status.PROCESSING -> 3
            SupportRequest.Status.COMPLETED -> steps.lastIndex
        }
    } else {
        when (currentStatus) {
            SupportRequest.Status.SUBMITTED, SupportRequest.Status.REJECTED -> 0
            SupportRequest.Status.PRE_APPROVED -> 1
            SupportRequest.Status.APPROVED,
            SupportRequest.Status.RECEIPT_SUBMITTED,
            SupportRequest.Status.PROCESSING -> 2
            SupportRequest.Status.COMPLETED -> steps.lastIndex
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(SubtleBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, step ->
                val isCompleted = !isRejected && (index < activeIndex || currentStatus == SupportRequest.Status.COMPLETED)
                val isCurrent = index == activeIndex
                val stepColor = when {
                    isRejected && isCurrent -> DangerRed
                    isCompleted || isCurrent -> OfficialGreenBright
                    else -> Color(0xFF374151)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = index < steps.lastIndex)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    when {
                                        isCompleted -> OfficialGreenBright
                                        isCurrent -> stepColor.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    },
                                    CircleShape
                                )
                                .border(1.5.dp, stepColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else if (isRejected && isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = DangerRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text(
                                    text = (index + 1).toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) stepColor else TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRejected && isCurrent) "مرفوض" else step,
                            fontSize = 10.sp,
                            fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isRejected && isCurrent -> DangerRed
                                isCurrent -> TextPrimary
                                isCompleted -> OfficialGreenBright
                                else -> TextSecondary
                            }
                        )
                    }

                    if (index < steps.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .padding(bottom = 14.dp),
                            thickness = 1.5.dp,
                            color = if (index < activeIndex && !isRejected) OfficialGreenBright else Color(0xFF2A3942)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PulseIndicator(color: Color, sizeDp: Int = 18) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseProgress"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(modifier = Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color,
                radius = (size.minDimension / 2) * progress,
                alpha = alpha,
                style = Stroke(2.dp.toPx())
            )
            drawCircle(color = color, radius = 4.5.dp.toPx())
        }
    }
}

@Composable
fun BottomFooterComponent() {
    Surface(
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Support",
                    tint = OfficialGreenBright,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "الدعم الفني المباشر: ${FirebaseRemoteConfig.getInstance().getString("support_whatsapp_number").ifBlank { "770034578" }}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "جميع الحقوق محفوظة وكالة ناسا - عبدالله التميمي © 2026",
                fontSize = 10.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SubmissionScreen(viewModel: UserSupportViewModel, onSuccess: (String) -> Unit) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    // القيمة الافتراضية null: لا يوجد خيار محدد تلقائياً ويجب على المستخدم الاختيار بنفسه
    var selectedService by remember { mutableStateOf<ServiceType?>(null) }
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    val isStep1NeedService = selectedService == null
    val isStep2NeedPhone = selectedService != null && phone.length < 9
    val isStep3ReadyToSubmit = selectedService != null && phone.length == 9

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(OfficialGreenBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(OfficialGreenBright.copy(alpha = 0.14f), CircleShape)
                        .border(1.5.dp, OfficialGreenBright.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = OfficialGreenBright,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = selectedService?.title ?: "مرحباً بك في بوابة الخدمات التقنية",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = selectedService?.subtitle
                        ?: "اتبع السهم التوضيحي خطوة بخطوة: اختر نوع الخدمة أولاً ثم اكتب رقم هاتفك.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // =========================================================
                // السهم المتحرك للخطوة 1: يظهر فقط عندما لم يختر المستخدم أي خدمة بعد
                // =========================================================
                AnimatedVisibility(
                    visible = isStep1NeedService,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    AnimatedStepGuideTooltip(
                        stepBadge = "الخطوة 1",
                        message = "اضغط على نوع الخدمة التي تريدها من الخيارات أدناه حتى تفتح لك الخطوة التالية",
                        accentColor = AmberWarning
                    )
                }

                // قائمة اختيار نوع الخدمة (بدون تحديد افتراضي)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. اختر نوع الخدمة المطلوبة:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedService != null) OfficialGreenBright else AmberWarning
                        )
                        if (selectedService != null) {
                            Text(
                                text = "✓ تم الاختيار",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OfficialGreenBright
                            )
                        }
                    }

                    ServiceType.entries.forEach { service ->
                        val isSelected = selectedService == service
                        Surface(
                            color = if (isSelected) OfficialGreenDeep.copy(alpha = 0.6f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else if (isStep1NeedService) 1.2.dp else 1.dp,
                                color = when {
                                    isSelected -> OfficialGreenBright
                                    isStep1NeedService -> AmberWarning.copy(alpha = 0.5f)
                                    else -> Color.White.copy(alpha = 0.1f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedService = service }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // مربع علامة الصح الواضح للمستخدم العادي
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (isSelected) OfficialGreenBright else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSelected) OfficialGreenBright else TextSecondary,
                                            shape = RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = service.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = service.subtitle,
                                        fontSize = 11.sp,
                                        color = if (isSelected) OfficialGreenSoft.copy(alpha = 0.9f) else TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // =========================================================
                // السهم المتحرك للخطوة 2: يظهر تلقائياً فوق حقل الرقم فور اختيار الخدمة
                // =========================================================
                AnimatedVisibility(
                    visible = isStep2NeedPhone,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    AnimatedStepGuideTooltip(
                        stepBadge = "الخطوة 2",
                        message = "الآن اكتب رقم هاتفك المكون من 9 أرقام (يبدأ بالرقم 7) في الخانة أدناه",
                        accentColor = OfficialGreenBright
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        if (digitsOnly.length <= 9 && (digitsOnly.isEmpty() || digitsOnly.startsWith("7"))) {
                            phone = digitsOnly
                        }
                    },
                    enabled = selectedService != null,
                    label = {
                        Text(
                            if (selectedService == null) {
                                "اختر نوع الخدمة أعلاه أولاً لتفعيل خانة الرقم"
                            } else {
                                "2. رقم الهاتف (يبدأ بـ 7)"
                            }
                        )
                    },
                    placeholder = { Text("7xxxxxxxx") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = if (selectedService != null) OfficialGreenBright else TextSecondary
                        )
                    },
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when {
                                    selectedService == null -> "⚠️ يجب اختيار الخدمة أولاً"
                                    phone.length == 9 -> "✓ رقم الهاتف مكتمل وصحيح"
                                    else -> "أدخل 9 أرقام تبدأ بـ 7"
                                },
                                fontSize = 11.sp,
                                color = when {
                                    selectedService == null -> AmberWarning
                                    phone.length == 9 -> OfficialGreenBright
                                    else -> TextSecondary
                                }
                            )
                            Text(
                                text = "${phone.length} / 9",
                                fontSize = 11.sp,
                                color = if (phone.length == 9) OfficialGreenBright else TextSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OfficialGreenBright,
                        unfocusedBorderColor = if (isStep2NeedPhone) OfficialGreenBright.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f),
                        disabledBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        disabledContainerColor = DarkSurfaceElevated.copy(alpha = 0.5f),
                        focusedLabelColor = OfficialGreenBright,
                        unfocusedLabelColor = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // =========================================================
                // السهم المتحرك للخطوة 3: يظهر فوق زر الإرسال فور اكتمال اختيار الخدمة والـ 9 أرقام
                // =========================================================
                AnimatedVisibility(
                    visible = isStep3ReadyToSubmit && !isSubmitting,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    AnimatedStepGuideTooltip(
                        stepBadge = "الخطوة 3",
                        message = "اكتملت البيانات! اضغط على الزر الأخضر أدناه لإرسال الطلب",
                        accentColor = OfficialGreenBright
                    )
                }

                Button(
                    onClick = {
                        val chosenService = selectedService ?: return@Button
                        if (phone.length == 9 && phone.startsWith("7")) {
                            viewModel.sendSupportRequest(
                                phoneNumber = phone,
                                issueType = chosenService.title,
                                onSuccess = onSuccess,
                                onFailure = {
                                    Toast.makeText(
                                        context,
                                        "تعذر إرسال الطلب، يرجى التحقق من اتصال الإنترنت",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .animateContentSize(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OfficialGreenPrimary,
                        disabledContainerColor = Color(0xFF1F2C34)
                    ),
                    enabled = !isSubmitting && isStep3ReadyToSubmit
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("جاري إرسال الطلب...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            text = when {
                                selectedService == null -> "اختر نوع الخدمة أولاً للمتابعة"
                                phone.length < 9 -> "أكمل إدخال رقم الهاتف (9 أرقام)"
                                else -> "متابعة وإرسال طلب المعالجة ✓"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStep3ReadyToSubmit) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        UserRequestsHistoryCard(onSelectRequest = onSuccess)
    }
}

@Composable
fun UserRequestsHistoryCard(onSelectRequest: (String) -> Unit) {
    var historyList by remember { mutableStateOf<List<SupportRequest>>(emptyList()) }

    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: runCatching {
            auth.signInAnonymously()
            null
        }.getOrNull() ?: return@LaunchedEffect

        val ref = FirebaseDatabase.getInstance().getReference(DatabaseKeys.NODE_REQUESTS)
            .orderByChild(DatabaseKeys.FIELD_OWNER_UID)
            .equalTo(uid)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { snap ->
                    val id = snap.child(DatabaseKeys.FIELD_ID).getValue(String::class.java) ?: snap.key.orEmpty()
                    val phone = snap.child(DatabaseKeys.FIELD_PHONE_NUMBER).getValue(String::class.java).orEmpty()
                    val issueType = snap.child(DatabaseKeys.FIELD_ISSUE_TYPE).getValue(String::class.java)
                        ?: SupportRequest.DEFAULT_ISSUE_TYPE
                    val statusStr = snap.child(DatabaseKeys.FIELD_STATUS).getValue(String::class.java) ?: "SUBMITTED"
                    val status = runCatching { SupportRequest.Status.valueOf(statusStr) }
                        .getOrDefault(SupportRequest.Status.SUBMITTED)
                    val createdAt = snap.child(DatabaseKeys.FIELD_CREATED_AT).getValue(Long::class.java) ?: 0L
                    if (phone.isBlank()) null else SupportRequest(
                        id = id,
                        phoneNumber = phone,
                        issueType = issueType,
                        status = status,
                        createdAt = createdAt
                    )
                }.sortedByDescending { it.createdAt }
                historyList = list
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    if (historyList.isNotEmpty()) {
        val sdf = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.US) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(SubtleBorder, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "سجل طلباتك السابقة (${historyList.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OfficialGreenBright
                )
                historyList.take(5).forEach { item ->
                    Surface(
                        color = DarkSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRequest(item.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.phoneNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text(item.issueType, fontSize = 11.sp, color = OfficialGreenSoft)
                                if (item.createdAt > 0L) {
                                    Text(sdf.format(Date(item.createdAt)), fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                            Text(
                                text = when (item.status) {
                                    SupportRequest.Status.COMPLETED -> "مكتمل ✓"
                                    SupportRequest.Status.REJECTED -> "مرفوض ✖"
                                    else -> "قيد المتابعة ←"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (item.status) {
                                    SupportRequest.Status.COMPLETED -> OfficialGreenBright
                                    SupportRequest.Status.REJECTED -> DangerRed
                                    else -> AmberWarning
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingScreen(
    requestId: String,
    viewModel: UserSupportViewModel,
    requestState: SupportRequest?,
    attachedReceipt: ProcessedReceipt?,
    isSharedFromExternal: Boolean,
    isProcessingFile: Boolean,
    isUploading: Boolean,
    onClearSession: () -> Unit
) {
    LaunchedEffect(requestId) {
        viewModel.startObservingRequest(requestId)
    }

    Crossfade(targetState = requestState?.status, label = "StatusRouter") { status ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                SupportRequest.Status.SUBMITTED -> {
                    WaitingApprovalScreen(
                        request = requestState,
                        attachedReceipt = attachedReceipt,
                        onCancelSession = onClearSession
                    )
                }

                SupportRequest.Status.REJECTED -> {
                    RejectedScreen(
                        request = requestState,
                        onClearSession = onClearSession
                    )
                }

                SupportRequest.Status.PRE_APPROVED -> {
                    val isRemoteActivationService =
                        requestState?.issueType == ServiceType.REMOTE_APP_ACTIVATION.title

                    if (isRemoteActivationService) {
                        RemoteAppActivationWizardScreen(
                            requestId = requestId,
                            request = requestState,
                            viewModel = viewModel,
                            isSubmittingStep = isUploading
                        )
                    } else {
                        ReviewTimerScreen(
                            request = requestState,
                            attachedReceipt = attachedReceipt
                        )
                    }
                }

                SupportRequest.Status.APPROVED -> {
                    val isRemoteActivationService =
                        requestState?.issueType == ServiceType.REMOTE_APP_ACTIVATION.title

                    if (isRemoteActivationService && !requestState?.activationZipUrl.isNullOrBlank()) {
                        RemoteZipRestoreScreen(request = requestState)
                    } else {
                        OfficialDepositScreen(
                            requestId = requestId,
                            request = requestState,
                            viewModel = viewModel,
                            attachedReceipt = attachedReceipt,
                            isSharedFromExternal = isSharedFromExternal,
                            isProcessingFile = isProcessingFile,
                            isUploading = isUploading
                        )
                    }
                }

                SupportRequest.Status.RECEIPT_SUBMITTED -> {
                    DepositUnderReviewScreen(
                        request = requestState
                    )
                }

                SupportRequest.Status.PROCESSING -> {
                    val isRemoteActivationService =
                        requestState?.issueType == ServiceType.REMOTE_APP_ACTIVATION.title

                    if (isRemoteActivationService && !requestState?.activationZipUrl.isNullOrBlank()) {
                        RemoteZipRestoreScreen(request = requestState)
                    } else {
                        ProcessingProblemScreen(
                            request = requestState
                        )
                    }
                }

                SupportRequest.Status.COMPLETED -> {
                    CompletedSuccessScreen(
                        request = requestState,
                        onEndSession = onClearSession
                    )
                }

                null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = OfficialGreenBright,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "جاري الاتصال بالسيرفر ومزامنة حالة الطلب...",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// 1. واجهة الانتظار الأولية بعد إدخال رقم الهاتف
@Composable
fun WaitingApprovalScreen(
    request: SupportRequest?,
    attachedReceipt: ProcessedReceipt?,
    onCancelSession: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(SubtleBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(AmberWarning.copy(alpha = 0.12f), CircleShape)
                    .border(1.5.dp, AmberWarning.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AmberWarning,
                    modifier = Modifier.size(54.dp),
                    strokeWidth = 3.dp
                )
                PulseIndicator(color = AmberWarning, sizeDp = 22)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "في انتظار مراجعة وموافقة الإدارة",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "تم استلام طلبك (${request?.issueType ?: SupportRequest.DEFAULT_ISSUE_TYPE})، ويقوم المختص حالياً بفحص رقم الهاتف:",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OfficialGreenBright.copy(alpha = 0.3f))
            ) {
                Text(
                    text = request?.phoneNumber.orEmpty(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OfficialGreenBright,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }

            if (attachedReceipt != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = OfficialGreenDeep.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    border = OfficialGreenBorder
                ) {
                    Text(
                        text = "📄 تم تجهيز ملف الإشعار (${attachedReceipt.fileName}) وسيظهر تلقائياً عند فتح نافذة الإيداع.",
                        fontSize = 12.sp,
                        color = OfficialGreenSoft,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = DarkSurfaceElevated.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "سيتم تحديث هذه الشاشة تلقائياً فور قبول الطلب أو الرد من قِبل الإدارة دون الحاجة لإغلاق التطبيق.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ContactSupportWaitingButton(
                requestId = request?.id.orEmpty(),
                phoneNumber = request?.phoneNumber.orEmpty(),
                statusLabel = "بانتظار مراجعة وموافقة الإدارة"
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = onCancelSession) {
                Text(
                    text = "تعديل رقم الهاتف / إلغاء الطلب",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

// 2. واجهة رفض الطلب مع عرض ملاحظة الرفض من الأدمن
@Composable
fun RejectedScreen(
    request: SupportRequest?,
    onClearSession: () -> Unit
) {
    val reason = request?.rejectionReason?.takeIf { it.isNotBlank() }
        ?: request?.adminNotes?.takeIf { it.isNotBlank() }
        ?: "تعذر قبول الطلب للرقم المدخل، يرجى التحقق من البيانات والمحاولة مجدداً."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.2.dp, DangerRed.copy(alpha = 0.5f)), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(DangerRed.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, DangerRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Rejected",
                    tint = DangerRed,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "اعتذار، تم رفض الطلب",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DangerRed
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "رقم الهاتف: ${request?.phoneNumber.orEmpty()}",
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                color = DangerRed.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "سبب الرفض / ملاحظة الإدارة:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reason,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ContactSupportWaitingButton(
                requestId = request?.id.orEmpty(),
                phoneNumber = request?.phoneNumber.orEmpty(),
                statusLabel = "مرفوض"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClearSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OfficialGreenPrimary)
            ) {
                Text(
                    text = "إنهاء الجلسة وتقديم طلب جديد",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// 3. واجهة قبول الطلب ومراجعته على شكل عداد انتظار توقيت
@Composable
fun ReviewTimerScreen(
    request: SupportRequest?,
    attachedReceipt: ProcessedReceipt?
) {
    val endTime = request?.timerEndTime ?: 0L
    val totalSeconds = (request?.timerTotalSeconds ?: 1800L).coerceAtLeast(60L)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(OfficialGreenBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = OfficialGreenDeep.copy(alpha = 0.6f),
                shape = RoundedCornerShape(50),
                border = OfficialGreenBorder
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = OfficialGreenBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "تم قبول طلبك بنجاح",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OfficialGreenSoft
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "جاري مراجعة طلبك وفحص الرقم",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "يتم الآن فحص مسار الخدمة لرقم الهاتف (${request?.phoneNumber.orEmpty()}) وتجهيز المتطلبات...",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            ProfessionalCountdownWidget(
                endTime = endTime,
                totalSeconds = totalSeconds,
                labelText = "عداد انتظار المراجعة"
            )

            if (attachedReceipt != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = OfficialGreenDeep.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    border = OfficialGreenBorder
                ) {
                    Text(
                        text = "📄 تم استلام ملف الإشعار (${attachedReceipt.fileName}) وسيتم إرفاقه فور انتقال الطلب لخطوة الإيداع.",
                        fontSize = 12.sp,
                        color = OfficialGreenSoft,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PulseIndicator(color = OfficialGreenBright, sizeDp = 16)
                    Text(
                        text = "يرجى الانتظار؛ سيتم تحويلك تلقائياً إلى الخطوة التالية فور انتهاء المراجعة الفنية.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            ContactSupportWaitingButton(
                requestId = request?.id.orEmpty(),
                phoneNumber = request?.phoneNumber.orEmpty(),
                statusLabel = "عداد انتظار المراجعة"
            )
        }
    }
}

// 4. واجهة الإيداع الاحترافية باللون الأخضر الرسمي مع دعم استقبال ملفات PDF والصور
@Composable
fun OfficialDepositScreen(
    requestId: String,
    request: SupportRequest?,
    viewModel: UserSupportViewModel,
    attachedReceipt: ProcessedReceipt?,
    isSharedFromExternal: Boolean,
    isProcessingFile: Boolean,
    isUploading: Boolean
) {
    val context = LocalContext.current
    var showPreviewDialog by remember { mutableStateOf(false) }

    // منتقي ملفات PDF أو الصور من الجهاز
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.processReceiptUri(context, it, fromExternalShare = false) { success ->
                if (!success) {
                    Toast.makeText(
                        context,
                        "حجم الملف يتجاوز 5 ميجابايت أو صيغته غير مدعومة (المسموح: PDF أو صورة)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // منتقي الصور السريع من المعرض
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.processReceiptUri(context, it, fromExternalShare = false) { success ->
                if (!success) {
                    Toast.makeText(
                        context,
                        "حجم الملف يتجاوز 5 ميجابايت أو صيغته غير مدعومة (المسموح: PDF أو صورة)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val amountText = request?.bankDetails?.takeIf { it.isNotBlank() } ?: "يحدد من الإدارة"
    val remoteConfig = remember { FirebaseRemoteConfig.getInstance() }
    var bankName by remember {
        mutableStateOf(remoteConfig.getString("deposit_bank_name").ifBlank { "شركة العمقي وإخوانه للصرافة" })
    }
    var accountName by remember {
        mutableStateOf(remoteConfig.getString("deposit_account_name").ifBlank { "عبدالله سعيد عبدالله علاوه التميمي" })
    }
    var accountNumber by remember {
        mutableStateOf(remoteConfig.getString("deposit_account_number").ifBlank { "254293617" })
    }

    LaunchedEffect(Unit) {
        remoteConfig.setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = 3600 })
        remoteConfig.fetchAndActivate().addOnSuccessListener {
            bankName = remoteConfig.getString("deposit_bank_name").ifBlank { "شركة العمقي وإخوانه للصرافة" }
            accountName = remoteConfig.getString("deposit_account_name").ifBlank { "عبدالله سعيد عبدالله علاوه التميمي" }
            accountNumber = remoteConfig.getString("deposit_account_number").ifBlank { "254293617" }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // تنبيه عدم تأكيد الإيداع من الإدارة (يظهر عندما يعيد الأدمن الطلب لخطوة الإيداع)
        val depositNotConfirmedNote = request?.rejectionReason?.takeIf { it.isNotBlank() }
        if (depositNotConfirmedNote != null) {
            Surface(
                color = DangerRed.copy(alpha = 0.10f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚠️ لم يتم تأكيد الإيداع",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = depositNotConfirmedNote,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يرجى التأكد من الإيداع وإرفاق إشعار صحيح مرة أخرى.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // البطاقة العلوية باللون الأخضر الرسمي لتوضيح متطلبات إتمام الطلب
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.5.dp, OfficialGreenBright), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = OfficialGreenDeep)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                OfficialGreenDark,
                                OfficialGreenDeep
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "تأكيد إيداع رسوم المعالجة الفنية",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "لرقم الهاتف: ${request?.phoneNumber.orEmpty()}",
                            fontSize = 12.sp,
                            color = OfficialGreenSoft
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "حتى يتم حل المشكلة بنجاح وإتمام الطلب، يرجى إيداع مبلغ ($amountText) على الحساب الموضح بالواجهة أدناه، ثم مشاركة إشعار الإيداع (ملف PDF أو صورة):",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // بطاقة الحساب المصرفي (شركة العمقي وإخوانه للصرافة) باللون الأخضر الرسمي
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(OfficialGreenBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val logoResId = remember {
                            context.resources.getIdentifier("bank_logo", "drawable", context.packageName)
                        }
                        if (logoResId != 0) {
                            Image(
                                painter = painterResource(id = logoResId),
                                contentDescription = "شعار مصرف العمقي",
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, OfficialGreenBright.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            )
                        }
                        Column {
                            Text(
                                text = bankName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OfficialGreenBright
                            )
                            Text(
                                text = "حساب معتمد للتحويل المباشر",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        color = OfficialGreenBright.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, OfficialGreenBright.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "رسمي ✓",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OfficialGreenBright,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = Color.White.copy(alpha = 0.08f)
                )

                // اسم صاحب الحساب مع زر النسخ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "اسم صاحب الحساب:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = accountName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    OutlinedButton(
                        onClick = { copyToClipboard(context, "اسم الحساب", accountName) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, OfficialGreenBright.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("نسخ الاسم", fontSize = 11.sp, color = OfficialGreenBright)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // رقم الحساب مع زر نسخ بارز
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, OfficialGreenBright.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "رقم حساب العمقي:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = accountNumber,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = OfficialGreenBright,
                                letterSpacing = 1.sp
                            )
                        }
                        Button(
                            onClick = { copyToClipboard(context, "رقم الحساب", accountNumber) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OfficialGreenPrimary)
                        ) {
                            Text("نسخ الرقم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // المبلغ المطلوب إيداعه
                Surface(
                    color = OfficialGreenDeep.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, OfficialGreenBright),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "المبلغ المطلوب إيداعه لإتمام الطلب:",
                                fontSize = 12.sp,
                                color = OfficialGreenSoft
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = amountText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = OfficialGreenBright,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // قسم تلقي ملفات PDF أو الصور (مشاركة من تطبيقات المصارف أو إرفاق مباشر)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(SubtleBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = OfficialGreenBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "تأكيد الإيداع (ملف PDF أو صورة الإشعار)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "يمكنك مشاركة إشعار الإيداع بصيغة PDF مباشرة من تطبيق المصرف إلى تطبيقنا عبر زر (مشاركة)، أو اختيار ملف الـ PDF / الصورة من جهازك:",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (isProcessingFile) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = OfficialGreenBright,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Text("جاري قراءة ومعالجة ملف إشعار الإيداع...", fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                }

                // عرض معاينة الملف المرفق أو المشارك من تطبيق المصرف
                if (attachedReceipt != null) {
                    Surface(
                        color = OfficialGreenDeep.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, OfficialGreenBright),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = OfficialGreenBright,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (attachedReceipt.fileType == "PDF") "PDF" else "صورة",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (isSharedFromExternal) "تم الاستلام عبر المشاركة من تطبيق المصرف" else "تم تجهيز ملف الإشعار",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OfficialGreenSoft
                                        )
                                        Text(
                                            text = attachedReceipt.fileName,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                                TextButton(onClick = { viewModel.clearAttachedReceipt() }) {
                                    Text("تغيير", fontSize = 12.sp, color = DangerRed)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clickable { showPreviewDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Image(
                                    bitmap = attachedReceipt.previewBitmap.asImageBitmap(),
                                    contentDescription = "معاينة إشعار الإيداع",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Text(
                                text = "🔍 اضغط على المعاينة لتكبير الإشعار والتأكد من وضوحه",
                                fontSize = 11.sp,
                                color = OfficialGreenSoft,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // أزرار اختيار ملف PDF أو صورة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            documentLauncher.launch(arrayOf("application/pdf", "image/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OfficialGreenBright.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = TextPrimary
                        )
                    ) {
                        Text(
                            text = "📄 اختيار ملف PDF",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OfficialGreenBright
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            imageLauncher.launch("image/*")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = TextPrimary
                        )
                    ) {
                        Text(
                            text = "🖼️ اختيار صورة",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.confirmAndUploadReceipt(requestId) { success ->
                            if (success) {
                                Toast.makeText(
                                    context,
                                    "تم إرسال إشعار الإيداع، بانتظار تأكيد الإدارة",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "فشل إرسال الإشعار، يرجى المحاولة مرة أخرى",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = attachedReceipt != null && !isUploading && !isProcessingFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OfficialGreenPrimary,
                        disabledContainerColor = Color(0xFF1F2C34)
                    )
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("جاري رفع وتأكيد الإيداع...", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إرسال إشعار الإيداع للمراجعة",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                ContactSupportWaitingButton(
                    requestId = requestId,
                    phoneNumber = request?.phoneNumber.orEmpty(),
                    statusLabel = "خطوة الإيداع"
                )
            }
        }
    }

    if (showPreviewDialog && attachedReceipt != null) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            confirmButton = {
                TextButton(onClick = { showPreviewDialog = false }) {
                    Text("إغلاق", color = OfficialGreenBright, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = "معاينة إشعار الإيداع (${attachedReceipt.fileName})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                ) {
                    Image(
                        bitmap = attachedReceipt.previewBitmap.asImageBitmap(),
                        contentDescription = "Full Receipt Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// 4.5 واجهة انتظار تأكيد الأدمن للإيداع (بعد إرفاق السند وقبل بدء المعالجة)
@Composable
fun DepositUnderReviewScreen(request: SupportRequest?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.2.dp, AmberWarning.copy(alpha = 0.6f)), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = AmberWarning,
                modifier = Modifier.size(46.dp),
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "تم استلام إشعار الإيداع",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "بانتظار تأكيد الإدارة للإيداع للرقم (${request?.phoneNumber.orEmpty()}). ستبدأ معالجة المشكلة تلقائياً فور التأكد من صحة الإيداع.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                color = AmberWarning.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⏳ يرجى إبقاء هذه الصفحة مفتوحة، سيتم تحديث الحالة تلقائياً.",
                    fontSize = 12.sp,
                    color = AmberWarning,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            ContactSupportWaitingButton(
                requestId = request?.id.orEmpty(),
                phoneNumber = request?.phoneNumber.orEmpty(),
                statusLabel = "بانتظار تأكيد الإيداع"
            )
        }
    }
}

// 5. واجهة "جاري معالجة المشكلة / التفعيل" الاحترافية مع عداد التوقيت
@Composable
fun ProcessingProblemScreen(request: SupportRequest?) {
    val isConfirmedByAdmin = request?.status == SupportRequest.Status.PROCESSING
    val endTime = request?.timerEndTime ?: 0L
    val totalSeconds = (request?.timerTotalSeconds ?: 1200L).coerceAtLeast(60L)
    val currentService = remember(request?.issueType) {
        ServiceType.fromTitle(request?.issueType)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(OfficialGreenBorder, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = OfficialGreenDeep.copy(alpha = 0.65f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, OfficialGreenBright)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = OfficialGreenBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (currentService == ServiceType.REMOTE_APP_ACTIVATION) {
                            "تم استلام بيانات التفعيل وتثبيت الحزمة ✓"
                        } else if (isConfirmedByAdmin) {
                            "تم تأكيد الإيداع من قِبل الإدارة ✓"
                        } else {
                            "تم استلام إشعار الإيداع بنجاح ✓"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (currentService == ServiceType.REMOTE_APP_ACTIVATION) {
                    "جاري استكمال التفعيل عن بُعد"
                } else {
                    "جاري معالجة المشكلة لرقم هاتفك"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${currentService.subtitle} للرقم (${request?.phoneNumber.orEmpty()})",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            ProfessionalCountdownWidget(
                endTime = endTime,
                totalSeconds = totalSeconds,
                labelText = "الوقت المتبقي لإتمام العملية"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // خطوات المعالجة الخاصة بالخدمة المحددة ديناميكياً
            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                border = SubtleBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    currentService.processingSteps.forEachIndexed { index, stepTitle ->
                        ProcessingStepRow(
                            title = stepTitle,
                            isDone = index == 0,
                            isInProgress = index == 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            ContactSupportWaitingButton(
                requestId = request?.id.orEmpty(),
                phoneNumber = request?.phoneNumber.orEmpty(),
                statusLabel = "جاري المعالجة"
            )
        }
    }
}

@Composable
fun ProcessingStepRow(title: String, isDone: Boolean, isInProgress: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when {
            isDone -> {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(OfficialGreenBright, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            isInProgress -> {
                PulseIndicator(color = OfficialGreenBright, sizeDp = 22)
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(1.5.dp, Color(0xFF4B5563), CircleShape)
                )
            }
        }
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isDone || isInProgress) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isDone -> OfficialGreenSoft
                isInProgress -> TextPrimary
                else -> TextSecondary
            }
        )
    }
}

// 6. واجهة اكتمال وحل المشكلة بنجاح مع علامة صح وملاحظات الأدمن وزر إنهاء الجلسة
@Composable
fun CompletedSuccessScreen(
    request: SupportRequest?,
    onEndSession: () -> Unit
) {
    val context = LocalContext.current
    val notes = request?.adminNotes?.takeIf { it.isNotBlank() }
        ?: "تمت معالجة طلبك لرقم الهاتف بنجاح. يمكنك الآن استخدام الخدمة."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.5.dp, OfficialGreenBright), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(OfficialGreenBright.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, OfficialGreenBright, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(OfficialGreenPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success Check",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "تم إتمام الطلب بنجاح!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = OfficialGreenBright,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "اكتملت عملية (${request?.issueType ?: SupportRequest.DEFAULT_ISSUE_TYPE}) لرقم الهاتف (${request?.phoneNumber.orEmpty()}) بنجاح تام.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = OfficialGreenDeep.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp),
                border = OfficialGreenBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ملاحظات وتعليمات الإدارة المرفقة:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OfficialGreenSoft
                        )
                        TextButton(
                            onClick = { copyToClipboard(context, "الملاحظات", notes) }
                        ) {
                            Text("نسخ الملاحظات", fontSize = 12.sp, color = OfficialGreenBright)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = OfficialGreenBright.copy(alpha = 0.2f)
                    )

                    Text(
                        text = notes,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ContactSupportWaitingButton(
                requestId = request?.id.orEmpty(),
                phoneNumber = request?.phoneNumber.orEmpty(),
                statusLabel = "مكتمل"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onEndSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OfficialGreenPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إنهاء الجلسة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// مكون العداد الدائري الاحترافي المزود بحلقة نبض متوهجة ومزامنة وقت السيرفر
@Composable
fun ProfessionalCountdownWidget(
    endTime: Long,
    totalSeconds: Long,
    labelText: String
) {
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

    LaunchedEffect(key1 = endTime, key2 = serverTimeOffset) {
        while (true) {
            val estimatedServerNow = System.currentTimeMillis() + serverTimeOffset
            val diff = endTime - estimatedServerNow
            timeLeft = if (diff > 0) diff / 1000L else 0L
            if (timeLeft <= 0L) break
            delay(1000L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "TimerGlow")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    val safeTotal = totalSeconds.coerceAtLeast(1L).toFloat()
    val progress = if (timeLeft > 0) (timeLeft.toFloat() / safeTotal).coerceIn(0.03f, 1f) else 0f

    Box(
        modifier = Modifier.size(175.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 8.dp.toPx()
            drawCircle(
                color = Color(0xFF1F2C34),
                style = Stroke(width = strokeWidthPx)
            )
            drawArc(
                color = OfficialGreenBright,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
            drawArc(
                color = OfficialGreenBright.copy(alpha = 0.35f),
                startAngle = sweepAngle,
                sweepAngle = 45f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formattedTime,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (timeLeft > 0) labelText else "جاري الإنهاء...",
                fontSize = 11.sp,
                color = OfficialGreenBright,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun isAppPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
        true
    } catch (_: Exception) {
        false
    }
}

data class TargetAppOption(
    val name: String,
    val packageName: String,
    val iconDrawableName: String
)

private fun openInstalledApp(context: Context, packageName: String): Boolean {
    return try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}

@Composable
fun RemoteAppActivationWizardScreen(
    requestId: String,
    request: SupportRequest?,
    viewModel: UserSupportViewModel,
    isSubmittingStep: Boolean
) {
    val context = LocalContext.current

    // قائمة الـ 3 تطبيقات المستهدفة مع أسماء أيقوناتها بصيغة PNG في مجلد drawable
    val targetApps = remember {
        listOf(
            TargetAppOption(name = "Red", packageName = "com.target.app", iconDrawableName = "ic_app_red"),
            TargetAppOption(name = "Blue", packageName = "com.target.blue", iconDrawableName = "ic_app_blue"),
            TargetAppOption(name = "Gold", packageName = "com.target.gold", iconDrawableName = "ic_app_gold")
        )
    }

    val abdullahStorePackage = DatabaseKeys.ABDULLAH_STORE_PACKAGE // com.fix.engine.abdullah
    val storeDirectApkUrl = remember {
        FirebaseRemoteConfig.getInstance()
            .getString(DatabaseKeys.RC_ABDULLAH_STORE_APK_URL)
            .ifBlank { "https://abdullahtamemi.blogspot.com" }
    }

    // المرحلة الداخلية: 1 = سؤال هل الرقم مفعل؟ | 2 = اختيار التطبيق والتحقق من تثبيته عبر متجر Abdullah
    var currentWizardStep by remember { mutableStateOf(1) }

    // متغيرات الخطوة 1 (حالة الرقم)
    var mainActivationChoice by remember { mutableStateOf("") }
    var subAppChoice by remember { mutableStateOf("") }

    // متغيرات الخطوة 2 (اختيار أحد التطبيقات الثلاثة وفحص متجر Abdullah والحزمة المختارة)
    var selectedTargetApp by remember { mutableStateOf<TargetAppOption?>(null) }
    var isStoreInstalled by remember {
        mutableStateOf(isAppPackageInstalled(context, abdullahStorePackage))
    }
    var isTargetInstalled by remember { mutableStateOf(false) }

    LaunchedEffect(currentWizardStep, selectedTargetApp) {
        if (currentWizardStep == 2) {
            while (true) {
                isStoreInstalled = isAppPackageInstalled(context, abdullahStorePackage)
                selectedTargetApp?.let { target ->
                    isTargetInstalled = isAppPackageInstalled(context, target.packageName)
                }
                delay(1500L)
            }
        }
    }

    val isStep1Valid = when (mainActivationChoice) {
        "ACTIVE" -> subAppChoice.isNotBlank()
        "INACTIVE" -> true
        else -> false
    }

    val finalNumberStateText = when (mainActivationChoice) {
        "ACTIVE" -> "مفعل ويعمل حاليا ($subAppChoice)"
        "INACTIVE" -> "غير مفعل"
        else -> ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(OfficialGreenBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
                .animateContentSize()
        ) {
            Surface(
                color = OfficialGreenDeep.copy(alpha = 0.6f),
                shape = RoundedCornerShape(50),
                border = OfficialGreenBorder
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = OfficialGreenBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "تمت موافقة الإدارة • خطوة $currentWizardStep من 2",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OfficialGreenSoft
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentWizardStep == 1) {
                Text(
                    text = "هل الرقم مفعل حاليا و يعمل؟",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "يرجى تحديد الحالة الحالية للرقم (${request?.phoneNumber.orEmpty()}) للمتابعة:",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                AnimatedVisibility(
                    visible = mainActivationChoice.isEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    AnimatedStepGuideTooltip(
                        stepBadge = "الخطوة 1",
                        message = "اضغط على أحد الخيارين أدناه لتوضيح ما إذا كان الرقم يعمل حالياً أم متوقفاً",
                        accentColor = AmberWarning
                    )
                }

                SelectableCheckOptionCard(
                    title = "مفعل ويعمل حاليا",
                    subtitle = "اختر هذا الخيار إذا كان الرقم مفتوحاً لديك الآن ويرسل ويستقبل الرسائل",
                    isSelected = mainActivationChoice == "ACTIVE",
                    onClick = { mainActivationChoice = "ACTIVE" }
                )

                if (mainActivationChoice == "ACTIVE") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, top = 10.dp, bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedVisibility(
                            visible = subAppChoice.isEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            AnimatedStepGuideTooltip(
                                stepBadge = "تحديد النسخة",
                                message = "اختر التطبيق المفعل فيه الرقم حالياً (تطبيق ١ أو تطبيق ٢)",
                                accentColor = OfficialGreenBright
                            )
                        }

                        Text(
                            text = "حدد التطبيق المفعل فيه الرقم حالياً:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OfficialGreenBright
                        )

                        SelectableCheckOptionCard(
                            title = "مفعل في تطبيق ١",
                            iconDrawableName = "ic_source_app_1",
                            isSelected = subAppChoice == "مفعل في تطبيق ١",
                            onClick = { subAppChoice = "مفعل في تطبيق ١" }
                        )

                        SelectableCheckOptionCard(
                            title = "مفعل في تطبيق ٢",
                            iconDrawableName = "ic_source_app_2",
                            isSelected = subAppChoice == "مفعل في تطبيق ٢",
                            onClick = { subAppChoice = "مفعل في تطبيق ٢" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SelectableCheckOptionCard(
                    title = "غير مفعل",
                    subtitle = "اختر هذا الخيار إذا كان الرقم متوقفاً أو يطلب منك إدخال الكود الآن",
                    isSelected = mainActivationChoice == "INACTIVE",
                    onClick = {
                        mainActivationChoice = "INACTIVE"
                        subAppChoice = ""
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedVisibility(
                    visible = isStep1Valid,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    AnimatedStepGuideTooltip(
                        stepBadge = "متابعة",
                        message = "ممتاز! اضغط الآن على زر (استكمال) بالأسفل للانتقال لاختيار التطبيق",
                        accentColor = OfficialGreenBright
                    )
                }

                Button(
                    onClick = { currentWizardStep = 2 },
                    enabled = isStep1Valid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OfficialGreenPrimary,
                        disabledContainerColor = Color(0xFF1F2C34)
                    )
                ) {
                    Text(
                        text = "استكمال",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = "اختيار التطبيق المطلوب تفعيله",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "اختر أحد التطبيقات الثلاثة المعتمدة لتفعيل رقمك عليه:",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = selectedTargetApp == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    AnimatedStepGuideTooltip(
                        stepBadge = "الخطوة 2",
                        message = "اضغط على أحد التطبيقات الثلاثة أدناه لفحص تثبيته واستكمال التفعيل",
                        accentColor = AmberWarning
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    targetApps.forEach { appOption ->
                        val isChosen = selectedTargetApp?.packageName == appOption.packageName
                        SelectableCheckOptionCard(
                            title = "تطبيق ${appOption.name}",
                            subtitle = "اسم الحزمة: ${appOption.packageName}",
                            iconDrawableName = appOption.iconDrawableName,
                            isSelected = isChosen,
                            onClick = {
                                selectedTargetApp = appOption
                                isStoreInstalled = isAppPackageInstalled(context, abdullahStorePackage)
                                isTargetInstalled = isAppPackageInstalled(context, appOption.packageName)
                            }
                        )
                    }
                }

                val chosenApp = selectedTargetApp
                if (chosenApp != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isTargetInstalled) {
                        Surface(
                            color = AmberWarning.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, AmberWarning.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "تنبيه هام لاستكمال التفعيل",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (!isStoreInstalled) {
                                    Text(
                                        text = "لتثبيت تطبيق (${chosenApp.name})، يجب أولاً تثبيت (متجر Abdullah) الرسمي صاحب الحزمة ($abdullahStorePackage)، ثم تحميل التطبيق من داخل المتجر:",
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(storeDirectApkUrl))
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                                    ) {
                                        Text(
                                            text = "1. تحميل وتثبيت متجر Abdullah (إجباري)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "متجر Abdullah مثبت لديك ✓
اضغط على الزر أدناه لفتح (متجر Abdullah) مباشرة وتنزيل وتثبيت تطبيق (${chosenApp.name}) صاحب الحزمة (${chosenApp.packageName}):",
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            val opened = openInstalledApp(context, abdullahStorePackage)
                                            if (!opened) {
                                                Toast.makeText(
                                                    context,
                                                    "تعذر فتح متجر Abdullah، يرجى التأكد من تثبيته",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                                    ) {
                                        Text(
                                            text = "فتح متجر Abdullah لتحميل تطبيق ${chosenApp.name}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = AmberWarning,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "جاري التحقق التلقائي من تثبيت الحزمة (${chosenApp.packageName})... سيظهر زر استكمال التفعيل فور تثبيت التطبيق من المتجر.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = OfficialGreenDeep.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, OfficialGreenBright),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = OfficialGreenBright,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "تم التحقق من تثبيت تطبيق ${chosenApp.name} بنجاح ✓",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "الحزمة (${chosenApp.packageName}) جاهزة على جهازك، يمكنك الآن استكمال التفعيل.",
                                        fontSize = 12.sp,
                                        color = OfficialGreenSoft
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        AnimatedStepGuideTooltip(
                            stepBadge = "الخطوة الأخيرة",
                            message = "اضغط على زر (استكمال التفعيل) أدناه لإرسال البيانات واستلام ملف التفعيل الذكي",
                            accentColor = OfficialGreenBright
                        )

                        Button(
                            onClick = {
                                viewModel.confirmRemoteAppSetup(
                                    requestId = requestId,
                                    numberStateAnswer = finalNumberStateText,
                                    targetAppName = chosenApp.name,
                                    targetAppPackage = chosenApp.packageName
                                ) { success ->
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "تم إرسال بيانات التفعيل للإدارة بنجاح",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "تعذر إرسال البيانات، تحقق من الإنترنت",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            enabled = !isSubmittingStep,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OfficialGreenPrimary)
                        ) {
                            if (isSubmittingStep) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("جاري الإرسال...", fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "استكمال التفعيل",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { currentWizardStep = 1 },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("الرجوع لتعديل حالة الرقم", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ContactSupportWaitingButton(
                requestId = requestId,
                phoneNumber = request?.phoneNumber.orEmpty(),
                statusLabel = "خطوات تفعيل التطبيق عن بُعد"
            )
        }
    }
}

/**
 * شاشة استعادة بيانات التفعيل الذكي (ZIP) بدون روت مع الاحتفاظ الدائم بزر إعادة المحاولة
 */
@Composable
fun RemoteZipRestoreScreen(request: SupportRequest?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val targetAppName = request?.targetAppName?.ifBlank { "Red" } ?: "Red"
    val targetPackage = request?.targetAppPackage?.ifBlank { "com.target.app" } ?: "com.target.app"
    val zipUrl = request?.activationZipUrl.orEmpty()

    var isRestoring by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var restoreSuccessMessage by remember { mutableStateOf<String?>(null) }
    var restoreErrorMessage by remember { mutableStateOf<String?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        isRestoring = false
        if (activityResult.resultCode == android.app.Activity.RESULT_OK) {
            val msg = activityResult.data?.getStringExtra("restore_message")
                ?: "تم استبدال بيانات الحزمة ($targetPackage) وتفعيل التطبيق بنجاح تام!"
            restoreSuccessMessage = msg
            restoreErrorMessage = null

            request?.id?.takeIf { it.isNotBlank() }?.let { reqId ->
                FirebaseDatabase.getInstance()
                    .getReference(DatabaseKeys.NODE_REQUESTS)
                    .child(reqId)
                    .child(DatabaseKeys.FIELD_RESTORE_COMPLETED)
                    .setValue(true)
            }
        } else {
            val err = activityResult.data?.getStringExtra("restore_error")
                ?: "تعذر إتمام استبدال البيانات داخل الحزمة ($targetPackage). يرجى المحاولة مرة أخرى."
            restoreErrorMessage = err
        }
    }

    fun triggerSmartRestoreWithoutRoot() {
        if (isRestoring) return
        scope.launch {
            isRestoring = true
            restoreErrorMessage = null
            val prepResult = SmartDataRestoreManager.downloadAndPrepareRestoreIntent(
                context = context,
                zipUrl = zipUrl,
                targetPackage = targetPackage,
                onProgressStep = { step -> progressText = step }
            )
            when (prepResult) {
                is PrepareZipResult.Ready -> {
                    runCatching {
                        restoreLauncher.launch(prepResult.launchIntent)
                    }.onFailure { e ->
                        isRestoring = false
                        restoreErrorMessage = "تعذر تشغيل أداة الاستعادة في ($targetAppName): ${e.localizedMessage}"
                    }
                }
                is PrepareZipResult.Error -> {
                    isRestoring = false
                    restoreErrorMessage = prepResult.errorMessage
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(OfficialGreenBorder, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = OfficialGreenDeep.copy(alpha = 0.65f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, OfficialGreenBright)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = OfficialGreenBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ملف التفعيل الذكي جاهز من الإدارة ✓",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "تفعيل تطبيق ($targetAppName) واستعادة البيانات",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "اضغط على زر التفعيل أدناه ليقوم التطبيق تلقائياً بتحميل حزمة البيانات وحقنها مباشرة داخل ($targetPackage) بدون الحاجة لروت.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (isRestoring) {
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = OfficialGreenBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = OfficialGreenBright,
                            modifier = Modifier.size(34.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = progressText.ifBlank { "جاري استعادة البيانات وتفعيل التطبيق..." },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OfficialGreenBright,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (restoreSuccessMessage != null) {
                Surface(
                    color = OfficialGreenDeep.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, OfficialGreenBright),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = OfficialGreenBright,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "تمت عملية التفعيل واستعادة البيانات بنجاح! ✓",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = restoreSuccessMessage.orEmpty(),
                            fontSize = 12.sp,
                            color = OfficialGreenSoft,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { openInstalledApp(context, targetPackage) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OfficialGreenBright)
                        ) {
                            Text(
                                text = "فتح تطبيق $targetAppName الآن",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (restoreErrorMessage != null) {
                Surface(
                    color = DangerRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DangerRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ ${restoreErrorMessage.orEmpty()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (restoreSuccessMessage == null && !isRestoring) {
                AnimatedStepGuideTooltip(
                    stepBadge = "تفعيل الآن",
                    message = "اضغط على الزر الأخضر أدناه لبدء استعادة بيانات التفعيل تلقائياً",
                    accentColor = OfficialGreenBright
                )
            }

            Button(
                onClick = { triggerSmartRestoreWithoutRoot() },
                enabled = !isRestoring,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (restoreSuccessMessage == null) OfficialGreenPrimary else DarkSurfaceElevated
                ),
                border = if (restoreSuccessMessage != null) BorderStroke(1.2.dp, OfficialGreenBright) else null
            ) {
                Text(
                    text = if (restoreSuccessMessage == null) {
                        "بدء تفعيل التطبيق واستعادة البيانات (ZIP)"
                    } else {
                        "🔄 إعادة المحاولة / تكرار استعادة البيانات"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            ContactSupportWaitingButton(
                requestId = request?.id.orEmpty(),
                phoneNumber = request?.phoneNumber.orEmpty(),
                statusLabel = "استعادة ملف التفعيل الذكي"
            )
        }
    }
}

/**
 * سهم توضيحي متحرك مع رسالة منبثقة لتوجيه المستخدم العادي خطوة بخطوة
 */
@Composable
fun AnimatedStepGuideTooltip(
    stepBadge: String,
    message: String,
    accentColor: Color = AmberWarning,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArrowBounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BounceOffset"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // الفقاعة النصية التوضيحية المنبثقة
        Surface(
            color = accentColor.copy(alpha = 0.16f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, accentColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = accentColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stepBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = message,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // السهم المتحرك الذي يشير إلى العنصر المطلوب الضغط عليه
        Box(
            modifier = Modifier
                .offset { IntOffset(0, bounceOffset.roundToInt()) }
                .padding(top = 2.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = accentColor,
                shape = CircleShape,
                shadowElevation = 6.dp,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "السهم التوضيحي",
                    tint = Color.Black,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )
            }
        }
    }
}

@Composable
fun SelectableCheckOptionCard(
    title: String,
    subtitle: String? = null,
    iconDrawableName: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val iconResId = remember(iconDrawableName) {
        if (iconDrawableName.isNullOrBlank()) 0
        else context.resources.getIdentifier(iconDrawableName, "drawable", context.packageName)
    }

    Surface(
        color = if (isSelected) OfficialGreenDeep.copy(alpha = 0.5f) else DarkSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) OfficialGreenBright else Color.White.copy(alpha = 0.12f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (!iconDrawableName.isNullOrBlank()) {
                    if (iconResId != 0) {
                        Image(
                            painter = painterResource(id = iconResId),
                            contentDescription = title,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, OfficialGreenBright.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(OfficialGreenBright.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, OfficialGreenBright.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = title,
                                tint = OfficialGreenBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else TextPrimary
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = if (isSelected) OfficialGreenSoft else TextSecondary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (isSelected) OfficialGreenBright else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        1.5.dp,
                        if (isSelected) OfficialGreenBright else TextSecondary,
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
