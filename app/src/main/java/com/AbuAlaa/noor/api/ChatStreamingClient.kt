package com.AbuAlaa.noor.api

import android.content.Context
import android.util.Log
import com.AbuAlaa.noor.config.ConfigManager
import com.AbuAlaa.noor.data.ApiProvider
import com.AbuAlaa.noor.data.AuthMethod
import com.AbuAlaa.noor.data.MessageFile
import com.AbuAlaa.noor.utils.ImageProcessor
import com.AbuAlaa.noor.utils.MessagePreparer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
object ChatStreamingClient {
    private const val TAG = "ChatStreamingClient"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    @Volatile
    private var currentEventSource: EventSource? = null
    private val streamLock = Any()
    
    fun cancelCurrentStream() {
        synchronized(streamLock) {
            Log.d(TAG, "Cancelling current stream")
            currentEventSource?.cancel()
            currentEventSource = null
        }
    }
    
    private fun setCurrentEventSource(eventSource: EventSource?) {
        synchronized(streamLock) {
            currentEventSource?.cancel()
            currentEventSource = eventSource
        }
    }
    
    fun chatCompletionStream(
        messages: List<ChatApiClient.ChatMessage>,
        model: String
    ): Flow<StreamEvent> = chatCompletionStreamWithFiles(
        messages = messages.map { MessagePreparer.ChatMessage(it.role, it.content) },
        model = model,
        isMultimodal = false
    )
    
    fun chatCompletionStreamWithFiles(
        messages: List<MessagePreparer.ChatMessage>,
        model: String,
        isMultimodal: Boolean = false,
        tools: JsonArray? = null,
        imageProcessorOptions: ImageProcessor.ProcessorOptions = ImageProcessor.DEFAULT_OPTIONS
    ): Flow<StreamEvent> = callbackFlow {
        val providerConfig = ConfigManager.getProviderConfig()
        
        if (!providerConfig.isValid()) {
            trySend(StreamEvent.Error("Invalid API configuration. Please check your settings."))
            close()
            return@callbackFlow
        }
        
        Log.i(TAG, "Using provider: ${providerConfig.provider.displayName}")
        
        if (providerConfig.provider == ApiProvider.GOOGLE_AI_STUDIO) {
            Log.i(TAG, "Using Google AI Studio Direct API")
            
            val apiKey = providerConfig.apiKey
            if (apiKey.isBlank()) {
                trySend(StreamEvent.Error("Google AI Studio API Key is required"))
                close()
                return@callbackFlow
            }
            
            val preparedMessages = async(Dispatchers.IO) {
                MessagePreparer.prepareMessagesForGemini(messages, isMultimodal, imageProcessorOptions)
            }.await()
            
            val modelId = if (model.startsWith("google/")) {
                model.substringAfter("google/")
            } else {
                model
            }
            
            val url = "${providerConfig.baseUrl}/models/$modelId:streamGenerateContent?key=$apiKey"
            
            val requestBody = buildJsonObject {
                put("contents", preparedMessages)
                put("generationConfig", buildJsonObject {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 4096)
                })
            }.toString()
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    Log.e(TAG, "Google AI Studio request failed: ${e.message}", e)
                    trySend(StreamEvent.Error("Network error: ${e.message}"))
                    close()
                }
                
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        trySend(StreamEvent.Error("Google AI Studio Error: HTTP ${response.code} | Body: $errorBody"))
                        response.close()
                        close()
                        return
                    }
                    
                    val source = response.body?.source()
                    if (source == null) {
                        trySend(StreamEvent.Error("Empty response body"))
                        response.close()
                        close()
                        return
                    }
                    
                    try {
                        val reader = source.inputStream().bufferedReader(Charsets.UTF_8)
                        val buffer = StringBuilder()
                        var braceDepth = 0
                        var inString = false
                        var escapeNext = false
                        
                        reader.use { r ->
                            while (true) {
                                val charInt = r.read()
                                if (charInt == -1) break
                                val char = charInt.toChar()
                                
                                when {
                                    escapeNext -> {
                                        escapeNext = false
                                        buffer.append(char)
                                    }
                                    char == '\\' && inString -> {
                                        escapeNext = true
                                        buffer.append(char)
                                    }
                                    char == '"' && !escapeNext -> {
                                        inString = !inString
                                        buffer.append(char)
                                    }
                                    char == '{' && !inString -> {
                                        braceDepth++
                                        buffer.append(char)
                                    }
                                    char == '}' && !inString -> {
                                        braceDepth--
                                        buffer.append(char)
                                        
                                        if (braceDepth == 0) {
                                            val jsonStr = buffer.toString().trim()
                                            buffer.clear()
                                            
                                            try {
                                                val jsonData = json.parseToJsonElement(jsonStr).jsonObject
                                                val candidates = jsonData["candidates"]?.jsonArray
                                                
                                                if (candidates != null && candidates.isNotEmpty()) {
                                                    val candidate = candidates[0].jsonObject
                                                    val content = candidate["content"]?.jsonObject
                                                    val parts = content?.get("parts")?.jsonArray
                                                    
                                                    if (parts != null && parts.isNotEmpty()) {
                                                        val text = parts[0].jsonObject["text"]?.jsonPrimitive?.content
                                                        if (!text.isNullOrEmpty()) {
                                                            trySend(StreamEvent.Token(text))
                                                        }
                                                    }
                                                    
                                                    val finishReason = candidate["finishReason"]?.jsonPrimitive?.content
                                                    if (finishReason == "STOP") {
                                                        trySend(StreamEvent.Complete(""))
                                                        response.close()
                                                        close()
                                                        return@onResponse
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error parsing chunk: ${e.message}")
                                            }
                                        }
                                    }
                                    braceDepth == 0 && (char == ',' || char == '[' || char == ']' || char.isWhitespace()) -> {
                                    }
                                    else -> {
                                        if (braceDepth > 0) {
                                            buffer.append(char)
                                        }
                                    }
                                }
                            }
                        }
                        
                        trySend(StreamEvent.Complete(""))
                        response.close()
                        close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Stream reading error: ${e.message}", e)
                        trySend(StreamEvent.Error("Stream error: ${e.message}"))
                        response.close()
                        close()
                    }
                }
            })
            
            awaitClose {
                Log.d(TAG, "Closing Google AI Studio stream")
            }
            return@callbackFlow
        }
        
        val accessToken = providerConfig.apiKey
        
        val preparedMessages = async(Dispatchers.IO) {
            MessagePreparer.prepareMessagesWithFiles(messages, isMultimodal, imageProcessorOptions)
        }.await()
        
        val messagesWithSystem = if (tools != null && tools.isNotEmpty()) {
            buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", buildToolsSystemPrompt(tools))
                })
                preparedMessages.forEach { element -> add(element) }
            }
        } else {
            preparedMessages
        }
        
        val modelId = if (model.startsWith("google/")) {
            model.substringAfter("google/")
        } else {
            model
        }
        
        val requestBody = buildJsonObject {
            put("model", modelId)
            put("messages", messagesWithSystem)
            put("stream", true)
            put("temperature", 0.7)
            put("max_tokens", 4096)
            
            if (tools != null && tools.isNotEmpty()) {
                Log.i(TAG, "Using prompt-based tool calling with ${tools.size} tools")
            }
        }.toString()
        
        val requestBuilder = Request.Builder()
            .url(providerConfig.getChatCompletionsUrl())
            .post(requestBody.toRequestBody("application/json".toMediaType()))
        
        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        
        providerConfig.customHeaders.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        val request = requestBuilder.build()
        
        Log.d(TAG, "Request URL: ${request.url}")
        Log.d(TAG, "Request Headers: ${request.headers}")
        
        var fullText = ""
        var eventSource: EventSource?
        
        try {
            eventSource = EventSources.createFactory(client)
                .newEventSource(request, object : EventSourceListener() {
                    override fun onOpen(eventSource: EventSource, response: Response) {
                        Log.d(TAG, "Stream opened")
                        trySend(StreamEvent.Status("Connected", false))
                    }
                    
                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        if (data == "[DONE]") {
                            trySend(StreamEvent.Complete(fullText, false))
                            close()
                            return
                        }
                        
                        try {
                            val jsonElement = json.parseToJsonElement(data)
                            val jsonObject = jsonElement.jsonObject
                            
                            val routerMetadata = jsonObject["x-router-metadata"]?.jsonObject
                            if (routerMetadata != null) {
                                val route = routerMetadata["route"]?.jsonPrimitive?.content ?: ""
                                val routeModel = routerMetadata["model"]?.jsonPrimitive?.content ?: ""
                                val provider = routerMetadata["provider"]?.jsonPrimitive?.contentOrNull
                                
                                trySend(StreamEvent.RouterMetadata(route, routeModel, provider))
                            }
                            
                            val choices = jsonObject["choices"]?.jsonArray
                            if (choices != null && choices.isNotEmpty()) {
                                val choice = choices[0].jsonObject
                                val delta = choice["delta"]?.jsonObject
                                val content = delta?.get("content")?.jsonPrimitive?.contentOrNull
                                
                                if (content != null && content.isNotEmpty()) {
                                    fullText += content
                                    trySend(StreamEvent.Token(content))
                                }
                                
                                val toolCalls = delta?.get("tool_calls")?.jsonArray
                                if (toolCalls != null && toolCalls.isNotEmpty()) {
                                    for (toolCall in toolCalls) {
                                        val tc = toolCall.jsonObject
                                        val tcIndex = tc["index"]?.jsonPrimitive?.intOrNull ?: 0
                                        val tcId = tc["id"]?.jsonPrimitive?.contentOrNull
                                        val function = tc["function"]?.jsonObject
                                        val tcName = function?.get("name")?.jsonPrimitive?.contentOrNull
                                        val tcArgs = function?.get("arguments")?.jsonPrimitive?.contentOrNull
                                        
                                        if (tcName != null || tcArgs != null) {
                                            trySend(StreamEvent.ToolCall(tcIndex, tcId, tcName, tcArgs))
                                        }
                                    }
                                }
                                
                                val finishReason = choice["finish_reason"]?.jsonPrimitive?.contentOrNull
                                if (finishReason == "stop" || finishReason == "length") {
                                    trySend(StreamEvent.Complete(fullText, false))
                                    close()
                                } else if (finishReason == "tool_calls") {
                                    trySend(StreamEvent.ToolCallsComplete(fullText))
                                    close()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse stream data: ${e.message}", e)
                            trySend(StreamEvent.Error("Parse error: ${e.message}"))
                        }
                    }
                    
                    override fun onClosed(eventSource: EventSource) {
                        Log.d(TAG, "Stream closed")
                        trySend(StreamEvent.Complete(fullText, false))
                        setCurrentEventSource(null)
                        channel.close()
                    }
                    
                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?
                    ) {
                        Log.e(TAG, "Stream failed: ${t?.message}", t)
                        val statusCode = response?.code
                        val responseBody = response?.body?.string()
                        
                        val errorMsg = buildString {
                            when {
                                statusCode == 401 -> append("⚠️ API Key غير صالح")
                                statusCode == 403 -> append("⚠️ غير مسموح بالوصول")
                                statusCode == 429 -> append("⚠️ تجاوزت الحد المسموح")
                                statusCode == 500 || statusCode == 502 || statusCode == 503 -> 
                                    append("⚠️ خطأ في الخادم")
                                t?.message?.contains("timeout", ignoreCase = true) == true ->
                                    append("⚠️ انتهت مهلة الاتصال")
                                t?.message?.contains("unable to resolve host", ignoreCase = true) == true ->
                                    append("⚠️ لا يمكن الوصول للخادم - تحقق من الاتصال بالإنترنت")
                                t?.message?.contains("SSL", ignoreCase = true) == true ->
                                    append("⚠️ خطأ في الاتصال الآمن (SSL)")
                                else -> append("⚠️ فشل الاتصال: ${t?.message ?: "خطأ غير معروف"}")
                            }
                            
                            if (statusCode != null) {
                                append(" (${statusCode})")
                            }
                            
                            if (!responseBody.isNullOrBlank() && responseBody.length < 500) {
                                append("\n\nتفاصيل: $responseBody")
                            }
                        }
                        
                        Log.e(TAG, "Error details - Status: $statusCode, Body: $responseBody")
                        trySend(StreamEvent.Error(errorMsg, statusCode))
                        setCurrentEventSource(null)
                        close(t)
                    }
                })
            setCurrentEventSource(eventSource)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create stream: ${e.message}", e)
            trySend(StreamEvent.Error(e.message ?: "Unknown error"))
            close(e)
        }
        
        awaitClose {
            Log.d(TAG, "Closing stream")
            cancelCurrentStream()
        }
    }
    
    private fun buildToolsSystemPrompt(tools: JsonArray): String {
        val toolDescriptions = tools.mapNotNull { toolElement ->
            try {
                val tool = toolElement.jsonObject
                val function = tool["function"]?.jsonObject ?: return@mapNotNull null
                val name = function["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val description = function["description"]?.jsonPrimitive?.contentOrNull ?: "No description"
                "- **$name**: $description"
            } catch (e: Exception) {
                null
            }
        }.joinToString("\n")
        
        return buildString {
            appendLine("# Available Tools")
            appendLine()
            appendLine("You have access to the following tools to help answer user questions:")
            appendLine()
            appendLine(toolDescriptions)
            appendLine()
            appendLine("## How to use tools:")
            appendLine("When you need to use a tool, respond with a tool call in this EXACT format:")
            appendLine()
            appendLine("<tool_call>")
            appendLine("""{"name": "tool_name", "arguments": {"param1": "value1"}}""")
            appendLine("</tool_call>")
            appendLine()
            appendLine("## IMPORTANT RULES:")
            appendLine("1. When asked about current events, news, sports scores, weather, or any real-time information - YOU MUST use the search tool")
            appendLine("2. Do NOT say you cannot access the internet - you CAN via the tools above")
            appendLine("3. Do NOT make up information - always search first for current data")
            appendLine("4. After receiving tool results, provide a helpful answer based on the results")
            appendLine()
            appendLine("Example: If asked about today's weather, respond with:")
            appendLine("<tool_call>")
            appendLine("""{"name": "web_search", "arguments": {"query": "weather today"}}""")
            appendLine("</tool_call>")
        }
    }
}
