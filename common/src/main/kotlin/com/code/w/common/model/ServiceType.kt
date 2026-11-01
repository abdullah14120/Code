package com.code.w.common.model

enum class ServiceType(
    val title: String,
    val subtitle: String,
    val stepperSteps: List<String>,
    val processingSteps: List<String>,
    val requiresDeposit: Boolean = true,
    val quickAdminNotes: List<String>
) {
    // الخدمة الأولى (الحالية)
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
            "تم تنشيط استقبال المكالمات والرسائل لرقمك بنجاح، يمكنك التسجيل الآن.",
            "تم فك تعليق الكود لرقم هاتفك بنجاح، يرجى الانتظار 5 دقائق ثم طلب الكود."
        )
    ),

    // الخدمة الثانية (مثال: خدمة فك الحظر بخطوات مختلفة)
    UNBAN_NUMBER(
        title = "فك حظر الرقم واستعادة الحساب",
        subtitle = "معالجة حظر الرقم المؤقت أو الدائم وإعادة تفعيل الحساب",
        stepperSteps = listOf("الطلب", "فحص الحظر", "الرسوم", "رفع الحظر", "التفعيل"),
        processingSteps = listOf(
            "مراجعة سجل الرقم وتحديد نوع الحظر المفروض",
            "رفع طلب استئناف فني لإزالة الحظر من الخادم",
            "تأكيد فك الحظر وتجهيز تعليمات الدخول الآمن"
        ),
        requiresDeposit = true,
        quickAdminNotes = listOf(
            "تم فك الحظر عن رقمك بنجاح! يمكنك الآن التسجيل في النسخة الرسمية.",
            "تم رفع القيد عن الرقم، يرجى الانتظار 10 دقائق قبل طلب كود التفعيل.",
            "اكتملت عملية استعادة الرقم بنجاح، تجنب استخدام نسخ معدلة."
        )
    ),

    // الخدمة الثالثة (مثال: خدمة ذات مسار مختصر من 4 خطوات بدون شاشة إيداع)
    FAST_CHECK(
        title = "فحص حالة الرقم السريع",
        subtitle = "كشف فني سريع لمعرفة سبب تعليق الرقم دون الحاجة لإيداع",
        stepperSteps = listOf("الطلب", "الفحص", "التشخيص", "النتيجة"),
        processingSteps = listOf(
            "استلام الرقم في وحدة الفحص المباشر",
            "اختبار استجابة البوابة البرمجية للرقم",
            "إصدار تقرير الفحص النهائي للمستخدم"
        ),
        requiresDeposit = false,
        quickAdminNotes = listOf(
            "نتيجة الفحص: الرقم سليم ولا يوجد عليه حظر، يرجى فقط إعادة تشغيل الجهاز.",
            "نتيجة الفحص: الرقم مقيد مؤقتاً لكثرة المحاولات، سينتهي القيد تلقائياً بعد ساعتين."
        )
    );

    companion object {
        fun fromTitle(title: String?): ServiceType {
            return entries.firstOrNull { it.title == title?.trim() } ?: VERIFICATION_CODE
        }
    }
}
