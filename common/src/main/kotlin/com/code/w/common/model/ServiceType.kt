package com.code.w.common.model

enum class ServiceType(
    val title: String,
    val subtitle: String,
    val stepperSteps: List<String>,
    val processingSteps: List<String>,
    val requiresDeposit: Boolean = true,
    val quickAdminNotes: List<String>
) {
    VERIFICATION_CODE(
        title = "مشكلة كود التحقق أو مكالمة فائتة",
        subtitle = "فحص وتنشيط مسار استقبال رمز التحقق (SMS) أو المكالمة الفائتة",
        stepperSteps = listOf("الطلب", "المراجعة", "الإيداع", "المعالجة", "الاكتمال"),
        processingSteps = listOf(
            "التحقق من بيانات الرقم وتأكيد عملية الإيداع",
            "إعادة ضبط مسار كود التحقق والمكالمة الفائتة على السيرفر",
            "إصدار إشعار الحل النهائي وتعليمات التشغيل"
        ),
        requiresDeposit = true,
        quickAdminNotes = listOf(
            "تم حل المشكلة بنجاح، يرجى الآن طلب الكود عبر رسالة نصية SMS وعدم طلب مكالمة.",
            "تم تنشيط استقبال المكالمات والرسائل لرقمك بنجاح، يمكنك التسجيل الآن."
        )
    ),

    REMOTE_APP_ACTIVATION(
        title = "تفعيل تطبيق تابع لنا عن بُعد",
        subtitle = "ربط وتفعيل رقمك على تطبيقاتنا المعتمدة عن بُعد بخطوات تفاعلية",
        stepperSteps = listOf("الطلب", "حالة الرقم", "تثبيت التطبيق", "التفعيل", "الاكتمال"),
        processingSteps = listOf(
            "استلام حالة الرقم والتحقق من تثبيت حزمة Red (com.target.app)",
            "تهيئة جلسة التفعيل عن بُعد وربط الرقم بالحزمة",
            "إصدار إشعار اكتمال التفعيل النهائي"
        ),
        requiresDeposit = false,
        quickAdminNotes = listOf(
            "تم تفعيل رقمك على تطبيق Red بنجاح! يمكنك الآن فتح التطبيق واستخدامه.",
            "يرجى فتح تطبيق Red الآن والبقاء متصلاً بالإنترنت لإتمام المزامنة."
        )
    );

    companion object {
        fun fromTitle(title: String?): ServiceType {
            return entries.firstOrNull { it.title == title?.trim() } ?: VERIFICATION_CODE
        }
    }
}
