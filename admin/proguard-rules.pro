# ===================================================================
# 1. إعدادات Crashlytics والانعكاس (Reflection)
# ===================================================================
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ===================================================================
# 2. حماية نماذج الأدمن والموديول المشترك لـ Firebase Realtime Database
# ===================================================================
-keep class com.code.w.admin.repository.AdminSupportRequest { *; }
-keepnames class com.code.w.admin.repository.AdminSupportRequest { *; }

-keep class com.code.w.common.model.** { *; }
-keepnames class com.code.w.common.model.** { *; }
-keepclassmembers enum com.code.w.common.model.** { *; }

# حماية كلاسات حالة الواجهة ونتائج قراءة السندات
-keep class com.code.w.admin.viewmodel.AdminDashboardUiState { *; }
-keep class com.code.w.admin.util.OcrExtractionResult { *; }

# ===================================================================
# 3. قواعد حماية مكتبة ML Kit Text Recognition (قراءة السندات OCR)
# ===================================================================
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.odml.**

# ===================================================================
# 4. قواعد توافق Coil و Coroutines و Firebase مع R8
# ===================================================================
-dontwarn coil.**
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
