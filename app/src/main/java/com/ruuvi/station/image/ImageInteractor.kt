package com.ruuvi.station.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import coil.imageLoader
import coil.request.ImageRequest
import com.ruuvi.station.R
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.random.Random
import kotlin.random.asJavaRandom

class ImageInteractor (
    private val context: Context
) {
    val defaultImages = listOf(
        R.drawable.new_bg5,
        R.drawable.new_bg6,
        R.drawable.new_bg7,
        R.drawable.new_bg8,
        R.drawable.new_bg9,
        R.drawable.new_bg10,
        R.drawable.bg2,
        R.drawable.bg3,
        R.drawable.bg4,
        R.drawable.bg5,
        R.drawable.bg6,
        R.drawable.bg7,
        R.drawable.bg8,
        R.drawable.bg9,
        R.drawable.new_bg1,
        R.drawable.new_bg2,
        R.drawable.new_bg3,
        R.drawable.new_bg4,
    )

    val randomImages = listOf(
        R.drawable.new_bg1,
        R.drawable.new_bg2,
        R.drawable.new_bg3,
        R.drawable.new_bg4,
    )

    private fun getExternalFilesDir() =
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

    suspend fun downloadImage(filename: String, url: String): File {
        return suspendCoroutine { continuation ->
            val imageLoader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(url)
                .target(
                    onStart = { },
                    onSuccess = { result ->
                        val bitmap = (result as BitmapDrawable).bitmap
                        val storageDir = getExternalFilesDir()
                        val imageFile = File(storageDir, "$filename.jpg")

                        var output: FileOutputStream? = null
                        try {
                            output = FileOutputStream(imageFile)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                            continuation.resume(imageFile)
                        } catch (e: Exception) {
                            Timber.e(e)
                            continuation.resumeWithException(Exception("Failed to save image $url"))
                        } finally {
                            output?.flush()
                            output?.close()
                        }
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

    //TODO filename and uri should become 1 param
    fun resize(filename: String?, uri: Uri?, rotation: Int) {
        try {
            val targetWidth = 1080

            var bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

            bitmap = rotate(bitmap, rotation.toFloat())

            Timber.d("Original ${bitmap.width} x ${bitmap.height} rotation = $rotation")

            val out = Bitmap.createScaledBitmap(bitmap, targetWidth, (targetWidth.toFloat() / bitmap.width.toFloat() * bitmap.height).toInt(), false)

            Timber.d("Scaled ${out.width} x ${out.height}")

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

    fun saveResourceAsFile(sensorId: String, resourceImage: Int): File? {
        val image = createFile(sensorId, ImageSource.DEFAULT)
        val bitmap = BitmapFactory.decodeResource(
            context.resources,
            resourceImage
        )

        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(image)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        } catch (e: Exception) {
            Timber.e(e)
          return null
        } finally {
            output?.flush()
            output?.close()
        }
        return image
    }

    fun getFilename(sensorId: String, source: ImageSource): String {
        val name = sensorId.replace(":","")
        return if (source == ImageSource.CLOUD) {
            val random = abs(Random.asJavaRandom().nextLong())
            "${source.prefix}_${name}_${random}"
        } else {
            "${source.prefix}_${name}_"
        }
    }

    fun createFile(name: String, imageSource: ImageSource): File {
        return File.createTempFile(getFilename(name, imageSource), ".jpg", getExternalFilesDir())
    }

    fun createFileForCamera(sensorId: String): Pair<File,Uri> {
        val image = createFile(sensorId, ImageSource.CAMERA)
        return image to FileProvider.getUriForFile(
            context,
            "com.ruuvi.station.fileprovider",
            image
        )
    }

    fun copyFile(from: Uri, destination: File): Boolean {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(from)
            if (inputStream != null) {
                destination.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                return true
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy file from $from to $destination")
            return false
        } finally {
            inputStream?.close()
        }
        return false
    }

    fun deleteFile(userBackground: String) {
        val file = File(userBackground)
        try {
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete old image file")
        }
    }

    fun getDefaultBackgroundById(number: Int): Int {
        return when (number) {
            1 -> R.drawable.bg2
            2 -> R.drawable.bg3
            3 -> R.drawable.bg4
            4 -> R.drawable.bg5
            5 -> R.drawable.bg6
            6 -> R.drawable.bg7
            7 -> R.drawable.bg8
            8 -> R.drawable.bg9
            else -> getRandomResource()
        }
    }

    fun getRandomResource(): Int = randomImages.random()
}

enum class ImageSource (val prefix: String) {
    CAMERA ("camera"),
    DEFAULT ("default"),
    GALLERY ("gallery"),
    CLOUD ("cloud")
}