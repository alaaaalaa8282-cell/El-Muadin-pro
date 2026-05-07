package com.AbuAlaa.noor.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.AbuAlaa.noor.api.ChatApiClient
import com.AbuAlaa.noor.api.ChatStreamingClient
import com.AbuAlaa.noor.api.LlmRouter
import com.AbuAlaa.noor.api.ModelsApiClient
import com.AbuAlaa.noor.api.StreamEvent
import com.AbuAlaa.noor.config.ConfigManager
import com.AbuAlaa.noor.data.ApiProvider
import com.AbuAlaa.noor.data.Message
import com.AbuAlaa.noor.data.Conversation
import com.AbuAlaa.noor.data.MessageFile
import com.AbuAlaa.noor.data.db.ConversationRepository
import com.AbuAlaa.noor.utils.ImageProcessor
import com.AbuAlaa.noor.utils.MessagePreparer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class NoorViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "NoorViewModel"
    private val conversationRepository = ConversationRepository(application.applicationContext)

    // UI State
    var conversations by mutableStateOf<List<Conversation>>(emptyList())
        private set

    var currentConversation by mutableStateOf<Conversation?>(null)
        private set

    var messages by mutableStateOf<List<Message>>(emptyList())
        private set

    var selectedModelId by mutableStateOf("omni")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    // Available models
    var availableModels by mutableStateOf<List<ModelsApiClient.FetchedModel>>(emptyList())
        private set

    var isLoadingModels by mutableStateOf(false)
        private set

    // Files for multimodal
    var pendingFiles by mutableStateOf<List<MessageFile>>(emptyList())
        private set

    var isUploadingFile by mutableStateOf(false)
        private set

    // Streaming job
    private var streamingJob: Job? = null

    init {
        selectedModelId = ConfigManager.get(ConfigManager.Keys.PUBLIC_LLM_ROUTER_ALIAS_ID, "omni")
        fetchModels()
        loadConversations()
    }
    
    private fun loadConversations() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val loadedConversations = conversationRepository.getAllConversationsSync()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    conversations = loadedConversations
                    Log.d(TAG, "Loaded ${loadedConversations.size} conversations from database")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load conversations: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
    }

    fun fetchModels() {
        viewModelScope.launch {
            isLoadingModels = true
            try {
                val models = ModelsApiClient.getAllModels()
                availableModels = models
                selectedModelId = ConfigManager.get(ConfigManager.Keys.PUBLIC_LLM_ROUTER_ALIAS_ID, "omni")
            } catch (e: Exception) {
                error = "فشل في جلب النماذج: ${e.message}"
            } finally {
                isLoadingModels = false
            }
        }
    }

    private fun isOmniRouter(): Boolean {
        val aliasId = ConfigManager.get(ConfigManager.Keys.PUBLIC_LLM_ROUTER_ALIAS_ID, "omni")
        return selectedModelId == aliasId || selectedModelId.isBlank()
    }

    fun selectConversation(conversation: Conversation) {
        currentConversation = conversation
        messages = conversation.messages
    }

    fun newChat() {
        currentConversation = null
        messages = emptyList()
        error = null
        pendingFiles = emptyList()
    }

    fun deleteConversation(conversation: Conversation) {
        conversations = conversations.filter { it.id != conversation.id }
        if (currentConversation?.id == conversation.id) {
            newChat()
        }
        
        // Delete from database
        viewModelScope.launch {
            try {
                conversationRepository.deleteConversation(conversation.id)
                Log.d(TAG, "Conversation deleted from DB: ${conversation.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete conversation: ${e.message}", e)
            }
        }
    }

    fun selectModel(modelId: String) {
        Log.d(TAG, "Selecting model: $modelId")
        selectedModelId = modelId
        ConfigManager.set(ConfigManager.Keys.PUBLIC_LLM_ROUTER_ALIAS_ID, modelId)
    }

    fun sendMessage(messageText: String) {
        if (messageText.isBlank() && pendingFiles.isEmpty()) return

        val messageFiles = pendingFiles.toList()

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            content = messageText,
            isUser = true,
            timestamp = System.currentTimeMillis(),
            files = messageFiles
        )
        messages = messages + userMessage
        pendingFiles = emptyList()

        if (currentConversation == null) {
            val newId = UUID.randomUUID().toString()
            val generatedTitle = conversationRepository.generateTitle(messageText)
            val newConversation = Conversation(
                id = newId,
                title = generatedTitle,
                messages = messages,
                timestamp = System.currentTimeMillis(),
                model = selectedModelId
            )
            currentConversation = newConversation
            conversations = listOf(newConversation) + conversations
            
            // Save to database in background (IO dispatcher)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    conversationRepository.saveConversation(newConversation)
                    Log.d(TAG, "Conversation saved: $generatedTitle")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save conversation: ${e.message}", e)
                }
            }
        }

        val assistantMessageId = UUID.randomUUID().toString()
        var assistantMessage = Message(
            id = assistantMessageId,
            content = "",
            isUser = false,
            timestamp = System.currentTimeMillis(),
            model = selectedModelId
        )
        messages = messages + assistantMessage

        val providerConfig = ConfigManager.getProviderConfig()
        if (!providerConfig.isValid()) {
            addSimulatedResponse()
            return
        }

        isLoading = true
        error = null

        streamingJob = viewModelScope.launch {
            try {
                val apiMessages = messages.dropLast(1).map { msg ->
                    MessagePreparer.ChatMessage(
                        role = if (msg.isUser) "user" else "assistant",
                        content = msg.content,
                        files = msg.files
                    )
                }

                val hasMultimodalContent = MessagePreparer.hasMultimodalContent(apiMessages)
                val providerIsGoogleDirect = providerConfig.provider == ApiProvider.GOOGLE_AI_STUDIO

                val modelToUse = if (isOmniRouter() && !providerIsGoogleDirect) {
                    val legacyMessages = apiMessages.map {
                        ChatApiClient.ChatMessage(it.role, it.content)
                    }
                    LlmRouter.selectModel(legacyMessages, hasMultimodalContent, false)
                } else {
                    selectedModelId
                }

                Log.i(TAG, "Starting stream with model: $modelToUse")

                ChatStreamingClient.chatCompletionStreamWithFiles(
                    messages = apiMessages,
                    model = modelToUse,
                    isMultimodal = hasMultimodalContent
                ).collect { event ->
                    when (event) {
                        is StreamEvent.Token -> {
                            assistantMessage = assistantMessage.copy(
                                content = assistantMessage.content + event.text
                            )
                            messages = messages.dropLast(1) + assistantMessage
                        }
                        is StreamEvent.Complete -> {
                            isLoading = false
                            updateCurrentConversation()
                            saveCurrentConversationToDB() // حفظ فقط عند الانتهاء
                        }
                        is StreamEvent.Error -> {
                            Log.e(TAG, "Stream error: ${event.error}")
                            error = event.error
                            isLoading = false
                            val errorMessage = Message(
                                id = assistantMessageId,
                                content = "⚠️ خطأ: ${event.error}",
                                isUser = false,
                                timestamp = System.currentTimeMillis()
                            )
                            messages = messages.dropLast(1) + errorMessage
                            updateCurrentConversation()
                            saveCurrentConversationToDB() // حفظ حتى عند الخطأ
                        }
                        is StreamEvent.RouterMetadata -> {}
                        is StreamEvent.Status -> {}
                        is StreamEvent.KeepAlive -> {}
                        is StreamEvent.ToolCall -> {}
                        is StreamEvent.ToolCallsComplete -> {
                            isLoading = false
                            updateCurrentConversation()
                            saveCurrentConversationToDB() // حفظ عند انتهاء Tool Calls
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Streaming error: ${e.message}", e)
                error = e.message
                isLoading = false
                val errorMessage = Message(
                    id = assistantMessageId,
                    content = "⚠️ خطأ: ${e.message}",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                messages = messages.dropLast(1) + errorMessage
                updateCurrentConversation()
                saveCurrentConversationToDB() // حفظ عند exception
            }
        }
    }

    fun stopGeneration() {
        ChatStreamingClient.cancelCurrentStream()
        streamingJob?.cancel()
        streamingJob = null
        isLoading = false
        updateCurrentConversation()
        saveCurrentConversationToDB() // حفظ عند الإيقاف اليدوي
    }

    fun regenerateLastMessage() {
        try {
            // Stop any ongoing generation first
            if (isLoading) {
                stopGeneration()
                return
            }
            
            val lastUserMessage = messages.lastOrNull { it.isUser }
            if (lastUserMessage == null) {
                Log.w(TAG, "No user message found to regenerate")
                return
            }
            
            // Remove last AI response if exists
            val lastMessage = messages.lastOrNull()
            if (lastMessage != null && !lastMessage.isUser) {
                messages = messages.dropLast(1)
                updateCurrentConversation()
            }
            
            // Send the last user message again
            sendMessage(lastUserMessage.content)
        } catch (e: Exception) {
            Log.e(TAG, "Error regenerating message: ${e.message}", e)
            error = "فشل إعادة التوليد: ${e.message}"
        }
    }

    private fun addSimulatedResponse() {
        viewModelScope.launch {
            isLoading = true
            kotlinx.coroutines.delay(1000)

            val aiResponse = Message(
                id = UUID.randomUUID().toString(),
                content = buildString {
                    appendLine("👋 مرحباً! أنا **نور** - مساعدك الذكي.")
                    appendLine()
                    appendLine("⚠️ **لم يتم تكوين API**")
                    appendLine()
                    appendLine("لتفعيل الردود الحقيقية:")
                    appendLine("1. اذهب إلى **الإعدادات** ⚙️")
                    appendLine("2. اختر **إعدادات نور**")
                    appendLine("3. أدخل **HuggingFace Token** أو **Google AI Studio API Key**")
                    appendLine()
                    appendLine("المزودين المدعومين:")
                    appendLine("- **HuggingFace** (100+ نموذج)")
                    appendLine("- **Google AI Studio** (Gemini)")
                },
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            messages = messages + aiResponse
            updateCurrentConversation()
            saveCurrentConversationToDB() // حفظ بعد الرد المحاكي
            isLoading = false
        }
    }

    private fun updateCurrentConversation() {
        currentConversation = currentConversation?.copy(messages = messages)
        currentConversation?.let { conv ->
            conversations = conversations.map { if (it.id == conv.id) conv else it }
        }
    }
    
    private fun saveCurrentConversationToDB() {
        currentConversation?.let { conv ->
            // Save to database in background (IO dispatcher)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    conversationRepository.updateConversation(conv)
                    Log.d(TAG, "Conversation saved to DB: ${conv.title}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save conversation: ${e.message}", e)
                }
            }
        }
    }

    fun addPendingFile(file: MessageFile) {
        pendingFiles = pendingFiles + file
    }

    fun removePendingFile(file: MessageFile) {
        pendingFiles = pendingFiles.filter { it.name != file.name }
    }

    fun addImageFromUri(context: Context, uri: Uri, fileName: String, mimeType: String) {
        viewModelScope.launch {
            isUploadingFile = true
            try {
                val processedFile = ImageProcessor.processImageFromUri(
                    context = context,
                    uri = uri,
                    fileName = fileName,
                    mimeType = mimeType
                )

                if (processedFile != null) {
                    pendingFiles = pendingFiles + processedFile
                    Log.i(TAG, "Image added: ${processedFile.name}")
                } else {
                    error = "فشل في معالجة الصورة"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add image: ${e.message}", e)
                error = "فشل في إضافة الصورة: ${e.message}"
            } finally {
                isUploadingFile = false
            }
        }
    }

    fun addTextFileFromUri(context: Context, uri: Uri, fileName: String, mimeType: String) {
        viewModelScope.launch {
            isUploadingFile = true
            try {
                val file = MessageFile.fromUri(context, uri, fileName, mimeType)
                if (file != null) {
                    pendingFiles = pendingFiles + file
                    Log.i(TAG, "File added: ${file.name}")
                } else {
                    error = "فشل في قراءة الملف"
                }
            } catch (e: Exception) {
                error = "فشل في إضافة الملف: ${e.message}"
            } finally {
                isUploadingFile = false
            }
        }
    }
}
