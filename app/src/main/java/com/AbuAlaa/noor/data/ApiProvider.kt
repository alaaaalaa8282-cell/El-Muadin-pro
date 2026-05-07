package com.AbuAlaa.noor.data

import kotlinx.serialization.Serializable

@Serializable
enum class ApiProvider(val displayName: String, val defaultBaseUrl: String) {
    HUGGINGFACE(
        displayName = "HuggingFace Router",
        defaultBaseUrl = "https://router.huggingface.co/v1"
    ),
    GOOGLE_AI_STUDIO(
        displayName = "Google AI Studio API (Direct)",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta"
    );

    companion object {
        fun fromString(value: String): ApiProvider {
            return entries.find { it.name == value } ?: HUGGINGFACE
        }
    }
}

enum class AuthMethod {
    API_KEY,
    GOOGLE_SIGN_IN
}

@Serializable
data class ProviderConfig(
    val provider: ApiProvider = ApiProvider.HUGGINGFACE,
    val baseUrl: String = provider.defaultBaseUrl,
    val apiKey: String = "",
    val customHeaders: Map<String, String> = emptyMap(),
    val useGoogleAuth: Boolean = false
) {
    fun getChatCompletionsUrl(): String {
        return "${baseUrl.trimEnd('/')}/chat/completions"
    }

    fun getAuthHeader(): String {
        return when (provider) {
            ApiProvider.HUGGINGFACE -> "Bearer $apiKey"
            ApiProvider.GOOGLE_AI_STUDIO -> ""
        }
    }
    
    fun getAuthMethod(): AuthMethod {
        return when {
            provider == ApiProvider.GOOGLE_AI_STUDIO && useGoogleAuth -> AuthMethod.GOOGLE_SIGN_IN
            else -> AuthMethod.API_KEY
        }
    }
    
    fun getApiKeyForQueryParam(): String? {
        return when (provider) {
            ApiProvider.GOOGLE_AI_STUDIO -> apiKey
            else -> null
        }
    }

    fun getApiKeyHeaderPair(): Pair<String, String>? {
        return when (provider) {
            ApiProvider.GOOGLE_AI_STUDIO -> "x-goog-api-key" to apiKey
            else -> null
        }
    }

    fun isValid(): Boolean {
        return baseUrl.isNotBlank() && (apiKey.isNotBlank() || useGoogleAuth)
    }

    fun getHeaders(): Map<String, String> {
        val headers = mutableMapOf(
            "Content-Type" to "application/json",
            "Authorization" to getAuthHeader()
        )
        
        headers.putAll(customHeaders)
        
        return headers
    }
}
