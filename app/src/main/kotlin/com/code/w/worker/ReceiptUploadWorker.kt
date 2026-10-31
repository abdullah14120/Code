package com.code.w.worker

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.code.w.common.model.DatabaseKeys
import com.code.w.common.model.RequestStatus
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.TimeUnit

class ReceiptUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val requestId = inputData.getString(KEY_REQUEST_ID) ?: return Result.failure()
        val ownerUid = inputData.getString(KEY_OWNER_UID) ?: "anonymous"
        val localFilePath = inputData.getString(KEY_LOCAL_FILE_PATH) ?: return Result.failure()
        val mimeType = inputData.getString(KEY_MIME_TYPE) ?: "image/jpeg"
        val sha256 = inputData.getString(KEY_SHA256) ?: ""
        val fileType = inputData.getString(KEY_FILE_TYPE) ?: "IMAGE"
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "receipt.jpg"

        val file = File(localFilePath)
        if (!file.exists()) return Result.failure()

        return try {
            val storagePath = "receipts/$ownerUid/$requestId/${file.name}"
            val storageRef = FirebaseStorage.getInstance().reference.child(storagePath)
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("sha256", sha256)
                .build()

            storageRef.putFile(Uri.fromFile(file), metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val updates = mapOf<String, Any>(
                DatabaseKeys.FIELD_RECEIPT_URL to downloadUrl,
                DatabaseKeys.FIELD_RECEIPT_STORAGE_PATH to storagePath,
                DatabaseKeys.FIELD_RECEIPT_SHA256 to sha256,
                DatabaseKeys.FIELD_RECEIPT_FILE_TYPE to fileType,
                DatabaseKeys.FIELD_RECEIPT_FILE_NAME to fileName,
                DatabaseKeys.FIELD_STATUS to RequestStatus.RECEIPT_SUBMITTED.name,
                DatabaseKeys.FIELD_REJECTION_REASON to "",
                DatabaseKeys.FIELD_UPDATED_AT to ServerValue.TIMESTAMP
            )

            FirebaseDatabase.getInstance()
                .getReference(DatabaseKeys.NODE_REQUESTS)
                .child(requestId)
                .updateChildren(updates)
                .await()

            file.delete()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val KEY_REQUEST_ID = "request_id"
        private const val KEY_OWNER_UID = "owner_uid"
        private const val KEY_LOCAL_FILE_PATH = "local_file_path"
        private const val KEY_MIME_TYPE = "mime_type"
        private const val KEY_SHA256 = "sha256"
        private const val KEY_FILE_TYPE = "file_type"
        private const val KEY_FILE_NAME = "file_name"

        fun enqueueUpload(
            context: Context,
            requestId: String,
            ownerUid: String,
            localFilePath: String,
            mimeType: String,
            sha256: String,
            fileType: String,
            fileName: String
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                KEY_REQUEST_ID to requestId,
                KEY_OWNER_UID to ownerUid,
                KEY_LOCAL_FILE_PATH to localFilePath,
                KEY_MIME_TYPE to mimeType,
                KEY_SHA256 to sha256,
                KEY_FILE_TYPE to fileType,
                KEY_FILE_NAME to fileName
            )

            val workRequest = OneTimeWorkRequestBuilder<ReceiptUploadWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "upload_receipt_$requestId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
