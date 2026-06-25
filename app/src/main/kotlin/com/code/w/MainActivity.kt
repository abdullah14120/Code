package com.code.w

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke // تصحيح استدعاء الـ Stroke بحجم كبير
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.code.w.model.SupportRequest
import com.code.w.viewmodel.UserSupportViewModel
import kotlinx.coroutines.delay
import java.util.Locale

private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1E1E1E)
private val AccentPrimary = Color(0xFF0D9488) 
private val TextPrimary = Color(0xFFF3F4F6)
private val TextSecondary = Color(0xFF9CA3AF)
private val NeumorphicBorder = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPreferences = getSharedPreferences("support_prefs", Context.MODE_PRIVATE)
        val savedRequestId = sharedPreferences.getString("last_request_id", null)

        setContent {
            CustomDarkTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigationRouter(initialRequestId = savedRequestId)
                }
            }
        }
    }
}

@Composable
fun CustomDarkTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        background = DarkBackground,
        surface = DarkSurface,
        primary = AccentPrimary,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        surfaceVariant = DarkSurface
    )
    MaterialTheme(colorScheme = darkColorScheme, content = content)
}

@Composable
fun AppNavigationRouter(initialRequestId: String?) {
    val context = LocalContext.current
    val viewModel: UserSupportViewModel = viewModel()
    var currentRequestId by remember { mutableStateOf(initialRequestId) }
    val requestState by viewModel.currentRequest.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopBannerComponent()
                requestState?.let { request ->
                    StepperComponent(currentStatus = request.status)
                }
            }
        },
        bottomBar = { BottomFooterComponent() },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Crossfade(targetState = currentRequestId, label = "AppNav") { id ->
                if (id == null) {
                    SubmissionScreen(
                        viewModel = viewModel,
                        onSuccess = { createdId -> 
                            val sharedPreferences = context.getSharedPreferences("support_prefs", Context.MODE_PRIVATE)
                            sharedPreferences.edit().putString("last_request_id", createdId).apply()
                            currentRequestId = createdId 
                        }
                    )
                } else {
                    TrackingScreen(
                        requestId = id, 
                        viewModel = viewModel,
                        requestState = requestState,
                        isUploading = viewModel.isUploading.collectAsState().value,
                        onClearSession = {
                            val sharedPreferences = context.getSharedPreferences("support_prefs", Context.MODE_PRIVATE)
                            sharedPreferences.edit().remove("last_request_id").apply()
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).border(NeumorphicBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Banner Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(text = "نظام الدّم الفني الذكي", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "معالجة فورية ومتابعة لحظية لطلباتكم", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun StepperComponent(currentStatus: SupportRequest.Status) {
    val steps = listOf("مراجعة", "موافقة", "إيداع", "اكتمال")
    val activeIndex = when (currentStatus) {
        SupportRequest.Status.SUBMITTED -> 0
        SupportRequest.Status.PRE_APPROVED -> 1
        SupportRequest.Status.APPROVED -> 2
        SupportRequest.Status.RECEIPT_SUBMITTED -> 2
        SupportRequest.Status.COMPLETED -> 3
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = index <= activeIndex
            val color = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF374151)
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = index < steps.lastIndex)) {
                Box(
                    modifier = Modifier.size(24.dp).background(if (isActive) color.copy(alpha = 0.2f) else Color.Transparent, CircleShape).border(1.5.dp, color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = (index + 1).toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = step, fontSize = 11.sp, fontWeight = if (index == activeIndex) FontWeight.Bold else FontWeight.Normal, color = if (index == activeIndex) TextPrimary else TextSecondary)
                
                if (index < steps.lastIndex) {
                    HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), thickness = 1.dp, color = if (index < activeIndex) MaterialTheme.colorScheme.primary else Color(0xFF374151))
                }
            }
        }
    }
}

@Composable
fun PulseIndicator(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        // تم تصحيح الـ tween بدلاً من الـ twin الخاطئة برمجياً
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "PulseProgress"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "PulseAlpha"
    )

    Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // تم تصحيح استدعاء الـ Stroke بحرف S كبير
            drawCircle(color = color, radius = size.minDimension / 2 * progress, alpha = alpha, style = Stroke(2.dp.toPx()))
            drawCircle(color = color, radius = 4.dp.toPx())
        }
    }
}

@Composable
fun BottomFooterComponent() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = Icons.Default.Call, contentDescription = "Support", tint = TextSecondary, modifier = Modifier.size(12.dp))
            Text(text = "الدعم المباشر: +966500000000", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        }
        Text(text = "جميع الحقوق محفوظة © 2026", fontSize = 11.sp, color = Color(0xFF4B5563), textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionScreen(viewModel: UserSupportViewModel, onSuccess: (String) -> Unit) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val issues = listOf("المشكلة الاولى")
    var selectedIssue by remember { mutableStateOf(issues[0]) }
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "إنشاء طلب دعم جديد", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }
                if (digitsOnly.length <= 9 && (digitsOnly.isEmpty() || digitsOnly.startsWith("7"))) {
                    phone = digitsOnly
                }
            },
            label = { Text("رقم الهاتف") },
            placeholder = { Text("7xxxxxxxx") },
            modifier = Modifier.fillMaxWidth().border(NeumorphicBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            // تم تصحيح الـ colors وتمريرها للهيكل المعتمد داخل الـ OutlinedTextFieldDefaults
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().border(NeumorphicBorder, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary, containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.dp, Color.Transparent)
            ) {
                Text(text = "نوع المشكلة: $selectedIssue")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                issues.forEach { issue ->
                    DropdownMenuItem(text = { Text(issue, color = TextPrimary) }, onClick = { selectedIssue = issue; expanded = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                if (phone.length == 9 && phone.startsWith("7")) {
                    viewModel.sendSupportRequest(phone, selectedIssue, onSuccess, {
                        Toast.makeText(context, "فشل إرسال الطلب", Toast.LENGTH_SHORT).show()
                    })
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp).animateContentSize().border(NeumorphicBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isSubmitting && phone.length == 9
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
            } else {
                Text("تأكيد وإرسال", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TrackingScreen(requestId: String, viewModel: UserSupportViewModel, requestState: SupportRequest?, isUploading: Boolean, onClearSession: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(requestId) { viewModel.startObservingRequest(requestId) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? -> selectedImageUri = uri }

    Crossfade(targetState = requestState?.status, label = "TrackingNav") { status ->
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            when (status) {
                SupportRequest.Status.SUBMITTED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PulseIndicator(color = Color(0xFFE65100)) 
                            Text("جاري مراجعة الطلب...", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        }
                        CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                    }
                }
                
                SupportRequest.Status.PRE_APPROVED -> {
                    val endTime = requestState?.timerEndTime ?: 0L
                    CountdownTimerScreen(endTime = endTime)
                }

                SupportRequest.Status.APPROVED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PulseIndicator(color = Color(0xFF0D9488)) 
                            Text("تمت الموافقة على طلبك", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(modifier = Modifier.fillMaxWidth().border(NeumorphicBorder, RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("الحساب البنكي للأدمن:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(requestState?.bankDetails ?: "", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth().border(NeumorphicBorder, RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = TextPrimary), border = BorderStroke(0.dp, Color.Transparent)) {
                            Text(if (selectedImageUri == null) "إرفاق صورة الإيداع" else "تم اختيار صورة الإيداع")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                selectedImageUri?.let { uri ->
                                    viewModel.uploadReceipt(context, requestId, uri) { success ->
                                        if (!success) Toast.makeText(context, "فشل معالجة وإرسال الصورة", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = selectedImageUri != null && !isUploading,
                            modifier = Modifier.fillMaxWidth().height(50.dp).border(NeumorphicBorder, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isUploading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text("تأكيد وإرسال الإيصال", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                SupportRequest.Status.RECEIPT_SUBMITTED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PulseIndicator(color = Color(0xFFF57C00))
                            Text("جاري التحقق من إيصال التحويل...", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        }
                        CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                    }
                }
                
                SupportRequest.Status.COMPLETED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("تمت العملية بنجاح", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(modifier = Modifier.fillMaxWidth().border(NeumorphicBorder, RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("ملاحظات وتعليمات الأدمن النهائي:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(requestState?.adminNotes ?: "", fontSize = 16.sp, color = TextPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        TextButton(onClick = onClearSession) {
                            Text("العودة لإنشاء طلب جديد", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("جاري قراءة وتحديث البيانات السحابية...", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun CountdownTimerScreen(endTime: Long) {
    var timeLeft by remember { mutableStateOf(0L) }

    LaunchedEffect(key1 = endTime) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            val difference = endTime - currentTime
            timeLeft = if (difference > 0) difference / 1000 else 0L
            if (timeLeft <= 0L) break
            delay(1000)
        }
    }

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { if (timeLeft > 0) (timeLeft.toFloat() / 1800f) else 0f },
                modifier = Modifier.size(130.dp),
                strokeWidth = 5.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFF1E1E1E)
            )
            Text(text = formattedTime, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // تم تصحيح الـ tween هنا أيضاً
            PulseIndicator(color = Color(0xFF7B1FA2)) 
            Text(text = "تمت الموافقة المبدئية، جاري موازنة الطلب...", fontSize = 14.sp, color = TextSecondary)
        }
    }
}
