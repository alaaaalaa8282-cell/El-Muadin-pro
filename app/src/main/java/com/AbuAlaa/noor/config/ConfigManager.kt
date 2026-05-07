package com.AbuAlaa.noor.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.AbuAlaa.noor.data.ApiProvider
import com.AbuAlaa.noor.data.ProviderConfig
import java.util.Properties

object ConfigManager {
    private const val TAG = "ConfigManager"
    private const val LEGACY_PREFS_NAME = "noor_config"
    private const val ENCRYPTED_PREFS_NAME = "noor_config_secure"

    private lateinit var sharedPrefs: SharedPreferences
    private val defaultConfig = Properties()
    private val localConfig = Properties()
    private var isInitialized = false
    
    private lateinit var appContext: Context
    
    fun getAppContext(): Context? = if (::appContext.isInitialized) appContext else null

    object Keys {
        const val API_PROVIDER = "API_PROVIDER"
        const val OPENAI_BASE_URL = "OPENAI_BASE_URL"
        const val OPENAI_API_KEY = "OPENAI_API_KEY"
        const val GOOGLE_STUDIO_API_KEY = "GOOGLE_STUDIO_API_KEY"
        const val USE_GOOGLE_AUTH = "USE_GOOGLE_AUTH"
        const val PUBLIC_APP_NAME = "PUBLIC_APP_NAME"
        const val PUBLIC_APP_DESCRIPTION = "PUBLIC_APP_DESCRIPTION"
        const val DEFAULT_MODEL = "DEFAULT_MODEL"
        const val TASK_MODEL = "TASK_MODEL"
        const val LLM_ROUTER_ARCH_BASE_URL = "LLM_ROUTER_ARCH_BASE_URL"
        const val LLM_ROUTER_ARCH_MODEL = "LLM_ROUTER_ARCH_MODEL"
        const val LLM_ROUTER_FALLBACK_MODEL = "LLM_ROUTER_FALLBACK_MODEL"
        const val LLM_ROUTER_OTHER_ROUTE = "LLM_ROUTER_OTHER_ROUTE"
        const val LLM_ROUTER_ARCH_TIMEOUT_MS = "LLM_ROUTER_ARCH_TIMEOUT_MS"
        const val LLM_ROUTER_MAX_ASSISTANT_LENGTH = "LLM_ROUTER_MAX_ASSISTANT_LENGTH"
        const val LLM_ROUTER_MAX_PREV_USER_LENGTH = "LLM_ROUTER_MAX_PREV_USER_LENGTH"
        const val LLM_ROUTER_ENABLE_MULTIMODAL = "LLM_ROUTER_ENABLE_MULTIMODAL"
        const val LLM_ROUTER_ENABLE_TOOLS = "LLM_ROUTER_ENABLE_TOOLS"
        const val PUBLIC_LLM_ROUTER_DISPLAY_NAME = "PUBLIC_LLM_ROUTER_DISPLAY_NAME"
        const val PUBLIC_LLM_ROUTER_ALIAS_ID = "PUBLIC_LLM_ROUTER_ALIAS_ID"
        const val LLM_SUMMARIZATION = "LLM_SUMMARIZATION"
        const val ENABLE_DARK_MODE = "ENABLE_DARK_MODE"
        const val MESSAGES_PER_MINUTE = "MESSAGES_PER_MINUTE"
        const val MAX_MESSAGE_LENGTH = "MAX_MESSAGE_LENGTH"
    }

    fun init(context: Context) {
        if (isInitialized) return
        
        appContext = context.applicationContext

        sharedPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

        try {
            context.assets.open("config.properties").use { stream -> 
                defaultConfig.load(stream) 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config.properties", e)
        }

        try {
            context.assets.open("local.properties").use { stream -> 
                localConfig.load(stream) 
            }
        } catch (e: Exception) {
        }

        isInitialized = true
    }


    fun get(key: String, default: String = ""): String {
        if (sharedPrefs.contains(key)) {
            return sharedPrefs.getString(key, default) ?: default
        }

        if (localConfig.containsKey(key)) {
            return localConfig.getProperty(key, default)
        }

        return defaultConfig.getProperty(key, default)
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        val value = get(key, default.toString())
        return value.equals("true", ignoreCase = true)
    }

    fun getInt(key: String, default: Int = 0): Int {
        return try {
            get(key, default.toString()).toInt()
        } catch (e: NumberFormatException) {
            default
        }
    }

    fun set(key: String, value: String) {
        sharedPrefs.edit().putString(key, value).apply()
    }

    fun setBoolean(key: String, value: Boolean) {
        set(key, value.toString())
    }

    fun setInt(key: String, value: Int) {
        set(key, value.toString())
    }

    fun remove(key: String) {
        sharedPrefs.edit().remove(key).apply()
    }

    fun hasUserValue(key: String): Boolean {
        return sharedPrefs.contains(key)
    }

    fun getPublicConfig(): Map<String, String> {
        val config = mutableMapOf<String, String>()

        defaultConfig.stringPropertyNames().filter { it.startsWith("PUBLIC_") }.forEach { key ->
            config[key] = defaultConfig.getProperty(key, "")
        }

        localConfig.stringPropertyNames().filter { it.startsWith("PUBLIC_") }.forEach { key ->
            config[key] = localConfig.getProperty(key, "")
        }

        sharedPrefs.all.filter { it.key.startsWith("PUBLIC_") }.forEach { (key, value) ->
            config[key] = value?.toString() ?: ""
        }

        return config
    }

    val openAiBaseUrl: String
        get() = get(Keys.OPENAI_BASE_URL, "https://router.huggingface.co/v1")

    val openAiApiKey: String
        get() = get(Keys.OPENAI_API_KEY, "")

    val googleStudioApiKey: String
        get() = get(Keys.GOOGLE_STUDIO_API_KEY, "")

    val appName: String
        get() = get(Keys.PUBLIC_APP_NAME, "Noor")

    val defaultModel: String
        get() = get(Keys.DEFAULT_MODEL, "gpt-4")

    val isDarkModeEnabled: Boolean
        get() = getBoolean(Keys.ENABLE_DARK_MODE, true)

    val isMultimodalEnabled: Boolean
        get() = getBoolean(Keys.LLM_ROUTER_ENABLE_MULTIMODAL, true)

    val messagesPerMinute: Int
        get() = getInt(Keys.MESSAGES_PER_MINUTE, 60)

    val maxMessageLength: Int
        get() = getInt(Keys.MAX_MESSAGE_LENGTH, 4096)
    
    fun getApiKeyForProvider(provider: ApiProvider): String {
        val key = when (provider) {
            ApiProvider.GOOGLE_AI_STUDIO -> Keys.GOOGLE_STUDIO_API_KEY
            ApiProvider.HUGGINGFACE -> Keys.OPENAI_API_KEY
        }
        return get(key, "")
    }

    fun getBaseUrlForProvider(provider: ApiProvider): String {
        val baseOverride = get(Keys.OPENAI_BASE_URL, "")
        return if (baseOverride.isNotBlank()) baseOverride else provider.defaultBaseUrl
    }
    
    fun getDefaultModelForProvider(provider: ApiProvider): String {
        val globalDefault = get(Keys.DEFAULT_MODEL, "")
        if (globalDefault.isNotBlank()) {
            return globalDefault
        }
        
        return when(provider) {
            ApiProvider.HUGGINGFACE -> "omni"
            ApiProvider.GOOGLE_AI_STUDIO -> "gemini-2.5-flash"
        }
    }

    fun getProviderConfig(): ProviderConfig {
        val providerStr = get(Keys.API_PROVIDER, ApiProvider.HUGGINGFACE.name)
        val provider = ApiProvider.fromString(providerStr)

        val baseUrl = getBaseUrlForProvider(provider)
        val apiKey = getApiKeyForProvider(provider)
        
        val useGoogleAuth = getBoolean(Keys.USE_GOOGLE_AUTH, false)

        return ProviderConfig(
            provider = provider,
            baseUrl = baseUrl,
            apiKey = apiKey,
            useGoogleAuth = useGoogleAuth
        )
    }
    
    fun saveProviderConfig(config: ProviderConfig) {
        set(Keys.API_PROVIDER, config.provider.name)
        set(Keys.OPENAI_BASE_URL, config.baseUrl) 
        
        if (config.provider == ApiProvider.GOOGLE_AI_STUDIO) {
            set(Keys.GOOGLE_STUDIO_API_KEY, config.apiKey)
            remove(Keys.OPENAI_API_KEY)
        } else {
            set(Keys.OPENAI_API_KEY, config.apiKey)
            remove(Keys.GOOGLE_STUDIO_API_KEY)
        }
        setBoolean(Keys.USE_GOOGLE_AUTH, config.useGoogleAuth)
    }

    fun getGoogleStudioApiKeyFromFile(context: Context): String {
        return try {
            val properties = Properties()
            context.assets.open("config.properties").use { inputStream ->
                properties.load(inputStream)
            }
            val key = properties.getProperty("GOOGLE_STUDIO_API_KEY", "")
            if (key.isNotBlank()) {
                Log.i(TAG, "Successfully loaded GOOGLE_STUDIO_API_KEY from config.properties (length: ${key.length})")
            } else {
                Log.w(TAG, "GOOGLE_STUDIO_API_KEY not found or empty in config.properties")
            }
            key
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read GOOGLE_STUDIO_API_KEY from config.properties", e)
            ""
        }
    }
    
    fun hasGoogleStudioApiKeyInFile(context: Context): Boolean {
        return getGoogleStudioApiKeyFromFile(context).isNotBlank()
    }
    
    fun getProviderConfigWithApiKey(context: Context): ProviderConfig {
        val baseConfig = getProviderConfig()
        
        if (baseConfig.provider == ApiProvider.GOOGLE_AI_STUDIO && 
            baseConfig.apiKey.isBlank()) {
            
            val fileApiKey = getGoogleStudioApiKeyFromFile(context)
            if (fileApiKey.isNotBlank()) {
                Log.i(TAG, "Using Google AI Studio API key from config.properties")
                return baseConfig.copy(apiKey = fileApiKey)
            } else {
                Log.w(TAG, "No Google AI Studio API key found in SharedPreferences or config.properties")
            }
        }
        
        return baseConfig
    }
}
