package com.cyclesafe.app.utils

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object CloudinaryUploader {

    suspend fun uploadImage(uri: Uri): String? {
        return suspendCoroutine { continuation ->
            MediaManager.get().upload(uri).callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("secure_url") as? String
                    continuation.resume(imageUrl)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    continuation.resume(null)
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
        }
    }
}