package com.AbuAlaa.noor.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.AbuAlaa.R
import com.AbuAlaa.noor.config.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

@Serializable
data class CatalogModel(
    val id: String,
    val name: String,
    val company: String,
    val features: String,
    val vision: Boolean = false,
    val thinking: Boolean = false,
    val tools: Boolean = false
)

@Serializable
data class ModelsCatalog(
    val metadata: CatalogMetadata,
    val models: List<CatalogModel>
)

@Serializable
data class CatalogMetadata(
    val total_models: Int,
    val last_updated: String,
    val source: String
)

private fun loadModelsCatalog(context: Context): List<CatalogModel> {
    return try {
        val inputStream = context.assets.open("models_catalog.json")
        val reader = InputStreamReader(inputStream)
        val content = reader.readText()
        reader.close()
        val json = Json { ignoreUnknownKeys = true }
        val catalog = json.decodeFromString<ModelsCatalog>(content)
        catalog.models
    } catch (e: Exception) {
        emptyList()
    }
}

private fun generateModelColor(modelId: String): Color {
    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFF45B7D1), Color(0xFFA55EEA),
        Color(0xFFFF9F43), Color(0xFF2ED573), Color(0xFF1E90FF), Color(0xFFFF6B81)
    )
    return colors[kotlin.math.abs(modelId.hashCode()) % colors.size]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    onBackClick: () -> Unit,
    onModelSelect: (String) -> Unit,
    selectedModelId: String
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(selectedModelId) }
    var isLoading by remember { mutableStateOf(true) }
    var models by remember { mutableStateOf<List<CatalogModel>>(emptyList()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val backgroundColor = Color(0xFFFAF8F5)
    val surfaceColor = Color(0xFFF5F0E8)
    val textPrimary = Color(0xFF10171A)
    val textSecondary = Color(0xFF6B7280)
    
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            models = withContext(Dispatchers.IO) {
                loadModelsCatalog(context)
            }
            isLoading = false
        }
    }
    
    val filteredModels = models.filter {
        it.id.contains(searchQuery, ignoreCase = true) ||
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.company.contains(searchQuery, ignoreCase = true)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Models",
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (models.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${filteredModels.size})",
                            color = textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            models = withContext(Dispatchers.IO) {
                                loadModelsCatalog(context)
                            }
                            isLoading = false
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = textPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = backgroundColor
            )
        )
        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text(
                    text = "Search by name...",
                    color = textSecondary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = textSecondary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                focusedBorderColor = Color(0xFF4A90D9),
                unfocusedBorderColor = textSecondary.copy(alpha = 0.3f),
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor,
                cursorColor = Color(0xFF4A90D9)
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Loading State
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF4A90D9))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading models...",
                        color = textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Language Models Section
                item {
                    SectionHeader("Language Models")
                }
                
                items(filteredModels) { model ->
                    ModelCard(
                        model = model,
                        isSelected = selectedModel == model.id,
                        onClick = {
                            selectedModel = model.id
                            onModelSelect(model.id)
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val textPrimary = Color(0xFF10171A)
    val dividerColor = Color(0xFFE5E7EB)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(dividerColor)
        )
    }
}

@Composable
private fun ModelCard(
    model: CatalogModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = Color(0xFFFAF8F5)
    val surfaceColor = Color(0xFFF5F0E8)
    val textPrimary = Color(0xFF10171A)
    val textSecondary = Color(0xFF6B7280)
    
    val isOmni = model.id == "Omni" || model.name.contains("Omni", ignoreCase = true)
    val omniColor = Color(0xFF8B5CF6)
    val color = if (isOmni) omniColor else generateModelColor(model.id)
    
    val visionColor = Color(0xFFF59E0B)
    val thinkingColor = Color(0xFF3B82F6)
    val toolsColor = Color(0xFF10B981)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isOmni) omniColor.copy(alpha = 0.1f) else surfaceColor)
            .border(
                width = if (isSelected) 2.dp else if (isOmni) 1.dp else 0.dp,
                color = if (isSelected) Color(0xFF4A90D9) else if (isOmni) omniColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Model Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isOmni -> {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_omni),
                            contentDescription = "Omni",
                            tint = omniColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    model.company.contains("google", ignoreCase = true) -> {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google",
                            modifier = Modifier.size(32.dp),
                            tint = Color.Unspecified
                        )
                    }
                    else -> {
                        Text(
                            text = model.name.take(2).uppercase(),
                            color = color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.name,
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                
                Text(
                    text = model.company,
                    color = textSecondary,
                    fontSize = 12.sp
                )
            }
            
            // Selection Indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4A90D9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // Capability Badges Row
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isOmni) {
                ModelBadge(text = "🔀 Router", color = omniColor)
            }
            
            if (model.vision) {
                ModelBadge(text = "👁 Vision", color = visionColor)
            }
            
            if (model.thinking) {
                ModelBadge(text = "🧠 Thinking", color = thinkingColor)
            }
            
            if (model.tools) {
                ModelBadge(text = "🔧 Tools", color = toolsColor)
            }
        }
        
        // Features / Description
        if (model.features.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = model.features,
                color = textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ModelBadge(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
