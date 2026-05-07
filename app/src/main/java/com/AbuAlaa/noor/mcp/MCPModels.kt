package com.AbuAlaa.noor.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@Serializable
data class MCPServerConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val type: MCPTransportType = MCPTransportType.SSE,
    val headers: Map<String, String> = emptyMap()
)

@Serializable
enum class MCPTransportType {
    SSE,
    WEBSOCKET,
    STDIO
}

@Serializable
data class MCPTool(
    val name: String,
    val description: String? = null,
    val inputSchema: JsonElement? = null,
    val serverId: String = ""
)

@Serializable
data class MCPToolCall(
    val toolName: String,
    val arguments: Map<String, JsonElement> = emptyMap(),
    val serverId: String
)

@Serializable
data class MCPToolResult(
    val content: String,
    val isError: Boolean = false,
    val toolName: String = ""
)

enum class MCPConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STANDBY,
    ERROR,
}

data class MCPServerStatus(
    val serverId: String,
    val serverName: String,
    val state: MCPConnectionState,
    val error: String? = null,
    val toolCount: Int = 0
)

fun sanitizeToolName(name: String): String {
    return name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(64)
}

data class MCPToolMapping(
    val fnName: String,
    val serverName: String,
    val originalName: String,
    val serverId: String
)

fun List<MCPTool>.toLLMToolsWithMapping(): Pair<JsonArray, Map<String, MCPToolMapping>> {
    val tools = mutableListOf<JsonElement>()
    val mapping = mutableMapOf<String, MCPToolMapping>()
    val seenNames = mutableSetOf<String>()
    
    for (tool in this) {
        var sanitizedName = sanitizeToolName(tool.name)
        
        if (sanitizedName in seenNames) {
            val serverSuffix = sanitizeToolName(tool.serverId).take(20)
            var candidate = "${sanitizedName}_$serverSuffix".take(64)
            
            if (candidate in seenNames) {
                var i = 2
                while ("${candidate}_$i".take(64) in seenNames && i < 10) {
                    i++
                }
                candidate = "${candidate}_$i".take(64)
            }
            sanitizedName = candidate
        }
        
        seenNames.add(sanitizedName)
        
        val parameters = try {
            tool.inputSchema?.let { schema ->
                kotlinx.serialization.json.Json.parseToJsonElement(schema.toString())
            } ?: JsonObject(emptyMap())
        } catch (e: Exception) {
            JsonObject(mapOf(
                "type" to JsonPrimitive("object"),
                "properties" to JsonObject(emptyMap())
            ))
        }
        
        val toolJson = buildJsonObject {
            put("type", JsonPrimitive("function"))
            put("function", buildJsonObject {
                put("name", JsonPrimitive(sanitizedName))
                put("description", JsonPrimitive(tool.description ?: "No description"))
                put("parameters", parameters)
            })
        }
        tools.add(toolJson)
        
        mapping[sanitizedName] = MCPToolMapping(
            fnName = sanitizedName,
            serverName = tool.serverId,
            originalName = tool.name,
            serverId = tool.serverId
        )
    }
    
    return Pair(JsonArray(tools), mapping)
}

fun List<MCPTool>.toLLMTools(): JsonArray {
    return toLLMToolsWithMapping().first
}
