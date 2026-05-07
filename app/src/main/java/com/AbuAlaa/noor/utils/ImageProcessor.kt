package com.AbuAlaa.noor.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.AbuAlaa.noor.data.MessageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

object ImageProcessor {
    private const val TAG = "ImageProcessor"
    
    data class ProcessorOptions(
        val supportedMimeTypes: List<String> = listOf("image/png", "image/jpeg"),
        val preferredMimeType: String = "image/jpeg",
        val maxSizeInMB: Float = 1f,
        val maxWidth: Int = 1024,
        val maxHeight: Int = 1024,
        val quality: Int = 85
    )
    
    data class ProcessedImage(
        val data: ByteArray,
        val mime: String,
        val width: Int,
        val height: Int
    ) {
        fun toBase64(): String = Base64.encodeToString(data, Base64.NO_WRAP)
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ProcessedImage
            return data.contentEquals(other.data) && mime == other.mime
        }
        
        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + mime.hashCode()
            return result
        }
    }
    
    val DEFAULT_OPTIONS = ProcessorOptions()
    
    val ANTHROPIC_OPTIONS = ProcessorOptions(
        supportedMimeTypes = listOf("image/png", "image/jpeg", "image/gif", "image/webp"),
        preferredMimeType = "image/jpeg",
        maxSizeInMB = 5f,
        maxWidth = 1568,
        maxHeight = 1568
    )
    
    suspend fun processImage(
        file: MessageFile,
        options: ProcessorOptions = DEFAULT_OPTIONS
    ): MessageFile = withContext(Dispatchers.IO) {
        if (!file.isImage()) {
            return@withContext file
        }
        
        var originalBitmap: Bitmap? = null
        var resizedBitmap: Bitmap? = null
        
        try {
            val bytes = Base64.decode(file.value, Base64.DEFAULT)
            
            originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (originalBitmap == null) {
                Log.w(TAG, "Failed to decode bitmap")
                return@withContext file
            }
            
            val originalWidth = originalBitmap.width
            val originalHeight = originalBitmap.height
            val originalSizeInBytes = bytes.size
            
            val maxSizeBytes = (options.maxSizeInMB * 1024 * 1024).toInt()
            val tooLargeInSize = originalWidth > options.maxWidth || originalHeight > options.maxHeight
            val tooLargeInBytes = originalSizeInBytes > maxSizeBytes
            
            val outputMime = chooseMimeType(
                supportedMimes = options.supportedMimeTypes,
                preferredMime = options.preferredMimeType,
                currentMime = file.mime,
                preferSizeReduction = tooLargeInBytes
            )
            
            var targetWidth = originalWidth
            var targetHeight = originalHeight
            
            if (tooLargeInSize || tooLargeInBytes) {
                val (newWidth, newHeight) = chooseImageSize(
                    width = originalWidth,
                    height = originalHeight,
                    maxWidth = options.maxWidth,
                    maxHeight = options.maxHeight,
                    maxSizeInMB = options.maxSizeInMB,
                    mime = outputMime
                )
                targetWidth = newWidth
                targetHeight = newHeight
            }
            
            resizedBitmap = if (targetWidth != originalWidth || targetHeight != originalHeight) {
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            } else {
                originalBitmap
            }
            
            val outputStream = ByteArrayOutputStream()
            val compressFormat = when (outputMime) {
                "image/png" -> Bitmap.CompressFormat.PNG
                "image/webp" -> Bitmap.CompressFormat.WEBP_LOSSY
                else -> Bitmap.CompressFormat.JPEG
            }
            
            resizedBitmap.compress(compressFormat, options.quality, outputStream)
            val outputBytes = outputStream.toByteArray()
            val outputBase64 = Base64.encodeToString(outputBytes, Base64.NO_WRAP)
            
            MessageFile(
                type = MessageFile.FileDataType.BASE64,
                name = file.name,
                value = outputBase64,
                mime = outputMime
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image: ${e.message}", e)
            file
        } finally {
            try {
                if (resizedBitmap != null && resizedBitmap != originalBitmap) {
                    resizedBitmap.recycle()
                }
                originalBitmap?.recycle()
            } catch (e: Exception) {
                Log.w(TAG, "Error recycling bitmaps: ${e.message}")
            }
        }
    }
    
    suspend fun processImageFromUri(
        context: Context,
        uri: Uri,
        fileName: String,
        mimeType: String,
        options: ProcessorOptions = DEFAULT_OPTIONS
    ): MessageFile? = withContext(Dispatchers.IO) {
        var originalBitmap: Bitmap? = null
        var resizedBitmap: Bitmap? = null
        var inputStream: java.io.InputStream? = null
        
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream from Uri")
                return@withContext null
            }
            
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            
            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from Uri")
                return@withContext null
            }
            
            val originalWidth = originalBitmap.width
            val originalHeight = originalBitmap.height
            
            val tooLargeInSize = originalWidth > options.maxWidth || originalHeight > options.maxHeight
            
            val outputMime = chooseMimeType(
                supportedMimes = options.supportedMimeTypes,
                preferredMime = options.preferredMimeType,
                currentMime = mimeType,
                preferSizeReduction = true
            )
            
            var targetWidth = originalWidth
            var targetHeight = originalHeight
            
            if (tooLargeInSize) {
                val (newWidth, newHeight) = chooseImageSize(
                    width = originalWidth,
                    height = originalHeight,
                    maxWidth = options.maxWidth,
                    maxHeight = options.maxHeight,
                    maxSizeInMB = options.maxSizeInMB,
                    mime = outputMime
                )
                targetWidth = newWidth
                targetHeight = newHeight
            }
            
            resizedBitmap = if (targetWidth != originalWidth || targetHeight != originalHeight) {
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            } else {
                originalBitmap
            }
            
            val outputStream = ByteArrayOutputStream()
            val compressFormat = when (outputMime) {
                "image/png" -> Bitmap.CompressFormat.PNG
                "image/webp" -> Bitmap.CompressFormat.WEBP_LOSSY
                else -> Bitmap.CompressFormat.JPEG
            }
            
            resizedBitmap.compress(compressFormat, options.quality, outputStream)
            val outputBytes = outputStream.toByteArray()
            val outputBase64 = Base64.encodeToString(outputBytes, Base64.NO_WRAP)
            
            MessageFile(
                type = MessageFile.FileDataType.BASE64,
                name = fileName,
                value = outputBase64,
                mime = outputMime
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image from Uri: ${e.message}", e)
            null
        } finally {
            try {
                inputStream?.close()
                if (resizedBitmap != null && resizedBitmap != originalBitmap) {
                    resizedBitmap.recycle()
                }
                originalBitmap?.recycle()
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up resources: ${e.message}")
            }
        }
    }
    
    private fun chooseMimeType(
        supportedMimes: List<String>,
        preferredMime: String,
        currentMime: String,
        preferSizeReduction: Boolean
    ): String {
        if (currentMime in supportedMimes && !preferSizeReduction) {
            return currentMime
        }
        
        val mimesBySizeAsc = listOf(
            "image/webp",
            "image/jpeg",
            "image/png"
        )
        
        if (preferSizeReduction) {
            val smallestSupported = mimesBySizeAsc.firstOrNull { it in supportedMimes }
            if (smallestSupported != null) {
                return smallestSupported
            }
        }
        
        return preferredMime
    }
    
    private fun chooseImageSize(
        width: Int,
        height: Int,
        maxWidth: Int,
        maxHeight: Int,
        maxSizeInMB: Float,
        mime: String
    ): Pair<Int, Int> {
        val widthScale = maxWidth.toFloat() / width
        val heightScale = maxHeight.toFloat() / height
        var scale = min(1f, min(widthScale, heightScale))
        
        var targetWidth = (width * scale).toInt()
        var targetHeight = (height * scale).toInt()
        
        val maxSizeBytes = (maxSizeInMB * 1024 * 1024).toLong()
        var estimatedSize = estimateImageSize(mime, targetWidth, targetHeight)
        
        while (estimatedSize > maxSizeBytes && targetWidth > 100 && targetHeight > 100) {
            scale *= 0.9f
            targetWidth = (width * scale).toInt()
            targetHeight = (height * scale).toInt()
            estimatedSize = estimateImageSize(mime, targetWidth, targetHeight)
        }
        
        return Pair(max(1, targetWidth), max(1, targetHeight))
    }
    
    private fun estimateImageSize(mime: String, width: Int, height: Int): Long {
        val pixels = width.toLong() * height.toLong()
        val bytesPerPixel = 4L
        val uncompressedSize = pixels * bytesPerPixel
        
        val compressionRatio = when (mime) {
            "image/png" -> 0.5
            "image/jpeg" -> 0.1
            "image/webp" -> 0.15
            else -> 0.25
        }
        
        return (uncompressedSize * compressionRatio).toLong()
    }
}
