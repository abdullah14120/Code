package com.code.w.admin.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.code.w.admin.MainActivity

object AdminNotificationHelper {
    private const val CHANNEL_ID = "admin_alerts_channel_v1"
    private const val CHANNEL_NAME = "تنبيهات الطلبات والسندات الجديدة"

    fun notifyNewRequest(context: Context, requestId: String, phoneNumber: String) {
        showNotification(
            context = context,
            notificationId = requestId.hashCode(),
            title = "🔔 طلب معالجة جديد!",
            body = "وصل طلب جديد للرقم: $phoneNumber — بانتظار المراجعة والقبول."
        )
    }

    fun notifyReceiptUploaded(
        context: Context,
        requestId: String,
        phoneNumber: String,
        fileType: String
    ) {
        val attachmentLabel = if (fileType == "PDF") "ملف PDF" else "صورة إشعار"
        showNotification(
            context = context,
            notificationId = (requestId + "_receipt").hashCode(),
            title = "💳 تم إرفاق سند إيداع جديد!",
            body = "قام صاحب الرقم ($phoneNumber) برفع $attachmentLabel — بانتظار تأكيدك."
        )
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        createNotificationChannel(context)

        // تشغيل صوت التنبيه مباشرة حتى لو كان التطبيق مفتوحاً أمام الأدمن
        playAlertSound(context)

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
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun playAlertSound(context: Context) {
        runCatching {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات صوتية فورية عند وصول طلب جديد أو إرفاق سند إيداع"
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
