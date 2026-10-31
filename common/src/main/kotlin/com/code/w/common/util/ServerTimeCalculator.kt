package com.code.w.common.util

object ServerTimeCalculator {
    /**
     * يحسب الوقت المتبقي بالملي ثانية بناءً على وقت بداية العداد المسجل من الخادم (ServerValue.TIMESTAMP)
     * مع تعويض فرق توقيت جهاز المستخدم عن خادم Firebase.
     */
    fun calculateRemainingMs(
        timerStartTimeServerMs: Long,
        timerDurationMs: Long,
        localCurrentTimeMs: Long,
        serverTimeOffsetMs: Long
    ): Long {
        if (timerStartTimeServerMs <= 0L || timerDurationMs <= 0L) return 0L
        val estimatedServerNow = localCurrentTimeMs + serverTimeOffsetMs
        val endTimeServerMs = timerStartTimeServerMs + timerDurationMs
        return (endTimeServerMs - estimatedServerNow).coerceAtLeast(0L)
    }
}
