package com.AbuAlaa.noor.data.db

import androidx.room.TypeConverter
import com.AbuAlaa.noor.data.Message
import com.AbuAlaa.noor.data.MessageFile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
data class SerializableMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val files: List<SerializableMessageFile> = emptyList(),
    val model: String = ""
)

@Serializable
data class SerializableMessageFile(
    val name: String,
    val mime: String,
    val value: String,
    val type: String
)

class Converters {
    
    companion object {
        private val json = Json { 
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    @TypeConverter
    fun fromMessagesJson(value: String): List<Message> {
        return try {
            val serializableMessages = json.decodeFromString<List<SerializableMessage>>(value)
            serializableMessages.map { it.toMessage() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toMessagesJson(messages: List<Message>): String {
        return try {
            val serializableMessages = messages.map { it.toSerializable() }
            json.encodeToString(serializableMessages)
        } catch (e: Exception) {
            "[]"
        }
    }

    private fun Message.toSerializable() = SerializableMessage(
        id = id,
        content = content,
        isUser = isUser,
        timestamp = timestamp,
        files = files.map { it.toSerializable() },
        model = model
    )

    private fun SerializableMessage.toMessage() = Message(
        id = id,
        content = content,
        isUser = isUser,
        timestamp = timestamp,
        files = files.map { it.toMessageFile() },
        model = model
    )

    private fun MessageFile.toSerializable() = SerializableMessageFile(
        name = name,
        mime = mime,
        value = value,
        type = type.name
    )

    private fun SerializableMessageFile.toMessageFile() = MessageFile(
        type = MessageFile.FileDataType.valueOf(type),
        name = name,
        value = value,
        mime = mime
    )
}
