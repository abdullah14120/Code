# الحفاظ على نماذج البيانات المشتركة وحالات الطلب لـ Firebase Realtime Database
-keep class com.code.w.common.model.** { *; }
-keepnames class com.code.w.common.model.** { *; }

# الحفاظ على معلومات الأسطر لـ Crashlytics حتى تظهر تقارير الأخطاء بدقة
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
