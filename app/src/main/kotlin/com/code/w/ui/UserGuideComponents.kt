package com.code.w.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale

private val GuideSurface = Color(0xFF111B21)
private val GuideSurfaceElevated = Color(0xFF1F2C34)
private val GuideGreenPrimary = Color(0xFF00A884)
private val GuideGreenBright = Color(0xFF10B981)
private val GuideGreenDeep = Color(0xFF064E3B)
private val GuideAmber = Color(0xFFF59E0B)
private val GuideTextPrimary = Color(0xFFF9FAFB)
private val GuideTextSecondary = Color(0xFF9CA3AF)

/**
 * 1. زر الاستماع للشرح الصوتي للمستخدم العادي (يعمل بدون إنترنت عبر محرك نطق أندرويد)
 */
@Composable
fun VoiceInstructionBanner(
    instructionTitle: String,
    spokenTextAr: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale("ar")
                engine?.setSpeechRate(0.92f)
            }
        }
        tts = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    Surface(
        color = GuideGreenDeep.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GuideGreenBright.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val engine = tts
                if (engine != null) {
                    if (isSpeaking) {
                        engine.stop()
                        isSpeaking = false
                    } else {
                        val res = engine.speak(spokenTextAr, TextToSpeech.QUEUE_FLUSH, null, "guide_tts")
                        if (res == TextToSpeech.ERROR) {
                            Toast.makeText(context, spokenTextAr, Toast.LENGTH_LONG).show()
                        } else {
                            isSpeaking = true
                        }
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(GuideGreenBright.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, GuideGreenBright, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = GuideGreenBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "🔊 $instructionTitle",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isSpeaking) "جاري قراءة الشرح... اضغط للإيقاف" else "اضغط هنا لسماع شرح مبسط لما يجب عليك فعله الآن",
                        fontSize = 11.sp,
                        color = GuideGreenBright
                    )
                }
            }
        }
    }
}

/**
 * 2. شريط الخطوات التفاعلي لشاشة الإيداع (يضع علامة صح تلقائياً مع كل خطوة ينجزها المستخدم)
 */
@Composable
fun DepositInteractiveChecklist(
    hasCopiedAccount: Boolean,
    hasAttachedReceipt: Boolean,
    onShowVisualHelp: () -> Unit
) {
    Surface(
        color = GuideSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GuideGreenBright.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 خطواتك المطلوبة الآن بالترتيب:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GuideGreenBright
                )
                TextButton(
                    onClick = onShowVisualHelp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = GuideAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("شرح بالصور", fontSize = 11.sp, color = GuideAmber, fontWeight = FontWeight.Bold)
                }
            }

            ChecklistStepItem(
                stepNumber = 1,
                title = "انسخ رقم حساب العمقي وحوّل المبلغ المطلوب",
                isDone = hasCopiedAccount,
                isCurrent = !hasCopiedAccount
            )

            ChecklistStepItem(
                stepNumber = 2,
                title = "أرفق إشعار التحويل (صورة أو ملف PDF)",
                isDone = hasAttachedReceipt,
                isCurrent = hasCopiedAccount && !hasAttachedReceipt
            )

            ChecklistStepItem(
                stepNumber = 3,
                title = "اضغط على زر (إرسال إشعار الإيداع للمراجعة) بالأسفل",
                isDone = false,
                isCurrent = hasAttachedReceipt
            )
        }
    }
}

@Composable
private fun ChecklistStepItem(
    stepNumber: Int,
    title: String,
    isDone: Boolean,
    isCurrent: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    when {
                        isDone -> GuideGreenBright
                        isCurrent -> GuideAmber.copy(alpha = 0.2f)
                        else -> Color.Transparent
                    },
                    CircleShape
                )
                .border(
                    width = 1.5.dp,
                    color = when {
                        isDone -> GuideGreenBright
                        isCurrent -> GuideAmber
                        else -> GuideTextSecondary
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(15.dp)
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) GuideAmber else GuideTextSecondary
                )
            }
        }

        Text(
            text = if (isCurrent) "👈 $title" else title,
            fontSize = 12.sp,
            fontWeight = if (isCurrent || isDone) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isDone -> GuideGreenBright
                isCurrent -> GuideTextPrimary
                else -> GuideTextSecondary
            }
        )
    }
}

/**
 * 3. بطاقة توضيح الفرق بين (التنزيل) و(التثبيت) في خدمة تفعيل التطبيق عن بُعد
 */
@Composable
fun ApkInstallStepByStepCard(
    appName: String,
    onDownloadClick: () -> Unit,
    onManualCheckClick: () -> Unit
) {
    var hasClickedDownload by remember { mutableStateOf(false) }

    Surface(
        color = GuideAmber.copy(alpha = 0.10f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, GuideAmber.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "⚠️ كيف تثبّت تطبيق ($appName) ليفتح لك زر استكمال التفعيل؟",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GuideAmber
            )

            // الخطوة الأولى: التنزيل
            Surface(
                color = GuideSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "الخطوة 1: تنزيل ملف التطبيق",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GuideGreenBright
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اضغط على الزر الأصفر أدناه لتحميل تطبيق ($appName) من متجر Abdullah.",
                        fontSize = 12.sp,
                        color = GuideTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            hasClickedDownload = true
                            onDownloadClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GuideAmber)
                    ) {
                        Text(
                            text = "1. اضغط هنا لتحميل تطبيق $appName",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            // الخطوة الثانية: التثبيت الفعلي على الهاتف
            Surface(
                color = GuideSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = if (hasClickedDownload) BorderStroke(1.5.dp, GuideGreenBright) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "الخطوة 2: تثبيت التطبيق على جهازك (مهم جداً)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasClickedDownload) GuideGreenBright else GuideAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "مجرد تنزيل الملف لا يكفي! بعد اكتمال التنزيل:\n" +
                            "• اسحب شاشة الإشعارات من الأعلى واضغط على الملف المحمّل.\n" +
                            "• اضغط على كلمة (تثبيت / Install) وانتظر حتى ينتهي.\n" +
                            "• ارجع إلى هذه الشاشة وسيظهر لك زر (استكمال التفعيل) فوراً.",
                        fontSize = 12.sp,
                        color = GuideTextPrimary,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onManualCheckClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GuideGreenBright)
                    ) {
                        Text(
                            text = "ثبّتت التطبيق الآن؟ اضغط هنا للتحقق الفوري",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GuideGreenBright
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4. نافذة منبثقة مصورة بالخطوات المرقمة لشرح طريقة إرفاق أو مشاركة سند الإيداع
 */
@Composable
fun VisualReceiptHelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GuideSurface,
            border = BorderStroke(1.2.dp, GuideGreenBright.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "💡 كيف ترفق إشعار الإيداع بسهولة؟",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GuideGreenBright
                )

                Text(
                    text = "يمكنك إرسال الإشعار بإحدى الطريقتين التاليتين:",
                    fontSize = 13.sp,
                    color = GuideTextSecondary
                )

                Surface(
                    color = GuideSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "الطريقة الأولى (الأسهل - تصوير الشاشة):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GuideAmber
                        )
                        Text(
                            text = "1. بعد التحويل في تطبيق العمقي، التقط صورة للشاشة (Screenshot) لإشعار التحويل.\n" +
                                "2. ارجع لتطبيقنا واضغط على زر (🖼️ اختيار صورة).\n" +
                                "3. اختر صورة الإشعار ثم اضغط (إرسال إشعار الإيداع).",
                            fontSize = 12.sp,
                            color = GuideTextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Surface(
                    color = GuideSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "الطريقة الثانية (مشاركة PDF مباشرة):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GuideGreenBright
                        )
                        Text(
                            text = "1. من داخل شاشة الإشعار في تطبيق العمقي، اضغط على زر (مشاركة الإشعار).\n" +
                                "2. اختر تطبيقنا (Fix كود الواتساب) من قائمة التطبيقات.\n" +
                                "3. سيفتح تطبيقنا تلقائياً وتظهر صورة الإشعار جاهزة، فقط اضغط (إرسال).",
                            fontSize = 12.sp,
                            color = GuideTextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GuideGreenPrimary)
                ) {
                    Text("فهمت الخطوات، العودة للإيداع", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
