# ===================================================================
# قواعد الموديول المشترك (:common) التي تُورّث لتطبيقي المستخدم والأدمن
# ===================================================================
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

# الحفاظ على جميع النماذج وسجل الإجراءات (AuditLogEntry) وحالات الطلب (RequestStatus)
-keep class com.code.w.common.model.** { *; }
-keepnames class com.code.w.common.model.** { *; }
-keepclassmembers enum com.code.w.common.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# الحفاظ على أدوات حساب توقيت السيرفر
-keep class com.code.w.common.util.** { *; }
