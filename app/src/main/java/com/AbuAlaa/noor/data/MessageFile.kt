package com.AbuAlaa.noor.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

data class MessageFile(
    val type: FileDataType,
    val name: String,
    val value: String,
    val mime: String
) {
    enum class FileDataType {
        HASH,
        BASE64
    }
    
    fun isImage(): Boolean = mime.startsWith("image/")
    
    fun isTextFile(): Boolean = TEXT_MIME_ALLOWLIST.any { allowed ->
        val (aType, aSubtype) = allowed.lowercase().split("/")
        val (fType, fSubtype) = mime.lowercase().split("/").let { 
            if (it.size >= 2) it[0] to it[1] else it[0] to "*"
        }
        (aType == "*" || aType == fType) && (aSubtype == "*" || aSubtype == fSubtype)
    }
    
    fun isPdf(): Boolean = mime.lowercase() == "application/pdf"
    
    fun getDataUrl(): String {
        return if (type == FileDataType.BASE64) {
            "data:$mime;base64,$value"
        } else {
            value
        }
    }
    
    fun getTextContent(): String? {
        return if (type == FileDataType.BASE64 && isTextFile()) {
            try {
                String(Base64.decode(value, Base64.DEFAULT), Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    companion object {
        private const val TAG = "MessageFile"
        
        private const val MAX_FILE_SIZE_BYTES = 8 * 1024 * 1024
        
        val IMAGE_MIME_ALLOWLIST = listOf(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/webp",
            "image/svg+xml"
        )
        
        val TEXT_MIME_ALLOWLIST = listOf(
            "text/plain",
            "text/markdown",
            "text/csv",
            "text/html",
            "text/css",
            "text/javascript",
            "text/xml",
            "application/json",
            "application/xml",
            "application/javascript",
            "application/typescript",
            "application/x-yaml",
            "application/x-sh",
            "application/x-python",
            "application/pdf",
            "text/x-python",
            "text/x-java",
            "text/x-kotlin",
            "text/x-c",
            "text/x-cpp",
            "text/x-csharp",
            "text/x-go",
            "text/x-rust",
            "text/x-swift"
        )
        
        val PDF_MIME_TYPES = listOf(
            "application/pdf"
        )
        
        suspend fun fromUri(
            context: Context,
            uri: Uri,
            fileName: String,
            mimeType: String
        ): MessageFile? = withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext null
                
                val bytes = inputStream.use { stream ->
                    val buffer = ByteArray(64 * 1024)
                    val output = ByteArrayOutputStream()
                    var totalRead = 0
                    
                    while (true) {
                        val read = stream.read(buffer)
                        if (read <= 0) break
                        
                        totalRead += read
                        if (totalRead > MAX_FILE_SIZE_BYTES) {
                            Log.e(TAG, "File too large: $totalRead bytes (max: $MAX_FILE_SIZE_BYTES)")
                            return@withContext null
                        }
                        
                        output.write(buffer, 0, read)
                    }
                    
                    output.toByteArray()
                }
                
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                
                MessageFile(
                    type = FileDataType.BASE64,
                    name = fileName,
                    value = base64,
                    mime = mimeType
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create MessageFile from Uri: ${e.message}", e)
                null
            }
        }
        
        suspend fun fromBitmap(
            bitmap: Bitmap,
            fileName: String = "camera_${System.currentTimeMillis()}.jpg"
        ): MessageFile = withContext(Dispatchers.IO) {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            
            MessageFile(
                type = FileDataType.BASE64,
                name = fileName,
                value = base64,
                mime = "image/jpeg"
            )
        }
        
        fun fromClipboardText(text: String): MessageFile {
            val base64 = Base64.encodeToString(
                text.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
            return MessageFile(
                type = FileDataType.BASE64,
                name = "clipboard_${System.currentTimeMillis()}.txt",
                value = base64,
                mime = "application/vnd.chatui.clipboard"
            )
        }
    }
}
