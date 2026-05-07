package com.AbuAlaa.noor.mcp

import android.util.Log
import com.AbuAlaa.noor.config.ConfigManager
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

class MCPClient(
    private val serverConfig: MCPServerConfig
) {
    private val TAG = "MCPClient"
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    private val requestId = AtomicInteger(1)
    private var initialized = false
    
    private fun getAuthHeaders(): Map<String, String> {
        val headers = serverConfig.headers.toMutableMap()
        
        if (!MCPUtils.hasAuthHeader(headers)) {
            try {
                val token = ConfigManager.get(ConfigManager.Keys.OPENAI_API_KEY, "")
                if (MCPUtils.hasNonEmptyToken(token)) {
                    headers["Authorization"] = "Bearer $token"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get token: ${e.message}", e)
            }
        }
        
        return headers
    }
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (initialized) return@withContext true
        
        try {
            val initRequest = buildJsonObject {
                put("jsonrpc", JsonPrimitive("2.0"))
                put("id", JsonPrimitive(requestId.getAndIncrement()))
                put("method", JsonPrimitive("initialize"))
                put("params", buildJsonObject {
                    put("protocolVersion", JsonPrimitive("2024-11-05"))
                    put("capabilities", buildJsonObject {
                        put("tools", buildJsonObject {})
                    })
                    put("clientInfo", buildJsonObject {
                        put("name", JsonPrimitive("Noor-Android"))
                        put("version", JsonPrimitive("1.0.0"))
                    })
                })
            }
            
            val response = sendJsonRpcRequest(initRequest)
            
            if (response != null && response.containsKey("result")) {
                Log.i(TAG, "MCP initialized for ${serverConfig.name}")
                
                val notifyRequest = buildJsonObject {
                    put("jsonrpc", JsonPrimitive("2.0"))
                    put("method", JsonPrimitive("notifications/initialized"))
                }
                sendJsonRpcRequest(notifyRequest, expectResponse = false)
                
                initialized = true
                return@withContext true
            }
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "Initialize error: ${e.message}", e)
            false
        }
    }
    
    suspend fun listTools(): List<MCPTool> = withContext(Dispatchers.IO) {
        if (!initialized && !initialize()) {
            return@withContext emptyList()
        }
        
        try {
            val request = buildJsonObject {
                put("jsonrpc", JsonPrimitive("2.0"))
                put("id", JsonPrimitive(requestId.getAndIncrement()))
                put("method", JsonPrimitive("tools/list"))
                put("params", buildJsonObject {})
            }
            
            val response = sendJsonRpcRequest(request)
            
            if (response != null) {
                val result = response["result"]?.jsonObject
                val toolsArray = result?.get("tools")?.jsonArray ?: return@withContext emptyList()
                
                return@withContext toolsArray.mapNotNull { toolJson ->
                    try {
                        MCPTool(
                            name = toolJson.jsonObject["name"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            description = toolJson.jsonObject["description"]?.jsonPrimitive?.contentOrNull,
                            inputSchema = toolJson.jsonObject["inputSchema"],
                            serverId = serverConfig.id
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "List tools error: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun callTool(name: String, arguments: Map<String, JsonElement>): MCPToolResult = withContext(Dispatchers.IO) {
        if (!initialized && !initialize()) {
            return@withContext MCPToolResult("Server not initialized", true, name)
        }
        
        try {
            val request = buildJsonObject {
                put("jsonrpc", JsonPrimitive("2.0"))
                put("id", JsonPrimitive(requestId.getAndIncrement()))
                put("method", JsonPrimitive("tools/call"))
                put("params", buildJsonObject {
                    put("name", JsonPrimitive(name))
                    put("arguments", buildJsonObject {
                        arguments.forEach { (key, value) ->
                            put(key, value)
                        }
                    })
                })
            }
            
            val response = sendJsonRpcRequest(request)
            
            if (response != null) {
                val error = response["error"]?.jsonObject
                if (error != null) {
                    val errorMsg = error["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                    return@withContext MCPToolResult(errorMsg, true, name)
                }
                
                val result = response["result"]?.jsonObject
                val content = result?.get("content")?.let { contentElement ->
                    when {
                        contentElement is JsonArray -> {
                            contentElement.joinToString("\n") { item ->
                                item.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: item.toString()
                            }
                        }
                        contentElement is JsonPrimitive -> contentElement.contentOrNull ?: ""
                        else -> contentElement.toString()
                    }
                } ?: "No content"
                
                val isError = result?.get("isError")?.jsonPrimitive?.booleanOrNull ?: false
                return@withContext MCPToolResult(content, isError, name)
            }
            
            MCPToolResult("No response from server", true, name)
        } catch (e: Exception) {
            Log.e(TAG, "Call tool error: ${e.message}", e)
            MCPToolResult("Tool call failed: ${e.message}", true, name)
        }
    }
    
    private suspend fun sendJsonRpcRequest(
        request: JsonObject,
        expectResponse: Boolean = true
    ): JsonObject? = withContext(Dispatchers.IO) {
        val headers = getAuthHeaders()
        
        try {
            val result = sendHttpPost(request, headers)
            if (result != null) return@withContext result
        } catch (e: Exception) {
            Log.d(TAG, "HTTP POST failed: ${e.message}")
        }
        
        null
    }
    
    private fun sendHttpPost(request: JsonObject, headers: Map<String, String>): JsonObject? {
        val url = URL(serverConfig.url)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.doInput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json, text/event-stream")
            
            headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(request.toString())
                writer.flush()
            }
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                val contentType = connection.contentType ?: ""
                
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    val response = StringBuilder()
                    
                    if (contentType.contains("text/event-stream")) {
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            if (line!!.startsWith("data:")) {
                                val data = line!!.substring(5).trim()
                                if (data.isNotEmpty() && data != "[DONE]") {
                                    return try {
                                        json.parseToJsonElement(data).jsonObject
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }
                        }
                    } else {
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        
                        if (response.isNotEmpty()) {
                            return try {
                                json.parseToJsonElement(response.toString()).jsonObject
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        
        return null
    }
    
    fun close() {
        initialized = false
    }
}
