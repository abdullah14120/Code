package com.code.w

import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewmodel.compose.viewModel
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.code.w.model.SupportRequest
import com.code.w.viewmodel.UserSupportViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigationRouter()
                }
            }
        }
    }
}

@Composable
fun AppNavigationRouter() {
    val viewModel: UserSupportViewModel = viewModel()
    var currentRequestId by remember { mutableStateOf<String?>(null) }

    Crossfade(targetState = currentRequestId) { id ->
        if (id == null) {
            SubmissionScreen(
                viewModel = viewModel,
                onSuccess = { createdId -> currentRequestId = createdId }
            )
        } else {
            TrackingScreen(requestId = id, viewModel = viewModel)
        }
    }
}

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
            onValueChange = { phone = it },
            label = { Text("رقم الهاتف") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
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
                if (phone.isNotBlank()) {
                    viewModel.sendSupportRequest(phone, selectedIssue, onSuccess, {
                        Toast.makeText(context, "فشل إرسال الطلب", Toast.LENGTH_SHORT).show()
                    })
                } else {
                    Toast.makeText(context, "يرجى إدخال رقم الهاتف", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp).animateContentSize(),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSubmitting
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
fun TrackingScreen(requestId: String, viewModel: UserSupportViewModel) {
    val context = LocalContext.current
    LaunchedEffect(requestId) { viewModel.startObservingRequest(requestId) }
    val requestState by viewModel.currentRequest.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    Crossfade(targetState = requestState?.status) { status ->
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            when (status) {
                SupportRequest.Status.SUBMITTED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("جاري مراجعة الطلب...", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
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
                                    viewModel.uploadReceipt(requestId, uri) { success ->
                                        if (!success) Toast.makeText(context, "فشل رفع الصورة", Toast.LENGTH_SHORT).show()
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
                    }
                }
            }
        }
    }
}
