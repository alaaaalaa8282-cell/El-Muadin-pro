package com.AbuAlaa.noor.utils

import android.util.Log
import com.AbuAlaa.noor.data.MessageFile
import kotlinx.serialization.json.*

object MessagePreparer {
    private const val TAG = "MessagePreparer"
    
    data class ChatMessage(
        val role: String,
        val content: String,
        val files: List<MessageFile> = emptyList()
    )
    
    suspend fun prepareMessagesWithFiles(
        messages: List<ChatMessage>,
        isMultimodal: Boolean,
        imageProcessor: ImageProcessor.ProcessorOptions = ImageProcessor.DEFAULT_OPTIONS
    ): JsonArray {
        return buildJsonArray {
            for (message in messages) {
                val preparedMessage = prepareMessage(message, isMultimodal, imageProcessor)
                add(preparedMessage)
            }
        }
    }
    
    private suspend fun prepareMessage(
        message: ChatMessage,
        isMultimodal: Boolean,
        imageProcessor: ImageProcessor.ProcessorOptions
    ): JsonObject {
        if (message.files.isEmpty()) {
            return buildJsonObject {
                put("role", message.role)
                put("content", message.content)
            }
        }
        
        if (message.role != "user") {
            return buildJsonObject {
                put("role", message.role)
                put("content", message.content)
            }
        }
        
        val imageFiles = message.files.filter { it.isImage() }
        val pdfFiles = message.files.filter { it.isPdf() }
        val textFiles = message.files.filter { it.isTextFile() && !it.isPdf() }
        
        val imageParts = if (isMultimodal && imageFiles.isNotEmpty()) {
            imageFiles.mapNotNull { file ->
                try {
                    val processedFile = ImageProcessor.processImage(file, imageProcessor)
                    buildJsonObject {
                        put("type", "image_url")
                        putJsonObject("image_url") {
                            put("url", "data:${processedFile.mime};base64,${processedFile.value}")
                            put("detail", "auto")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process image: ${e.message}", e)
                    null
                }
            }
        } else {
            emptyList()
        }
        
        val textContent = if (textFiles.isNotEmpty()) {
            textFiles.mapNotNull { file ->
                file.getTextContent()?.let { content ->
                    """<document name="${file.name}" type="${file.mime}">
$content
</document>"""
                }
            }.joinToString("\n\n")
        } else {
            ""
        }
        
        val pdfParts = if (isMultimodal && pdfFiles.isNotEmpty()) {
            pdfFiles.map { file ->
                buildJsonObject {
                    put("type", "file")
                    putJsonObject("file") {
                        put("filename", file.name)
                        put("file_data", "data:${file.mime};base64,${file.value}")
                    }
                }
            }
        } else {
            emptyList()
        }
        
        val pdfNotice = if (!isMultimodal && pdfFiles.isNotEmpty()) {
            pdfFiles.joinToString("\n") { file ->
                "[PDF file attached: ${file.name} - This model may not be able to read PDF content directly. Please use a vision-enabled model.]"
            }
        } else {
            ""
        }
        
        val messageText = buildString {
            if (textContent.isNotEmpty()) {
                append(textContent)
                append("\n\n")
            }
            if (pdfNotice.isNotEmpty()) {
                append(pdfNotice)
                append("\n\n")
            }
            append(message.content)
        }
        
        val hasMultimodalContent = (imageParts.isNotEmpty() || pdfParts.isNotEmpty()) && isMultimodal
        
        return if (hasMultimodalContent) {
            buildJsonObject {
                put("role", message.role)
                putJsonArray("content") {
                    add(buildJsonObject {
                        put("type", "text")
                        put("text", messageText)
                    })
                    imageParts.forEach { add(it) }
                    pdfParts.forEach { add(it) }
                }
            }
        } else {
            buildJsonObject {
                put("role", message.role)
                put("content", messageText)
            }
        }
    }
    
    fun fromLegacyMessages(
        messages: List<com.AbuAlaa.noor.api.ChatApiClient.ChatMessage>,
        filesMap: Map<String, List<MessageFile>> = emptyMap()
    ): List<ChatMessage> {
        return messages.mapIndexed { index, msg ->
            ChatMessage(
                role = msg.role,
                content = msg.content,
                files = filesMap[index.toString()] ?: emptyList()
            )
        }
    }
    
    fun hasFiles(messages: List<ChatMessage>): Boolean {
        return messages.any { it.files.isNotEmpty() }
    }
    
    fun hasImages(messages: List<ChatMessage>): Boolean {
        return messages.any { msg -> msg.files.any { it.isImage() } }
    }
    
    fun hasPdfs(messages: List<ChatMessage>): Boolean {
        return messages.any { msg -> msg.files.any { it.isPdf() } }
    }
    
    fun hasMultimodalContent(messages: List<ChatMessage>): Boolean {
        return hasImages(messages) || hasPdfs(messages)
    }
    
    fun getTotalFileCount(messages: List<ChatMessage>): Int {
        return messages.sumOf { it.files.size }
    }
    
    suspend fun prepareMessagesForGemini(
        messages: List<ChatMessage>,
        isMultimodal: Boolean,
        imageProcessor: ImageProcessor.ProcessorOptions = ImageProcessor.DEFAULT_OPTIONS
    ): JsonArray {
        return buildJsonArray {
            for (message in messages) {
                val geminiRole = if (message.role == "assistant") "model" else message.role
                
                val imageFiles = message.files.filter { it.isImage() }
                val textFiles = message.files.filter { it.isTextFile() && !it.isPdf() }
                
                val textContent = if (textFiles.isNotEmpty()) {
                    textFiles.mapNotNull { file ->
                        file.getTextContent()?.let { content ->
                            """<document name="${file.name}" type="${file.mime}">
$content
</document>"""
                        }
                    }.joinToString("\n\n")
                } else ""
                
                val messageText = buildString {
                    if (textContent.isNotEmpty()) {
                        append(textContent)
                        append("\n\n")
                    }
                    append(message.content)
                }
                
                add(buildJsonObject {
                    put("role", geminiRole)
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", messageText)
                        })
                        
                        if (isMultimodal && imageFiles.isNotEmpty()) {
                            imageFiles.forEach { file ->
                                try {
                                    val processedFile = kotlinx.coroutines.runBlocking {
                                        ImageProcessor.processImage(file, imageProcessor)
                                    }
                                    add(buildJsonObject {
                                        putJsonObject("inline_data") {
                                            put("mime_type", processedFile.mime)
                                            put("data", processedFile.value)
                                        }
                                    })
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to process image for Gemini: ${e.message}", e)
                                }
                            }
                        }
                    }
                })
            }
        }
    }
}
