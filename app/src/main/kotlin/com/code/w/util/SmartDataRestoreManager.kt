package com.code.w.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

sealed class RestoreResult {
    data class Success(val message: String) : RestoreResult()
    data class Error(val errorMessage: String) : RestoreResult()
}

object SmartDataRestoreManager {

    /**
     * تحميل ملف الـ ZIP من الرابط المرسل من الأدمن ثم فك ضغطه وحقنه داخل /data/data/<targetPackage>
     */
    suspend fun downloadAndRestorePackageData(
        context: Context,
        zipUrl: String,
        targetPackage: String,
        onProgressStep: (String) -> Unit
    ): RestoreResult = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "restore_work_${System.currentTimeMillis()}")
        val downloadedZip = File(context.cacheDir, "activation_${targetPackage}.zip")

        try {
            if (zipUrl.isBlank()) {
                return@withContext RestoreResult.Error("رابط ملف التفعيل (ZIP) غير متوفر بعد من الإدارة.")
            }

            // 1. التحقق من صلاحية الروت (su) قبل البدء
            onProgressStep("1/5 جاري التحقق من صلاحيات النظام (Root)...")
            if (!checkRootAccess()) {
                return@withContext RestoreResult.Error("تعذر الحصول على صلاحية الروت (Root). يرجى السماح للتطبيق بصلاحية Superuser ثم إعادة المحاولة.")
            }

            // 2. تحميل ملف الـ ZIP
            onProgressStep("2/5 جاري تحميل ملف التفعيل (ZIP)...")
            val downloadSuccess = downloadFile(zipUrl, downloadedZip)
            if (!downloadSuccess || !downloadedZip.exists() || downloadedZip.length() == 0L) {
                return@withContext RestoreResult.Error("فشل تحميل ملف الـ ZIP، يرجى التحقق من اتصال الإنترنت وإعادة المحاولة.")
            }

            // 3. فك ضغط ملف الـ ZIP في مجلد مؤقت وتحليل هيكلته بذكاء
            onProgressStep("3/5 جاري فك ضغط ملف التفعيل وفحص البيانات...")
            if (workDir.exists()) workDir.deleteRecursively()
            workDir.mkdirs()
            unzipSafely(downloadedZip, workDir)

            // إذا كان داخل الـ ZIP مجلد واحد فقط يحمل اسم الحزمة أو اسم عام، ندخل داخله تلقائياً
            val extractedRoot = resolveActualDataRoot(workDir, targetPackage)
            val extractedChildren = extractedRoot.listFiles()
            if (extractedChildren.isNullOrEmpty()) {
                return@withContext RestoreResult.Error("ملف الـ ZIP المرفق فارغ أو لا يحتوي على بيانات صالحة.")
            }

            // 4. إيقاف الحزمة إجبارياً ومسح محتويات /data/data/<package> واستبدالها
            onProgressStep("4/5 جاري إيقاف ($targetPackage) واستبدال بيانات Data/Data...")
            val stagingPath = extractedRoot.absolutePath
            val targetDataPath = "/data/data/$targetPackage"

            val rootScript = """
                set -e
                # إيقاف إجباري للحزمة المستهدفة
                am force-stop $targetPackage
                
                # التأكد من وجود مجلد البيانات الأساسي للحزمة
                if [ ! -d "$targetDataPath" ]; then
                    exit 2
                fi
                
                # استخراج UID و GID الخاص بالحزمة قبل الحذف لتعيين الصلاحيات الصحيحة لاحقاً
                APP_UID=${'$'}(stat -c '%u' "$targetDataPath")
                APP_GID=${'$'}(stat -c '%g' "$targetDataPath")
                
                # حذف جميع محتويات /data/data/<package> ما عدا مجلد المكتبات الأصلية lib
                for item in "$targetDataPath"/* "$targetDataPath"/.*; do
                    base_name=${'$'}(basename "${'$'}item")
                    if [ "${'$'}base_name" != "." ] && [ "${'$'}base_name" != ".." ] && [ "${'$'}base_name" != "lib" ]; then
                        rm -rf "${'$'}item"
                    fi
                done
                
                # نسخ جميع البيانات المستخرجة من ملف الـ ZIP إلى /data/data/<package>
                cp -rf "$stagingPath"/. "$targetDataPath"/
                
                # إصلاح ملكية جميع الملفات والمجلدات (chown) وصلاحيات القراءة والكتابة (chmod) وسياق SELinux (restorecon)
                chown -R "${'$'}APP_UID:${'$'}APP_GID" "$targetDataPath"
                chmod -R 771 "$targetDataPath"
                restorecon -R "$targetDataPath" >/dev/null 2>&1 || true
            """.trimIndent()

            val exitCode = executeRootScript(rootScript)
            if (exitCode == 2) {
                return@withContext RestoreResult.Error("لم يتم العثور على مجلد بيانات للحزمة ($targetPackage). يرجى فتح التطبيق مرة واحدة على الأقل أو التأكد من تثبيته.")
            } else if (exitCode != 0) {
                return@withContext RestoreResult.Error("فشل استبدال ملفات البيانات (كود الخطأ: $exitCode). يرجى الضغط على زر إعادة المحاولة.")
            }

            onProgressStep("5/5 اكتملت استعادة البيانات وتفعيل التطبيق بنجاح ✓")
            RestoreResult.Success("تم إيقاف الحزمة ($targetPackage) واستبدال بيانات التفعيل بنجاح تام!")
        } catch (e: Exception) {
            RestoreResult.Error("حدث خطأ أثناء عملية الاستعادة: ${e.localizedMessage ?: "خطأ غير معروف"}")
        } finally {
            runCatching { workDir.deleteRecursively() }
            runCatching { downloadedZip.delete() }
        }
    }

    /**
     * يكتشف بذكاء ما إذا كانت ملفات البيانات (مثل shared_prefs و databases) موجودة مباشرة
     * أو داخل مجلد فرعي داخل ملف الـ ZIP
     */
    private fun resolveActualDataRoot(extractDir: File, targetPackage: String): File {
        val knownAndroidDataDirs = setOf("shared_prefs", "databases", "files", "cache", "app_webview", "no_backup")
        val directFiles = extractDir.listFiles() ?: return extractDir

        // إذا كانت المجلدات المعروفة موجودة مباشرة في الجذر
        if (directFiles.any { it.isDirectory && it.name in knownAndroidDataDirs }) {
            return extractDir
        }

        // إذا كان هناك مجلد باسم الحزمة أو مجلد وحيد داخله البيانات
        val pkgFolder = File(extractDir, targetPackage)
        if (pkgFolder.exists() && pkgFolder.isDirectory) {
            return pkgFolder
        }

        val singleSubDir = directFiles.singleOrNull { it.isDirectory && !it.name.startsWith("__MACOSX") }
        if (singleSubDir != null) {
            return singleSubDir
        }

        return extractDir
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

    private fun unzipSafely(zipFile: File, targetDir: File) {
        val canonicalTarget = targetDir.canonicalPath
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            val buffer = ByteArray(8192)
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (!outFile.canonicalPath.startsWith(canonicalTarget)) {
                    throw SecurityException("Invalid Zip Entry path")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun checkRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            exitCode == 0 && output.contains("uid=0")
        } catch (_: Exception) {
            false
        }
    }

    private fun executeRootScript(script: String): Int {
        return try {
            val process = Runtime.getRuntime().exec("su")
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(script)
                writer.write("\nexit\n")
                writer.flush()
            }
            process.waitFor()
        } catch (_: Exception) {
            -1
        }
    }
}
