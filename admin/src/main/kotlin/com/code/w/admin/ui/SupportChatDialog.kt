package com.code.w.admin.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.code.w.common.model.ChatMessage
import com.code.w.common.model.DatabaseKeys
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

private val ChatDarkBg = Color(0xFF0B141A)
private val ChatSurface = Color(0xFF111B21)
private val ChatSurfaceElevated = Color(0xFF1F2C34)
private val ChatGreenPrimary = Color(0xFF00A884)
private val ChatGreenBright = Color(0xFF10B981)
private val ChatGreenBubble = Color(0xFF005C4B)
private val ChatAmber = Color(0xFFF59E0B)
private val ChatRed = Color(0xFFEF4444)
private val ChatTextPrimary = Color(0xFFF9FAFB)
private val ChatTextSecondary = Color(0xFF9CA3AF)

/**
 * زر التواصل المباشر مع الدعم الفني (يستبدل زر الواتساب ويفتح نافذة المحادثة مع عداد رسائل الأدمن الجديدة)
 */
@Composable
fun LiveChatSupportButton(
    requestId: String,
    phoneNumber: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showChatDialog by remember { mutableStateOf(false) }
    var unreadAdminCount by remember { mutableIntStateOf(0) }
    var lastAdminMessage by remember { mutableStateOf<String?>(null) }
    var previousUnreadCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(requestId) {
        if (requestId.isBlank()) return@LaunchedEffect
        val chatRef = FirebaseDatabase.getInstance()
            .getReference(DatabaseKeys.NODE_REQUESTS)
            .child(requestId)
            .child(DatabaseKeys.FIELD_CHAT_MESSAGES)

        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var unread = 0
                var latestMsgText: String? = null
                snapshot.children.forEach { child ->
                    val sender = child.child("sender").getValue(String::class.java) ?: ""
                    val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                    val text = child.child("text").getValue(String::class.java).orEmpty()
                    val hasImage = !child.child("imageData").getValue(String::class.java).isNullOrBlank()
                    if (sender == ChatMessage.SENDER_ADMIN) {
                        latestMsgText = if (text.isNotBlank()) text else if (hasImage) "📷 أرسل الأدمن صورة مرفقة" else null
                        if (!isRead) unread++
                    }
                }
                if (unread > previousUnreadCount && !showChatDialog) {
                    runCatching {
                        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        RingtoneManager.getRingtone(context, uri)?.play()
                    }
                }
                previousUnreadCount = unread
                unreadAdminCount = unread
                lastAdminMessage = latestMsgText
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // شريط تنبيه بارز إذا أرسل الأدمن ملاحظة أو طلب شيئاً من المستخدم
        if (unreadAdminCount > 0 && !lastAdminMessage.isNullOrBlank()) {
            Surface(
                color = ChatGreenPrimary.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, ChatGreenBright),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showChatDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "💬 رسالة جديدة من الدعم الفني:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChatGreenBright
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = lastAdminMessage.orEmpty(),
                            fontSize = 13.sp,
                            color = ChatTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = ChatRed,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "$unreadAdminCount",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { showChatDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = if (unreadAdminCount > 0) 1.5.dp else 1.dp,
                color = if (unreadAdminCount > 0) ChatAmber else ChatGreenBright.copy(alpha = 0.6f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = ChatSurfaceElevated
            )
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = ChatGreenBright,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "تواصل مع الدعم الفني (محادثة مباشرة)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ChatTextPrimary
            )
            if (unreadAdminCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = ChatRed,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "$unreadAdminCount جديد",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    if (showChatDialog) {
        SupportChatDialog(
            requestId = requestId,
            phoneNumber = phoneNumber,
            currentRole = ChatMessage.SENDER_USER,
            onDismiss = { showChatDialog = false }
        )
    }
}

/**
 * نافذة المحادثة المنبثقة (تعمل للمستخدم وللأدمن حسب المعامل currentRole)
 */
@Composable
fun SupportChatDialog(
    requestId: String,
    phoneNumber: String,
    currentRole: String, // ChatMessage.SENDER_USER أو ChatMessage.SENDER_ADMIN
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageBase64 by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var zoomBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val chatRef = remember(requestId) {
        FirebaseDatabase.getInstance()
            .getReference(DatabaseKeys.NODE_REQUESTS)
            .child(requestId)
            .child(DatabaseKeys.FIELD_CHAT_MESSAGES)
    }

    // منتقي الصور من الاستوديو
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val pair = withContext(Dispatchers.IO) {
                    compressUriToBitmapAndBase64(context, it)
                }
                if (pair != null) {
                    selectedImageBitmap = pair.first
                    selectedImageBase64 = pair.second
                } else {
                    Toast.makeText(context, "تعذر قراءة الصورة المختارة", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // الاستماع الفوري للرسائل وتعليم رسائل الطرف الآخر كمقروءة تلقائياً
    LaunchedEffect(requestId) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loaded = mutableListOf<ChatMessage>()
                snapshot.children.forEach { child ->
                    val id = child.child("id").getValue(String::class.java) ?: child.key.orEmpty()
                    val sender = child.child("sender").getValue(String::class.java) ?: ChatMessage.SENDER_USER
                    val text = child.child("text").getValue(String::class.java).orEmpty()
                    val imageData = child.child("imageData").getValue(String::class.java).orEmpty()
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false

                    loaded.add(ChatMessage(id, sender, text, imageData, timestamp, isRead))

                    // تعليم رسائل الطرف الآخر كمقروءة عند فتح النافذة
                    if (sender != currentRole && !isRead && child.key != null) {
                        chatRef.child(child.key!!).child("isRead").setValue(true)
                    }
                }
                messages = loaded.sortedBy { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        chatRef.addValueEventListener(listener)
    }

    // التمرير التلقائي لآخر رسالة عند وصول رسالة جديدة
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun sendMessage(customText: String? = null) {
        val textToSend = (customText ?: inputText).trim()
        val imageToSend = selectedImageBase64
        if (textToSend.isEmpty() && imageToSend.isEmpty()) return

        isSending = true
        val msgId = chatRef.push().key ?: "msg_${System.currentTimeMillis()}"
        val payload = mapOf(
            "id" to msgId,
            "sender" to currentRole,
            "text" to textToSend,
            "imageData" to imageToSend,
            "timestamp" to ServerValue.TIMESTAMP,
            "isRead" to false
        )

        chatRef.child(msgId).setValue(payload).addOnCompleteListener { task ->
            isSending = false
            if (task.isSuccessful) {
                if (customText == null) inputText = ""
                selectedImageBitmap = null
                selectedImageBase64 = ""
            } else {
                Toast.makeText(context, "فشل إرسال الرسالة، تحقق من الاتصال", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(22.dp),
            color = ChatDarkBg,
            border = BorderStroke(1.2.dp, ChatGreenBright.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // =========================================================
                // 1. الشريط العلوي لنافذة المحادثة
                // =========================================================
                Surface(
                    color = ChatSurface,
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                    .background(ChatGreenBright.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, ChatGreenBright, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = ChatGreenBright,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (currentRole == ChatMessage.SENDER_ADMIN) {
                                        "محادثة المستخدم ($phoneNumber)"
                                    } else {
                                        "الدعم الفني المباشر"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChatTextPrimary
                                )
                                Text(
                                    text = "متصل الآن • يمكنك إرسال نص أو إرفاق صورة",
                                    fontSize = 11.sp,
                                    color = ChatGreenBright
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = ChatTextSecondary
                            )
                        }
                    }
                }

                // =========================================================
                // 2. شريط الرسائل السريعة للأدمن فقط
                // =========================================================
                if (currentRole == ChatMessage.SENDER_ADMIN) {
                    Surface(color = ChatSurfaceElevated) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val quickReplies = listOf(
                                "أرسل لقطة شاشة للمشكلة",
                                "افتح التطبيق الآن ولا تغلقه",
                                "أرفق صورة السند هنا"
                            )
                            quickReplies.forEach { preset ->
                                OutlinedButton(
                                    onClick = { sendMessage(customText = preset) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, ChatGreenBright.copy(alpha = 0.4f))
                                ) {
                                    Text(preset, fontSize = 10.sp, color = ChatGreenBright)
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // 3. قائمة الرسائل
                // =========================================================
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "لا توجد رسائل بعد في هذه المحادثة",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChatTextPrimary
                            )
                            Text(
                                text = if (currentRole == ChatMessage.SENDER_ADMIN) {
                                    "يمكنك إرسال ملاحظة فورية أو صورة للمستخدم وسيتم تنبيهه فوراً."
                                } else {
                                    "اكتب استفسارك أو أرفق صورة للمشكلة وسيقوم المختص بالرد عليك مباشرة هنا."
                                },
                                fontSize = 12.sp,
                                color = ChatTextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatBubbleItem(
                                message = msg,
                                isMine = msg.sender == currentRole,
                                onImageClick = { bmp -> zoomBitmap = bmp }
                            )
                        }
                    }
                }

                // =========================================================
                // 4. معاينة الصورة المختارة قبل الإرسال (إن وجدت)
                // =========================================================
                if (selectedImageBitmap != null) {
                    Surface(
                        color = ChatSurfaceElevated,
                        border = BorderStroke(1.dp, ChatGreenBright.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    bitmap = selectedImageBitmap!!.asImageBitmap(),
                                    contentDescription = "Preview",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = "تم تجهيز الصورة للإرسال",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChatGreenBright
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedImageBitmap = null
                                    selectedImageBase64 = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إزالة الصورة",
                                    tint = ChatRed
                                )
                            }
                        }
                    }
                }

                // =========================================================
                // 5. شريط الكتابة وزر إرفاق الصورة وزر الإرسال
                // =========================================================
                Surface(
                    color = ChatSurface,
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // زر إرفاق صورة من الجهاز
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(44.dp)
                                .background(ChatSurfaceElevated, CircleShape)
                                .border(1.dp, ChatGreenBright.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "إرفاق صورة",
                                tint = ChatGreenBright
                            )
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "اكتب رسالتك هنا...",
                                    fontSize = 13.sp,
                                    color = ChatTextSecondary
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ChatGreenBright,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedContainerColor = ChatSurfaceElevated,
                                unfocusedContainerColor = ChatSurfaceElevated
                            )
                        )

                        // زر الإرسال
                        IconButton(
                            onClick = { sendMessage() },
                            enabled = !isSending && (inputText.isNotBlank() || selectedImageBase64.isNotBlank()),
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    if (inputText.isNotBlank() || selectedImageBase64.isNotBlank()) ChatGreenPrimary else ChatSurfaceElevated,
                                    CircleShape
                                )
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "إرسال",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة تكبير الصورة باللمس عند الضغط عليها داخل المحادثة
    if (zoomBitmap != null) {
        ZoomableChatImageDialog(
            bitmap = zoomBitmap!!,
            onDismiss = { zoomBitmap = null }
        )
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    isMine: Boolean,
    onImageClick: (Bitmap) -> Unit
) {
    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.US) }
    val decodedImg = remember(message.imageData) {
        decodeBase64Image(message.imageData)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            color = if (isMine) ChatGreenBubble else ChatSurfaceElevated,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 4.dp else 16.dp,
                bottomEnd = if (isMine) 16.dp else 4.dp
            ),
            border = BorderStroke(
                0.8.dp,
                if (isMine) ChatGreenBright.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (message.sender == ChatMessage.SENDER_ADMIN) "الإدارة / الدعم الفني" else "المستخدم",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (message.sender == ChatMessage.SENDER_ADMIN) ChatGreenBright else ChatAmber
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (decodedImg != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 210.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onImageClick(decodedImg) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Image(
                            bitmap = decodedImg.asImageBitmap(),
                            contentDescription = "صورة مرفقة",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        color = ChatTextPrimary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.timestamp > 0L) {
                        Text(
                            text = sdf.format(Date(message.timestamp)),
                            fontSize = 10.sp,
                            color = ChatTextSecondary
                        )
                    }
                    if (isMine) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (message.isRead) ChatGreenBright else ChatTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableChatImageDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Zoomable Image",
                    modifier = Modifier
                        .fillMaxSize()
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
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun compressUriToBitmapAndBase64(context: Context, uri: Uri): Pair<Bitmap, String>? {
    return try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val maxDim = 1024
        val scale = if (original.width > maxDim || original.height > maxDim) {
            maxDim.toFloat() / maxOf(original.width, original.height).toFloat()
        } else 1f

        val targetW = (original.width * scale).toInt().coerceAtLeast(1)
        val targetH = (original.height * scale).toInt().coerceAtLeast(1)
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(original, targetW, targetH, true) else original

        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 75, out)
        val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        resized to base64
    } catch (_: Exception) {
        null
    }
}

private fun decodeBase64Image(base64Str: String): Bitmap? {
    if (base64Str.isBlank()) return null
    return try {
        val clean = base64Str.substringAfter(",")
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}
