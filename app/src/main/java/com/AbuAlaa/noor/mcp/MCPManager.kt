package com.AbuAlaa.noor.mcp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

object MCPManager {
    private const val TAG = "MCPManager"
    private const val PREFS_NAME = "noor_mcp_servers"
    private const val SERVERS_KEY = "servers_json"
    
    const val MAX_TOOLS = 100
    
    private val defaultServers = listOf(
        MCPServerConfig(
            id = "exa-web-search",
            name = "Web Search (Exa)",
            url = "https://mcp.exa.ai/mcp",
            type = MCPTransportType.SSE,
            enabled = false,
            headers = emptyMap()
        )
    )
    
    private val clients = ConcurrentHashMap<String, MCPClient>()
    private val clientsMutex = Mutex()
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
        encodeDefaults = true
    }

    private val _servers = MutableStateFlow<List<MCPServerConfig>>(emptyList())
    val servers: StateFlow<List<MCPServerConfig>> = _servers.asStateFlow()

    private val _serverStatuses = MutableStateFlow<Map<String, MCPServerStatus>>(emptyMap())
    val serverStatuses: StateFlow<Map<String, MCPServerStatus>> = _serverStatuses.asStateFlow()

    private val _tools = MutableStateFlow<List<MCPTool>>(emptyList())
    val tools: StateFlow<List<MCPTool>> = _tools.asStateFlow()
    
    private val _toolMapping = MutableStateFlow<Map<String, MCPToolMapping>>(emptyMap())
    val toolMapping: StateFlow<Map<String, MCPToolMapping>> = _toolMapping.asStateFlow()

    private var scope: CoroutineScope? = null
    
    private fun getScope(): CoroutineScope {
        if (scope == null || !scope!!.isActive) {
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        }
        return scope!!
    }

    fun init(context: Context) {
        loadServers(context)
        Log.i(TAG, "MCPManager initialized")
    }

    private fun loadServers(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val serversJson = prefs.getString(SERVERS_KEY, null)
            
            val loadedServers = if (serversJson != null) {
                json.decodeFromString<List<MCPServerConfig>>(serversJson)
            } else {
                emptyList()
            }
            
            val existingIds = loadedServers.map { it.id }
            val missingDefaults = defaultServers.filter { it.id !in existingIds }
            _servers.value = loadedServers + missingDefaults
            
            if (missingDefaults.isNotEmpty()) {
                saveServers(context)
            }
            
            Log.i(TAG, "Loaded ${_servers.value.size} MCP servers")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load servers: ${e.message}", e)
            _servers.value = defaultServers
        }
    }

    private fun saveServers(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val serversJson = json.encodeToString(_servers.value)
            prefs.edit().putString(SERVERS_KEY, serversJson).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save servers: ${e.message}", e)
        }
    }

    fun toggleServerEnabled(context: Context, serverId: String) {
        val server = _servers.value.find { it.id == serverId } ?: return
        val newServer = server.copy(enabled = !server.enabled)
        _servers.value = _servers.value.map { if (it.id == serverId) newServer else it }
        saveServers(context)
        
        getScope().launch {
            if (newServer.enabled) {
                connectToServer(newServer)
            } else {
                disconnectFromServer(serverId)
            }
        }
    }

    fun connectAll() {
        getScope().launch {
            _servers.value.filter { it.enabled }.forEach { server ->
                connectToServer(server)
            }
        }
    }

    private suspend fun connectToServer(server: MCPServerConfig) {
        updateServerStatus(server.id, server.name, MCPConnectionState.CONNECTING)
        
        try {
            clientsMutex.withLock {
                clients[server.id]?.close()
                
                val client = MCPClient(server)
                clients[server.id] = client
            
                val initSuccess = withTimeoutOrNull(30000) {
                    client.initialize()
                } ?: false
                
                if (!initSuccess) {
                    throw Exception("Initialization timeout")
                }
                
                val serverTools = withTimeoutOrNull(30000) {
                    client.listTools()
                } ?: emptyList()
                
                _tools.value = _tools.value.filter { it.serverId != server.id } + serverTools
                
                val finalState = if (serverTools.isEmpty()) {
                    MCPConnectionState.STANDBY
                } else {
                    MCPConnectionState.CONNECTED
                }
                
                updateServerStatus(server.id, server.name, finalState, toolCount = serverTools.size)
                Log.i(TAG, "Connected to ${server.name} with ${serverTools.size} tools")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to ${server.name}: ${e.message}", e)
            updateServerStatus(server.id, server.name, MCPConnectionState.ERROR, e.message)
            clients.remove(server.id)
        }
    }

    private suspend fun disconnectFromServer(serverId: String) {
        clientsMutex.withLock {
            clients[serverId]?.close()
            clients.remove(serverId)
        }
        _tools.value = _tools.value.filter { it.serverId != serverId }
        _serverStatuses.value = _serverStatuses.value - serverId
    }

    private fun updateServerStatus(
        serverId: String, 
        serverName: String,
        state: MCPConnectionState, 
        error: String? = null,
        toolCount: Int = 0
    ) {
        _serverStatuses.value = _serverStatuses.value + (serverId to MCPServerStatus(
            serverId = serverId,
            serverName = serverName,
            state = state,
            error = error,
            toolCount = toolCount
        ))
    }

    suspend fun callTool(toolCall: MCPToolCall): MCPToolResult {
        val client = clients[toolCall.serverId]
            ?: return MCPToolResult("Server not connected", true, toolCall.toolName)
        
        return try {
            client.callTool(toolCall.toolName, toolCall.arguments)
        } catch (e: Exception) {
            MCPToolResult("Tool call failed: ${e.message}", true, toolCall.toolName)
        }
    }

    fun getAllTools(): List<MCPTool> = _tools.value

    fun getToolsForLLM(): JsonArray {
        val (tools, mapping) = _tools.value.toLLMToolsWithMapping()
        _toolMapping.value = mapping
        return tools
    }

    fun hasConnectedServers(): Boolean {
        return _serverStatuses.value.any { it.value.state == MCPConnectionState.CONNECTED }
    }

    fun getConnectedServerCount(): Int {
        return _serverStatuses.value.count { it.value.state == MCPConnectionState.CONNECTED }
    }

    fun getTotalToolCount(): Int = _tools.value.size
    
    suspend fun callToolBySanitizedName(sanitizedName: String, arguments: Map<String, JsonElement>): MCPToolResult {
        val mapping = _toolMapping.value[sanitizedName]
            ?: return MCPToolResult("Tool not found: $sanitizedName", true, sanitizedName)
        
        return callTool(MCPToolCall(
            toolName = mapping.originalName,
            arguments = arguments,
            serverId = mapping.serverId
        ))
    }
}
