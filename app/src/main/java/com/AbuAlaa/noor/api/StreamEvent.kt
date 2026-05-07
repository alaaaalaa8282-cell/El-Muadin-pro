package com.AbuAlaa.noor.api

sealed class StreamEvent {
    data class Token(
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : StreamEvent()
    
    data class Status(
        val message: String,
        val isError: Boolean = false,
        val statusCode: Int? = null
    ) : StreamEvent()
    
    data class RouterMetadata(
        val route: String,
        val model: String,
        val provider: String?
    ) : StreamEvent()
    
    data class Complete(
        val fullText: String,
        val interrupted: Boolean = false
    ) : StreamEvent()
    
    data class Error(
        val error: String,
        val statusCode: Int? = null
    ) : StreamEvent()
    
    object KeepAlive : StreamEvent()
    
    data class ToolCall(
        val index: Int,
        val id: String?,
        val name: String?,
        val arguments: String?
    ) : StreamEvent()
    
    data class ToolCallsComplete(
        val fullText: String
    ) : StreamEvent()
}
