package com.code.w.admin.repository

/**
 * كلاس البيانات الخاص بلوحة تحكم الأدمن
 * تم تصميمه ليتوافق بنيوياً مع البيانات المرفوعة من تطبيق المستخدم
 */
data class AdminSupportRequest(
    val id: String = "",
    val phoneNumber: String = "",
    val issueType: String = "",
    val status: String = "SUBMITTED", // يتم التعامل مع الحالة كـ String لتسهيل فحص النص في الـ when
    val bankDetails: String = "",
    val adminNotes: String = "",
    val receiptImageUrl: String = "",
    val timerEndTime: Long = 0L // الحقل الجديد المانح للوقت المستقبلي للعداد التنازلي
)
