package com.code.w.admin.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.code.w.admin.MainActivity
import com.code.w.admin.util.AdminNotificationHelper
import com.code.w.common.model.DatabaseKeys
import com.code.w.common.model.RequestStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminMonitorService : Service() {

    private var databaseRef: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val knownStatuses = mutableMapOf<String, String>()
    private var isInitialLoad = true

    override fun onCreate() {
        super.onCreate()
        acquirePartialWakeLock()
        startForegroundWithNotification()
        startDatabaseListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun acquirePartialWakeLock() {
        runCatching {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SupportAdmin::DatabaseMonitorWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun startForegroundWithNotification() {
        createServiceChannel()

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AdminMonitorService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("مراقبة الطلبات والسندات نشطة")
            .setContentText("متصل بقاعدة البيانات لتنبيهك فور وصول طلب أو سند إيداع")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "إيقاف المراقبة",
                stopPendingIntent
            )
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, notification)
        }
    }

    private fun startDatabaseListening() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener {
                attachFirebaseListener()
            }
        } else {
            attachFirebaseListener()
        }
    }

    private fun attachFirebaseListener() {
        if (valueEventListener != null) return

        val ref = FirebaseDatabase.getInstance().getReference(DatabaseKeys.NODE_REQUESTS)
        ref.keepSynced(true)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isInitialLoad) {
                    snapshot.children.forEach { child ->
                        val id = child.child(DatabaseKeys.FIELD_ID).getValue(String::class.java)
                            ?: child.key ?: return@forEach
                        val status = child.child(DatabaseKeys.FIELD_STATUS).getValue(String::class.java)
                            ?: RequestStatus.SUBMITTED.name
                        knownStatuses[id] = status
                    }
                    isInitialLoad = false
                    return
                }

                val currentIds = mutableSetOf<String>()
                snapshot.children.forEach { child ->
                    val id = child.child(DatabaseKeys.FIELD_ID).getValue(String::class.java)
                        ?: child.key ?: return@forEach
                    val phone = child.child(DatabaseKeys.FIELD_PHONE_NUMBER).getValue(String::class.java).orEmpty()
                    if (phone.isBlank()) return@forEach

                    val status = child.child(DatabaseKeys.FIELD_STATUS).getValue(String::class.java)
                        ?: RequestStatus.SUBMITTED.name
                    val fileType = child.child(DatabaseKeys.FIELD_RECEIPT_FILE_TYPE).getValue(String::class.java).orEmpty()

                    currentIds.add(id)
                    val previousStatus = knownStatuses[id]

                    if (previousStatus == null && status == RequestStatus.SUBMITTED.name) {
                        AdminNotificationHelper.notifyNewRequest(
                            context = applicationContext,
                            requestId = id,
                            phoneNumber = phone
                        )
                    } else if (previousStatus != RequestStatus.RECEIPT_SUBMITTED.name &&
                        status == RequestStatus.RECEIPT_SUBMITTED.name
                    ) {
                        AdminNotificationHelper.notifyReceiptUploaded(
                            context = applicationContext,
                            requestId = id,
                            phoneNumber = phone,
                            fileType = fileType
                        )
                    }

                    knownStatuses[id] = status
                }

                knownStatuses.keys.retainAll(currentIds)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        ref.addValueEventListener(listener)
        databaseRef = ref
        valueEventListener = listener
    }

    private fun createServiceChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "خدمة المراقبة في الخلفية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تحافظ على اتصال تطبيق الأدمن بقاعدة البيانات عند إغلاق الشاشة"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        valueEventListener?.let { databaseRef?.removeEventListener(it) }
        valueEventListener = null
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val SERVICE_CHANNEL_ID = "admin_foreground_monitor_channel"
        private const val SERVICE_NOTIFICATION_ID = 9001
        const val ACTION_STOP_SERVICE = "com.code.w.admin.STOP_MONITOR_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, AdminMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
