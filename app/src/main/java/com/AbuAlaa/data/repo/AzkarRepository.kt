package com.AbuAlaa.data.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ZikrItem(
    val text: String,
    val repeat: Int = 1,
    val title: String = "",
    val benefit: String = ""
)

enum class AzkarType { MORNING, EVENING }

@Singleton
class AzkarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttp: OkHttpClient
) {
    private val cacheFile = File(context.filesDir, "adhkar_cache.json")

    suspend fun getAzkar(type: AzkarType, forceRefresh: Boolean = false): List<ZikrItem> =
        withContext(Dispatchers.IO) {
            val fileName = if (type == AzkarType.MORNING) "morning_adhkar.json" else "evening_adhkar.json"
            
            runCatching {
                val jsonText = context.assets.open(fileName).bufferedReader().use { it.readText() }
                parseNewFormat(sanitize(jsonText))
            }.getOrElse {
                // Fallback to old format/file if new loading fails
                val fallback = sanitize(loadAsset())
                parse(fallback, type)
            }
        }

    private fun loadAsset(): String =
        context.assets.open("adhkar_fallback.json").bufferedReader().use { it.readText() }
        
    // Old loadJson removed as we now use local assets as primary source


    private fun sanitize(text: String): String {
        var t = text
        if (t.isNotEmpty() && t[0] == '\uFEFF') {
            t = t.substring(1)
        }
        return t.trim()
    }

    private fun parseNewFormat(jsonText: String): List<ZikrItem> {
        val root = JSONObject(jsonText)
        
        // Check if it has "adhkar" array (new format)
        val arr = root.optJSONArray("adhkar") ?: JSONArray(jsonText)
        
        val list = ArrayList<ZikrItem>(arr.length())
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            
            // Try "text" first, then "content"
            val text = item.optString("text", item.optString("content", "")).trim()
            val title = item.optString("title", "")
            val benefit = item.optString("benefit", "")
            
            // "repeat" or "count" - might be string or int
            val repeatVal = item.opt("repeat") ?: item.opt("count") ?: 1
            val repeat = when (repeatVal) {
                is Int -> repeatVal
                is String -> repeatVal.toIntOrNull() ?: 1
                else -> 1
            }
            
            if (text.isNotBlank()) {
                list.add(ZikrItem(
                    text = text,
                    repeat = repeat,
                    title = title,
                    benefit = benefit
                ))
            }
        }
        return list
    }

    private fun parse(jsonText: String, type: AzkarType): List<ZikrItem> {
        val obj = JSONObject(jsonText)

        val key = if (type == AzkarType.MORNING) "Morning" else "Evening"
        val arr: JSONArray = obj.optJSONArray(key) ?: JSONArray()

        val list = ArrayList<ZikrItem>(arr.length())
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val text = item.optString("text").trim()
            val repeat = item.optInt("repeat", 1).coerceAtLeast(1)
            if (text.isNotBlank()) list.add(ZikrItem(text = text, repeat = repeat))
        }
        return list
    }
}
