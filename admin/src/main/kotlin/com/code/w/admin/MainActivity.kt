package com.code.w.admin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.code.w.admin.repository.AdminSupportRequest
import com.code.w.admin.viewmodel.AdminSupportViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AdminDashboardScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen() {
    val viewModel: AdminSupportViewModel = viewModel()
    val requests by viewModel.requests.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("لوحة تحكم الإدارة - طلبات الدعم") })
        }
    ) { paddingValues ->
        if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("لا توجد طلبات دعم حالياً", fontSize = 16.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(requests, key = { it.id }) { request ->
                    AdminRequestItem(request = request, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AdminRequestItem(request: AdminSupportRequest, viewModel: AdminSupportViewModel) {
    val context = LocalContext.current
    var bankDetails by remember { mutableStateOf("") }
    var adminNotes by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "رقم المستخدم: ${request.phoneNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "نوع المشكلة: ${request.issueType}", modifier = Modifier.padding(top = 4.dp))
            Text(
                text = "الحالة: ${request.status}",
                color = when(request.status) {
                    "SUBMITTED" -> Color(0xFFE65100)
                    "APPROVED" -> Color(0xFF0288D1)
                    "RECEIPT_SUBMITTED" -> Color(0xFFF57C00)
                    else -> Color(0xFF388E3C)
                },
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            when (request.status) {
                "SUBMITTED" -> {
                    OutlinedTextField(
                        value = bankDetails,
                        onValueChange = { bankDetails = it },
                        label = { Text("أدخل بيانات الحساب البنكي للأدمن") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            if (bankDetails.isNotBlank()) {
                                viewModel.approveRequest(request.id, bankDetails) { success ->
                                    if (success) Toast.makeText(context, "تمت الموافقة وطلب الإيداع", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "يرجى كتابة البيانات البنكية أولاً", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("الموافقة وطلب إيداع")
                    }
                }

                "APPROVED" -> {
                    Text("في انتظار قيام المستخدم برفع صورة الإيداع البنكي...", color = Color.Gray, fontSize = 14.sp)
                }

                "RECEIPT_SUBMITTED" -> {
                    Text("قام المستخدم برفع الإيصال الحسابي:", fontWeight = FontWeight.Bold)
                    Text(text = "رابط الصورة: ${request.receiptImageUrl}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = adminNotes,
                        onValueChange = { adminNotes = it },
                        label = { Text("أدخل الملاحظات والتعليمات النهائية للعملية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            if (adminNotes.isNotBlank()) {
                                viewModel.completeRequest(request.id, adminNotes) { success ->
                                    if (success) Toast.makeText(context, "تم تأكيد العملية وإغلاق الطلب", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "يرجى كتابة التعليمات للمستخدم أولاً", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                    ) {
                        Text("تأكيد نجاح العملية وإرسال الإشعار")
                    }
                }

                "COMPLETED" -> {
                    Text("تم إغلاق مذكرة الطلب بنجاح مسبقاً.", color = Color(0xFF388E3C), fontWeight = FontWeight.Medium)
                    Text("الملاحظات المرسلة: ${request.adminNotes}", fontSize = 14.sp)
                }
            }
        }
    }
}
