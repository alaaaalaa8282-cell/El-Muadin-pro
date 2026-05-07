package com.AbuAlaa.noor.mcp

object MCPUtils {
    fun hasAuthHeader(headers: Map<String, String>): Boolean {
        return headers.keys.any { it.equals("Authorization", ignoreCase = true) }
    }
    
    fun hasNonEmptyToken(token: String?): Boolean {
        return !token.isNullOrBlank() && token.length > 10
    }
}
