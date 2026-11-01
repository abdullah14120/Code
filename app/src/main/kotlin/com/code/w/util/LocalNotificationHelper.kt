package com.code.w.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.code.w.MainActivity
import com.code.w.model.SupportRequest

object LocalNotificationHelper {
    private const val CHANNEL_ID = "support_status_channel"
    private const val CHANNEL_NAME = "تحديثات حالة الطلب"

    fun showStatusNotification(
        context: Context,
        status: SupportRequest.Status,
        phoneNumber: String
    ) {
        createNotificationChannel(context)

        val (title, body) = when (status) {
            SupportRequest.Status.PRE_APPROVED ->
                "تم قبول طلبك للمراجعة ✓" to "جاري الآن فحص الرقم ($phoneNumber)، يرجى متابعة العداد."
            SupportRequest.Status.APPROVED ->
                "مطلوب تأكيد الإيداع 💳" to "تمت مراجعة الرقم ($phoneNumber)، يرجى إرفاق إشعار الإيداع لإتمام الحل."
            SupportRequest.Status.PROCESSING ->
                "تم تأكيد إيداعك بنجاح ✓" to "جاري الآن معالجة مشكلة كود التحقق للرقم ($phoneNumber)."
            SupportRequest.Status.COMPLETED ->
                "تم حل المشكلة بنجاح! 🎉" to "اكتملت معالجة الرقم ($phoneNumber)، افتح التطبيق لقراءة التعليمات."
            SupportRequest.Status.REJECTED ->
                "تنبيه بخصوص طلبك ⚠️" to "تم تحديث حالة الطلب للرقم ($phoneNumber)، يرجى مراجعة الملاحظة."
            else -> return
        }

        // التحقق من إذن الإشعارات في أندرويد 13 فما فوق
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(status.ordinal + 100, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية عند تحديث حالة معالجة الرقم من الإدارة"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
