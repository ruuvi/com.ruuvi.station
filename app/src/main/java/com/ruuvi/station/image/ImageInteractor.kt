package com.ruuvi.station.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import coil.ImageLoader
import coil.request.ImageRequest
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ImageInteractor (
    private val context: Context
    ) {
    private fun getExternalFilesDir() =
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

    suspend fun downloadImage(filename: String, url: String): File {
        return suspendCoroutine { continuation ->
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .target(
                    onStart = { placeholder ->
                        // Handle the placeholder drawable.
                    },
                    onSuccess = { result ->
                        val bitmap = (result as BitmapDrawable).bitmap
                        val storageDir = getExternalFilesDir()
                        val imageFile = File(storageDir, "$filename.jpg")
                        val out = FileOutputStream(imageFile)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        out.flush()
                        out.close()
                        continuation.resume(imageFile)
                    },
                    onError = {
                        continuation.resumeWithException(Exception("Failed to load image $url"))
                    }
                )
                .build()
            imageLoader.enqueue(request)
        }
    }

    fun isImage(uri: Uri?): Boolean {
        val mime = uri?.let { getMimeType(it) }
        return mime == "jpeg" || mime == "jpg" || mime == "png"
    }

    fun getMimeType(uri: Uri): String? {
        val contentResolver = context.contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    fun getCameraPhotoOrientation(file: Uri?): Int {
        var rotate = 0
        file?.let {
            try {
                context.contentResolver.openInputStream(file).use { inputStream ->
                    val exif = ExifInterface(inputStream!!)

                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Could not get orientation of image")
            }
        }

        return rotate
    }

    fun getImage(imageUri: Uri): Bitmap? {
        try {
            return if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(
                    context.contentResolver,
                    imageUri
                )
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get image $imageUri")
            return null
        }
    }

    fun resize(filename: String?, uri: Uri?, rotation: Int) {
        try {
            val targetHeight = 1440
            val targetWidth = 960

            var bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

            bitmap = rotate(bitmap, rotation.toFloat())

            var out: Bitmap

            out = if ((targetHeight.toFloat() / bitmap.height.toFloat() * bitmap.width).toInt() > targetWidth) {
                Bitmap.createScaledBitmap(bitmap, (targetHeight.toFloat() / bitmap.height.toFloat() * bitmap.width).toInt(), targetHeight, false)
            } else {
                Bitmap.createScaledBitmap(bitmap, targetWidth, (targetWidth.toFloat() / bitmap.width.toFloat() * bitmap.height).toInt(), false)
            }

            var x = out.width / 2 - targetWidth / 2

            if (x < 0) x = 0

            out = Bitmap.createBitmap(out, x, 0, targetWidth, targetHeight)

            val file = filename?.let { File(it) }

            val outputStream = FileOutputStream(file)

            out.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)

            outputStream.flush()

            outputStream.close()

            bitmap.recycle()

            out.recycle()
        } catch (e: Exception) {
            Timber.e(e, "Could not resize background image")
        }
    }

    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}