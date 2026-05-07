package com.AbuAlaa.noor.api

import android.util.Log
import com.AbuAlaa.noor.config.ConfigManager
import com.AbuAlaa.noor.data.ApiProvider
import com.AbuAlaa.noor.data.GoogleModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.HttpURLConnection
import java.net.URL

object ModelsApiClient {
    private const val TAG = "ModelsApiClient"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Serializable
    data class Provider(
        val supports_tools: Boolean? = null
    )

    @Serializable
    data class Architecture(
        val input_modalities: List<String>? = null
    )

    @Serializable
    data class ModelData(
        val id: String,
        val description: String? = null,
        val providers: List<Provider>? = null,
        val architecture: Architecture? = null
    )

    @Serializable
    data class ModelsListResponse(
        val data: List<ModelData>
    )

    enum class ModelType {
        LANGUAGE,
        EMBEDDING
    }

    data class FetchedModel(
        val id: String,
        val name: String,
        val displayName: String,
        val description: String?,
        val logoUrl: String?,
        val multimodal: Boolean,
        val supportsTools: Boolean,
        val modelType: ModelType = ModelType.LANGUAGE
    )

    suspend fun fetchModels(): Result<List<FetchedModel>> = withContext(Dispatchers.IO) {
        try {
            val providerConfig = ConfigManager.getProviderConfig()

            if (providerConfig.provider == ApiProvider.GOOGLE_AI_STUDIO) {
                Log.i(TAG, "Fetching Google AI Studio models from API...")
                return@withContext fetchGoogleAIStudioModelsFromAPI()
            }

            val baseUrl = providerConfig.baseUrl.trimEnd('/')
            val apiKey = providerConfig.apiKey
            val isHfRouter = baseUrl == "https://router.huggingface.co/v1"

            Log.i(TAG, "Fetching models from: $baseUrl/models")

            val url = URL("$baseUrl/models")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                connectTimeout = 30000
                readTimeout = 30000
            }

            val responseCode = connection.responseCode
            Log.i(TAG, "Response code: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "Error fetching models: $error")
                return@withContext Result.failure(Exception("HTTP $responseCode: $error"))
            }

            val responseBody = connection.inputStream.bufferedReader().readText()
            val parsed = json.decodeFromString<ModelsListResponse>(responseBody)

            Log.i(TAG, "Fetched ${parsed.data.size} models")

            val models = parsed.data.map { model ->
                val inputModalities = model.architecture?.input_modalities?.map { it.lowercase() } ?: emptyList()
                val supportsImage = inputModalities.contains("image") || inputModalities.contains("vision")
                val supportsTools = model.providers?.any { it.supports_tools == true } ?: false

                val logoUrl = if (isHfRouter && model.id.contains("/")) {
                    val org = model.id.split("/")[0]
                    "https://huggingface.co/api/avatars/${java.net.URLEncoder.encode(org, "UTF-8")}"
                } else null

                FetchedModel(
                    id = model.id,
                    name = model.id,
                    displayName = model.id,
                    description = model.description,
                    logoUrl = logoUrl,
                    multimodal = supportsImage,
                    supportsTools = supportsTools
                )
            }

            Result.success(models)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching models", e)
            Result.failure(e)
        }
    }

    fun createOmniRouterModel(): FetchedModel {
        val displayName = ConfigManager.get(ConfigManager.Keys.PUBLIC_LLM_ROUTER_DISPLAY_NAME, "Omni")
        val aliasId = ConfigManager.get(ConfigManager.Keys.PUBLIC_LLM_ROUTER_ALIAS_ID, "omni")
        val isMultimodal = ConfigManager.isMultimodalEnabled

        return FetchedModel(
            id = aliasId,
            name = aliasId,
            displayName = displayName,
            description = "Automatically routes your messages to the best model for your request.",
            logoUrl = null,
            multimodal = isMultimodal,
            supportsTools = true
        )
    }

    private suspend fun fetchGoogleAIStudioModelsFromAPI(): Result<List<FetchedModel>> = withContext(Dispatchers.IO) {
        try {
            val providerConfig = ConfigManager.getProviderConfig()
            val baseUrl = providerConfig.baseUrl.trimEnd('/')
            val apiKey = providerConfig.apiKey

            if (apiKey.isBlank()) {
                Log.e(TAG, "Google AI Studio API Key is empty")
                return@withContext Result.failure(Exception("Google AI Studio API Key is empty"))
            }

            val allModels = mutableListOf<FetchedModel>()
            var pageToken: String? = null

            do {
                val urlStrNoKey = buildString {
                    append("$baseUrl/models")
                    append("?pageSize=200")
                    if (!pageToken.isNullOrBlank()) {
                        append("&pageToken=")
                        append(java.net.URLEncoder.encode(pageToken!!, "UTF-8"))
                    }
                }

                val urlStrWithKey = buildString {
                    append("$baseUrl/models?key=")
                    append(java.net.URLEncoder.encode(apiKey, "UTF-8"))
                    append("&pageSize=200")
                    if (!pageToken.isNullOrBlank()) {
                        append("&pageToken=")
                        append(java.net.URLEncoder.encode(pageToken!!, "UTF-8"))
                    }
                }

                fun open(urlStr: String): HttpURLConnection =
                    (URL(urlStr).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("x-goog-api-key", apiKey)
                        connectTimeout = 20000
                        readTimeout = 30000
                    }

                var connection = open(urlStrNoKey)
                var responseCode = connection.responseCode
                var body = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()
                    ?.readText()
                    .orEmpty()

                if (responseCode !in 200..299) {
                    Log.w(TAG, "Google AI Studio models.list failed without key in URL, retrying with ?key=. HTTP $responseCode - $body")
                    connection.disconnect()

                    connection = open(urlStrWithKey)
                    responseCode = connection.responseCode
                    body = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                        ?.bufferedReader()
                        ?.readText()
                        .orEmpty()
                }

                if (responseCode !in 200..299) {
                    Log.e(TAG, "Google AI Studio models.list failed: HTTP $responseCode - $body")
                    return@withContext Result.failure(Exception("Google AI Studio models.list failed: HTTP $responseCode"))
                }

                val root = json.parseToJsonElement(body).jsonObject
                val models = root["models"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())

                models.forEach { element ->
                    val obj = element.jsonObject
                    val nameRaw = obj["name"]?.jsonPrimitive?.content ?: return@forEach
                    val modelId = nameRaw.removePrefix("models/")
                    val displayName = obj["displayName"]?.jsonPrimitive?.content ?: modelId
                    val description = obj["description"]?.jsonPrimitive?.contentOrNull

                    val methods = obj["supportedGenerationMethods"]?.jsonArray
                        ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                        ?.toSet()
                        ?: emptySet()

                    val supportsAnythingUseful = methods.contains("generateContent") ||
                            methods.contains("streamGenerateContent") ||
                            methods.contains("bidiGenerateContent") ||
                            methods.contains("predict") ||
                            methods.contains("predictLongRunning") ||
                            methods.any { it.startsWith("embed") } ||
                            methods.contains("embedContent") ||
                            methods.contains("embedText")

                    if (!supportsAnythingUseful) return@forEach

                    val modelType = when {
                        methods.any { it.startsWith("embed") } || methods.contains("embedContent") || methods.contains("embedText") -> ModelType.EMBEDDING
                        else -> ModelType.LANGUAGE
                    }

                    allModels.add(
                        FetchedModel(
                            id = "google/$modelId",
                            name = modelId,
                            displayName = displayName,
                            description = description,
                            logoUrl = "https://www.gstatic.com/lamda/images/gemini_sparkle_v002_d4735304ff6292a690345.svg",
                            multimodal = true,
                            supportsTools = true,
                            modelType = modelType
                        )
                    )
                }

                pageToken = root["nextPageToken"]?.jsonPrimitive?.contentOrNull
            } while (!pageToken.isNullOrBlank())

            val distinct = allModels.distinctBy { it.id }

            Log.i(TAG, "Fetched ${distinct.size} models from Google AI Studio API")
            Result.success(distinct)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Google AI Studio models from API", e)
            Result.failure(e)
        }
    }

    suspend fun getAllModels(): List<FetchedModel> {
        val providerConfig = ConfigManager.getProviderConfig()
        val archBaseUrl = ConfigManager.get(ConfigManager.Keys.LLM_ROUTER_ARCH_BASE_URL, "")

        val fetchedModels = fetchModels().getOrDefault(emptyList())

        return if (providerConfig.provider == ApiProvider.HUGGINGFACE && archBaseUrl.isNotBlank()) {
            listOf(createOmniRouterModel()) + fetchedModels
        } else {
            fetchedModels
        }
    }
}
