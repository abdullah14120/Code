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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.code.w.model.SupportRequest
import com.code.w.viewmodel.UserSupportViewModel
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPreferences = getSharedPreferences("support_prefs", Context.MODE_PRIVATE)
        val savedRequestId = sharedPreferences.getString("last_request_id", null)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigationRouter(initialRequestId = savedRequestId)
                }
            }
        }
    }
}

@Composable
fun AppNavigationRouter(initialRequestId: String?) {
    val context = LocalContext.current
    val viewModel: UserSupportViewModel = viewModel()
    var currentRequestId by remember { mutableStateOf(initialRequestId) }

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
                onClearSession = {
                    val sharedPreferences = context.getSharedPreferences("support_prefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().remove("last_request_id").apply()
                    currentRequestId = null
                }
            )
        }
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "مركز المساعدة والدعم الفني", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }
                if (digitsOnly.length <= 9) {
                    if (digitsOnly.isEmpty()) {
                        phone = ""
                    } else if (digitsOnly.startsWith("7")) {
                        phone = digitsOnly
                    }
                }
            },
            label = { Text("رقم الهاتف (9 أرقام يبدأ بـ 7)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "نوع المشكلة: $selectedIssue")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                issues.forEach { issue ->
                    DropdownMenuItem(
                        text = { Text(issue) },
                        onClick = { selectedIssue = issue; expanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (phone.length == 9 && phone.startsWith("7")) {
                    viewModel.sendSupportRequest(phone, selectedIssue, onSuccess, {
                        Toast.makeText(context, "فشل إرسال الطلب", Toast.LENGTH_SHORT).show()
                    })
                } else {
                    Toast.makeText(context, "يجب أن يتكون الرقم من 9 أرقام ويبدأ بـ 7", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp).animateContentSize(),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSubmitting && phone.length == 9
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
            } else {
                Text("إرسال طلب", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun TrackingScreen(requestId: String, viewModel: UserSupportViewModel, onClearSession: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(requestId) { viewModel.startObservingRequest(requestId) }
    val requestState by viewModel.currentRequest.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    Crossfade(targetState = requestState?.status, label = "TrackingNav") { status ->
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            when (status) {
                SupportRequest.Status.SUBMITTED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("جاري مراجعة الطلب...", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                // الواجهة الجديدة: شاشة العداد التنازلي الذكي
                SupportRequest.Status.PRE_APPROVED -> {
                    val endTime = requestState?.timerEndTime ?: 0L
                    CountdownTimerScreen(endTime = endTime)
                }

                SupportRequest.Status.APPROVED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("تمت الموافقة على طلبك", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("الحساب البنكي للأدمن:", fontWeight = FontWeight.Bold)
                                Text(requestState?.bankDetails ?: "")
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Text(if (selectedImageUri == null) "إرفاق صورة الإيداع" else "تم اختيار صورة الإيداع")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                selectedImageUri?.let { uri ->
                                    viewModel.uploadReceipt(context, requestId, uri) { success ->
                                        if (success) {
                                            Toast.makeText(context, "تم إرسال الإيصال بنجاح", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "فشل معالجة وإرسال الصورة", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = selectedImageUri != null && !isUploading,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            if (isUploading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text("تأكيد وإرسال")
                        }
                    }
                }
                SupportRequest.Status.RECEIPT_SUBMITTED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("جاري التحقق من إيصال التحويل...", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
                SupportRequest.Status.COMPLETED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("تمت العملية بنجاح", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("ملاحظات وتعليمات الأدمن:", fontWeight = FontWeight.Bold)
                                Text(requestState?.adminNotes ?: "")
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        TextButton(onClick = onClearSession) {
                            Text("العودة لإنشاء طلب جديد")
                        }
                    }
                }
                null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("جاري جلب بيانات الحالة...")
                    }
                }
            }
        }
    }
}

// الكومبوننت المخصص لحساب وعرض العداد التنازلي بشكل حي ومحمي ضد الإغلاق
@Composable
fun CountdownTimerScreen(endTime: Long) {
    var timeLeft by remember { mutableStateOf(0L) }

    // حلقة تحديث برمجية تتأكد من حساب الفارق الزمني الحقيقي كل ثانية واحدة
    LaunchedEffect(key1 = endTime) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            val difference = endTime - currentTime
            timeLeft = if (difference > 0) difference / 1000 else 0L
            if (timeLeft <= 0L) break
            delay(1000)
        }
    }

    // تحويل الثواني المتبقية إلى صيغة MM:SS القياسية
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            progress = { if (timeLeft > 0) (timeLeft.toFloat() / 1800f) else 0f }, // 1800 ثانية هي 30 دقيقة
            modifier = Modifier.size(120.dp),
            strokeWidth = 6.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = formattedTime,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "تمت الموافقة المبدئية، جاري تجهيز المعاملة...",
            fontSize = 15.sp,
            color = Color.Gray
        )
    }
}
