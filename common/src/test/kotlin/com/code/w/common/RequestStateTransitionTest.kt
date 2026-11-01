package com.code.w.common

import com.code.w.common.model.RequestStatus
import com.code.w.common.util.ServerTimeCalculator
import org.junit.Assert.*
import org.junit.Test

class RequestStateTransitionTest {

    @Test
    fun `valid state transitions succeed`() {
        assertTrue(RequestStatus.PENDING_UPLOAD.canTransitionTo(RequestStatus.SUBMITTED))
        assertTrue(RequestStatus.SUBMITTED.canTransitionTo(RequestStatus.PRE_APPROVED))
        assertTrue(RequestStatus.PRE_APPROVED.canTransitionTo(RequestStatus.APPROVED))
        assertTrue(RequestStatus.APPROVED.canTransitionTo(RequestStatus.PROCESSING))
        assertTrue(RequestStatus.PROCESSING.canTransitionTo(RequestStatus.COMPLETED))
        assertTrue(RequestStatus.SUBMITTED.canTransitionTo(RequestStatus.REJECTED))
    }

    @Test
    fun `invalid backwards or terminal transitions fail`() {
        // لا يمكن الرجوع من مكتمل أو مرفوض إلى أي حالة أخرى
        assertFalse(RequestStatus.COMPLETED.canTransitionTo(RequestStatus.PROCESSING))
        assertFalse(RequestStatus.REJECTED.canTransitionTo(RequestStatus.APPROVED))
        // لا يمكن الانتقال لنفس الحالة
        assertFalse(RequestStatus.APPROVED.canTransitionTo(RequestStatus.APPROVED))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromString throws exception on typo instead of silently falling back`() {
        // التأكد من اكتشاف الأخطاء الإملائية مثل PRE_APROVED
        RequestStatus.fromString("PRE_APROVED")
    }

    @Test
    fun `isActive returns true only for non-terminal states`() {
        assertTrue(RequestStatus.PENDING_UPLOAD.isActive())
        assertTrue(RequestStatus.SUBMITTED.isActive())
        assertTrue(RequestStatus.PRE_APPROVED.isActive())
        assertTrue(RequestStatus.APPROVED.isActive())
        assertTrue(RequestStatus.PROCESSING.isActive())
        assertFalse(RequestStatus.COMPLETED.isActive())
        assertFalse(RequestStatus.REJECTED.isActive())
    }

    @Test
    fun `server time calculator accurately compensates for device clock skew`() {
        val serverStart = 1_700_000_000_000L
        val duration = 15 * 60 * 1000L // 15 دقيقة
        // لنفترض أن ساعة جهاز المستخدم متأخرة بـ 5 دقائق عن السيرفر
        val localClock = 1_700_000_000_000L + (5 * 60 * 1000L) // يظن أنه مر 5 دقائق فقط
        val serverOffset = 5 * 60 * 1000L // السيرفر متقدم بـ 5 دقائق إضافية (إجمالي الوقت الفعلي 10 دقائق)

        val remaining = ServerTimeCalculator.calculateRemainingMs(
            timerStartTimeServerMs = serverStart,
            timerDurationMs = duration,
            localCurrentTimeMs = localClock,
            serverTimeOffsetMs = serverOffset
        )

        // المتبقي الفعلي يجب أن يكون 5 دقائق فقط وليس 10 دقائق
        assertEquals(5 * 60 * 1000L, remaining)
    }
}
