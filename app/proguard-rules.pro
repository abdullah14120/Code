# ===================================================================
# 1. إعدادات Crashlytics والانعكاس (Reflection)
# ===================================================================
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ===================================================================
# 2. حماية نماذج البيانات المحلية والمشتركة لـ Firebase Realtime Database
# ===================================================================
-keep class com.code.w.model.** { *; }
-keepnames class com.code.w.model.** { *; }
-keepclassmembers enum com.code.w.model.** { *; }

-keep class com.code.w.common.model.** { *; }
-keepnames class com.code.w.common.model.** { *; }
-keepclassmembers enum com.code.w.common.model.** { *; }

# حماية كلاسات معالجة السندات وإعدادات الإيداع
-keep class com.code.w.util.ProcessedReceipt { *; }
-keep class com.code.w.util.ReceiptValidationResult$* { *; }
-keep class com.code.w.repository.DepositConfig { *; }

# ===================================================================
# 3. حماية WorkManager لرفع السندات تلقائياً في الخلفية
# ===================================================================
-keep class com.code.w.worker.ReceiptUploadWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ===================================================================
# 4. قواعد توافق Coroutines و Firebase مع R8
# ===================================================================
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
