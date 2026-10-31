import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.database();
const storage = admin.storage();

// 1. إرسال إشعار Push للمستخدم عند تغير حالة الطلب
export const onRequestStatusChanged = functions.database
  .ref("/requests/{requestId}/status")
  .onUpdate(async (change, context) => {
    const newStatus = change.after.val() as string;
    const oldStatus = change.before.val() as string;

    if (newStatus === oldStatus) return null;

    const notifyStatuses: Record<string, { title: string; body: string }> = {
      APPROVED: {
        title: "تم تأكيد إيداعك بنجاح",
        body: "قام فريق الإدارة بمراجعة وتأكيد سند الإيداع الخاص بك.",
      },
      PROCESSING: {
        title: "طلبك قيد التنفيذ",
        body: "جاري الآن تنفيذ طلبك، يرجى الانتظار قليلاً.",
      },
      COMPLETED: {
        title: "اكتمل طلبك بنجاح!",
        body: "تم إنجاز طلبك بالكامل. شكراً لتعاملك معنا.",
      },
      REJECTED: {
        title: "تم رفض الطلب",
        body: "تعذر قبول السند المرفق. يرجى مراجعة التطبيق أو التواصل مع الدعم.",
      },
    };

    const notificationMessage = notifyStatuses[newStatus];
    if (!notificationMessage) return null;

    const requestId = context.params.requestId;
    const requestSnap = await db.ref(`/requests/${requestId}`).get();
    const requestData = requestSnap.val();
    if (!requestData || !requestData.ownerUid) return null;

    const fcmTokenSnap = await db.ref(`/users_fcm/${requestData.ownerUid}`).get();
    const fcmToken = fcmTokenSnap.val() as string | null;
    if (!fcmToken) return null;

    await admin.messaging().send({
      token: fcmToken,
      notification: {
        title: notificationMessage.title,
        body: notificationMessage.body,
      },
      data: {
        requestId: requestId,
        status: newStatus,
      },
    });

    return null;
  });

// 2. مهمة مجدولة يومياً لحذف الطلبات المكتملة والسندات الأقدم من 30 يوماً
export const cleanupOldCompletedRequests = functions.pubsub
  .schedule("every 24 hours")
  .onRun(async () => {
    const RETENTION_DAYS = 30;
    const cutoffTimestamp = Date.now() - RETENTION_DAYS * 24 * 60 * 60 * 1000;

    const oldRequestsSnap = await db
      .ref("/requests")
      .orderByChild("completedAt")
      .startAt(1)
      .endAt(cutoffTimestamp)
      .get();

    if (!oldRequestsSnap.exists()) return null;

    const bucket = storage.bucket();
    const deletions: Promise<unknown>[] = [];

    oldRequestsSnap.forEach((childSnap) => {
      const req = childSnap.val();
      if (req.status === "COMPLETED" || req.status === "REJECTED") {
        // حذف ملف السند من Firebase Storage إن وجد
        if (req.receiptStoragePath) {
          deletions.push(
            bucket
              .file(req.receiptStoragePath)
              .delete()
              .catch(() => null)
          );
        }
        // حذف سجل الطلب من Realtime Database
        deletions.push(childSnap.ref.remove());
      }
    });

    await Promise.all(deletions);
    return null;
  });

// 3. منح صلاحية Custom Claim (admin: true) لحساب الأدمن
export const setAdminClaim = functions.https.onCall(async (data, context) => {
  // يُسمح فقط للأدمن الحالي بإضافة أدمن جديد (أو يمكن تشغيلها مرة واحدة لتهيئة أول أدمن)
  if (!context.auth || context.auth.token.admin !== true) {
    throw new functions.https.HttpsError("permission-denied", "غير مصرح لك بهذا الإجراء.");
  }
  const targetUid = data.uid as string;
  await admin.auth().setCustomUserClaims(targetUid, { admin: true });
  return { success: true };
});
