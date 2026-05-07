package com.AbuAlaa.noor.data

object GoogleModels {
    
    data class GoogleModel(
        val id: String,
        val displayName: String,
        val description: String,
        val multimodal: Boolean,
        val supportsTools: Boolean,
        val type: ModelType = ModelType.CHAT
    )
    
    enum class ModelType {
        CHAT,
        EMBEDDING
    }
    
    val AVAILABLE_MODELS = listOf(
        GoogleModel(
            id = "google/gemini-3-pro-preview",
            displayName = "Gemini 3 Pro (Preview)",
            description = "Next-gen most advanced model",
            multimodal = true,
            supportsTools = true,
            type = ModelType.CHAT
        ),
        GoogleModel(
            id = "google/gemini-3-flash-preview",
            displayName = "Gemini 3 Flash (Preview)",
            description = "Next-gen fast model",
            multimodal = true,
            supportsTools = true,
            type = ModelType.CHAT
        ),
        GoogleModel(
            id = "google/gemini-2.5-pro",
            displayName = "Gemini 2.5 Pro",
            description = "Most advanced stable model",
            multimodal = true,
            supportsTools = true,
            type = ModelType.CHAT
        ),
        GoogleModel(
            id = "google/gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash",
            description = "Fast and efficient (recommended)",
            multimodal = true,
            supportsTools = true,
            type = ModelType.CHAT
        ),
        GoogleModel(
            id = "google/gemini-2.0-flash-001",
            displayName = "Gemini 2.0 Flash",
            description = "Fast and efficient model for most tasks",
            multimodal = true,
            supportsTools = true
        ),
        GoogleModel(
            id = "google/gemini-1.5-pro-002",
            displayName = "Gemini 1.5 Pro",
            description = "Advanced model with extended context",
            multimodal = true,
            supportsTools = true,
            type = ModelType.CHAT
        ),
        GoogleModel(
            id = "google/gemini-1.5-flash-002",
            displayName = "Gemini 1.5 Flash",
            description = "Balanced performance and speed",
            multimodal = true,
            supportsTools = true,
            type = ModelType.CHAT
        ),
        GoogleModel(
            id = "google/gemini-embedding-001",
            displayName = "Gemini Embedding",
            description = "Text embedding model",
            multimodal = false,
            supportsTools = false,
            type = ModelType.EMBEDDING
        )
    )
    
    fun getDefaultModel(): String = "google/gemini-2.0-flash-001"
    
    fun isValidModel(modelId: String): Boolean {
        return AVAILABLE_MODELS.any { it.id == modelId }
    }
    
    fun getStableChatModels(): List<GoogleModel> {
        return AVAILABLE_MODELS.filter { model ->
            val id = model.id.lowercase()
            !id.contains("exp") &&
            !id.contains("preview") &&
            !id.contains("embedding") &&
            !id.contains("lite")
        }
    }
    
    object ModelCategory {
        val RECOMMENDED = listOf(
            "google/gemini-2.5-flash",
            "google/gemini-2.0-flash-001",
            "google/gemini-1.5-flash-002"
        )
        
        val PRO = listOf(
            "google/gemini-2.5-pro",
            "google/gemini-1.5-pro-002"
        )
    }
}
