package com.code.w.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class PrepareZipResult {
    data class Ready(val launchIntent: Intent) : PrepareZipResult()
    data class Error(val errorMessage: String) : PrepareZipResult()
}

object SmartDataRestoreManager {

    private const val ACTION_SMART_RESTORE = "com.code.w.action.SMART_RESTORE_DATA"
    private const val TARGET_RESTORE_ACTIVITY = "com.code.w.bridge.SmartRestoreActivity"

    /**
     * يقوم بتحميل ملف الـ ZIP من الرابط المرسل من الأدمن وتجهيز Intent آمن لإرساله للحزمة المستهدفة بدون روت
     */
    suspend fun downloadAndPrepareRestoreIntent(
        context: Context,
        zipUrl: String,
        targetPackage: String,
        onProgressStep: (String) -> Unit
    ): PrepareZipResult = withContext(Dispatchers.IO) {
        try {
            if (zipUrl.isBlank()) {
                return@withContext PrepareZipResult.Error("رابط ملف التفعيل (ZIP) غير متوفر بعد من الإدارة.")
            }

            onProgressStep("1/2 جاري تحميل ملف التفعيل (ZIP) من الخادم...")
            val zipDir = File(context.cacheDir, "activation_zips").apply { mkdirs() }
            val zipFile = File(zipDir, "restore_${targetPackage}.zip")

            if (zipFile.exists()) zipFile.delete()

            val downloaded = downloadFile(zipUrl, zipFile)
            if (!downloaded || !zipFile.exists() || zipFile.length() == 0L) {
                return@withContext PrepareZipResult.Error("فشل تحميل ملف الـ ZIP، يرجى التحقق من اتصال الإنترنت وإعادة المحاولة.")
            }

            onProgressStep("2/2 جاري إرسال البيانات للحزمة ($targetPackage) وتطبيق التفعيل...")

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )

            // منح الحزمة المستهدفة صلاحية قراءة ملف الـ ZIP المؤقت
            context.grantUriPermission(
                targetPackage,
                contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            val restoreIntent = Intent(ACTION_SMART_RESTORE).apply {
                component = ComponentName(targetPackage, TARGET_RESTORE_ACTIVITY)
                setDataAndType(contentUri, "application/zip")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // التحقق من أن الحزمة المستهدفة تحتوي على كود الاستقبال (SmartRestoreActivity)
            val resolveInfo = context.packageManager.resolveActivity(restoreIntent, 0)
            if (resolveInfo == null) {
                return@withContext PrepareZipResult.Error(
                    "النسخة المثبتة من ($targetPackage) لا تحتوي على مستقبل التفعيل الذكي. يرجى تحميل أحدث نسخة من متجر Abdullah."
                )
            }

            PrepareZipResult.Ready(restoreIntent)
        } catch (e: Exception) {
            PrepareZipResult.Error("حدث خطأ أثناء تجهيز ملف التفعيل: ${e.localizedMessage ?: "خطأ غير معروف"}")
        }
    }

    private fun downloadFile(urlStr: String, destination: File): Boolean {
        return try {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 30000
                instanceFollowRedirects = true
                connect()
            }
            if (connection.responseCode !in 200..299) return false

            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        output.write(buffer, 0, count)
                    }
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
