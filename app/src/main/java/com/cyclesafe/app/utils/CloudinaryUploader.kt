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

    fun getTransformedImageUrl(originalUrl: String): String {
        val parts = originalUrl.split("/upload/")
        if (parts.size == 2) {
            return "${parts[0]}/upload/w_256,h_256,c_fill,g_face/${parts[1]}"
        }
        return originalUrl
    }
}