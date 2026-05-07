package com.AbuAlaa.noor.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.AbuAlaa.noor.config.ConfigManager
import com.AbuAlaa.noor.data.Message
import com.AbuAlaa.noor.data.MessageFile
import com.AbuAlaa.noor.mcp.MCPManager
import com.AbuAlaa.noor.ui.components.MCPSettingsSheet
import com.AbuAlaa.noor.ui.components.NoorNavigationDrawer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoorScreen(
    contentPadding: PaddingValues,
    viewModel: NoorViewModel = viewModel(),
    onSettingsClick: () -> Unit = {},
    onModelsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputText by remember { mutableStateOf("") }
    
    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Speech Recognition launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            inputText = if (inputText.isNotEmpty()) "$inputText $spokenText" else spokenText
        }
    }
    
    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Start speech recognition
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA") // Arabic + English support
                putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن...")
            }
            speechRecognizerLauncher.launch(intent)
        }
    }
    
    // MCP Sheet state
    var showMCPSheet by remember { mutableStateOf(false) }
    
    // MCP state
    val mcpTools by MCPManager.tools.collectAsState()
    val mcpServerCount = MCPManager.getConnectedServerCount()
    
    // Initialize ConfigManager and MCP
    LaunchedEffect(Unit) {
        ConfigManager.init(context)
        MCPManager.init(context)
    }
    
    // Check if configured
    val isConfigured = remember(viewModel.availableModels) {
        ConfigManager.getProviderConfig().isValid()
    }
    
    // File pickers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val cursor = context.contentResolver.query(selectedUri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val fileName = if (nameIndex >= 0) it.getString(nameIndex) else "image.jpg"
                    val mimeType = context.contentResolver.getType(selectedUri) ?: "image/jpeg"
                    viewModel.addImageFromUri(context, selectedUri, fileName, mimeType)
                }
            }
        }
    }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val cursor = context.contentResolver.query(selectedUri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val fileName = if (nameIndex >= 0) it.getString(nameIndex) else "file"
                    val mimeType = context.contentResolver.getType(selectedUri) ?: "application/octet-stream"
                    if (mimeType.startsWith("image/")) {
                        viewModel.addImageFromUri(context, selectedUri, fileName, mimeType)
                    } else {
                        viewModel.addTextFileFromUri(context, selectedUri, fileName, mimeType)
                    }
                }
            }
        }
    }
    
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }
    
    // MCP Settings Sheet
    if (showMCPSheet) {
        MCPSettingsSheet(onDismiss = { showMCPSheet = false })
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NoorNavigationDrawer(
                conversations = viewModel.conversations,
                currentConversation = viewModel.currentConversation,
                onConversationClick = { conversation ->
                    viewModel.selectConversation(conversation)
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.newChat()
                    scope.launch { drawerState.close() }
                },
                onDeleteConversation = { conversation ->
                    viewModel.deleteConversation(conversation)
                },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    onSettingsClick()
                },
                onMCPClick = {
                    scope.launch { drawerState.close() }
                    showMCPSheet = true
                },
                onModelsClick = {
                    scope.launch { drawerState.close() }
                    onModelsClick()
                }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "القائمة",
                                tint = Color.White
                            )
                        }
                    },
                    title = { 
                        Column(
                            modifier = Modifier.clickable { onModelsClick() }
                        ) {
                            Text("نور", color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = viewModel.selectedModelId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                if (mcpTools.isNotEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "• ${mcpTools.size} أداة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1F24)
                    ),
                    actions = {
                        // MCP Button
                        IconButton(onClick = { showMCPSheet = true }) {
                            Icon(
                                Icons.Default.Extension,
                                contentDescription = "MCP",
                                tint = if (mcpTools.isNotEmpty()) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f)
                            )
                        }
                        
                        if (viewModel.messages.isNotEmpty()) {
                            IconButton(onClick = { viewModel.newChat() }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "محادثة جديدة",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                )
            },
            modifier = Modifier.padding(contentPadding)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1117))
                    .padding(innerPadding)
                    .imePadding()
            ) {
                if (!isConfigured) {
                    ConfigurationWarning()
                }
                
                if (viewModel.messages.isEmpty()) {
                    WelcomeMessage(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(viewModel.messages, key = { it.id }) { message ->
                            ChatMessageBubble(
                                message = message,
                                isLoading = viewModel.isLoading && message == viewModel.messages.lastOrNull() && !message.isUser
                            )
                        }
                    }
                }
                
                // Pending files row
                if (viewModel.pendingFiles.isNotEmpty()) {
                    PendingFilesRow(
                        files = viewModel.pendingFiles,
                        onRemove = { viewModel.removePendingFile(it) }
                    )
                }
                
                ChatInputField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank() || viewModel.pendingFiles.isNotEmpty()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                    },
                    onAttachImage = { imagePickerLauncher.launch("image/*") },
                    onAttachFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                    onVoiceInput = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    isLoading = viewModel.isLoading,
                    onStop = { viewModel.stopGeneration() },
                    onRegenerate = { viewModel.regenerateLastMessage() },
                    canRegenerate = viewModel.messages.isNotEmpty() && !viewModel.isLoading
                )
            }
        }
    }
}

@Composable
private fun PendingFilesRow(
    files: List<MessageFile>,
    onRemove: (MessageFile) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1F24))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files) { file ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D333B)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (file.isImage()) Icons.Default.Image else Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = file.name.take(15) + if (file.name.length > 15) "..." else "",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "إزالة",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onRemove(file) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigurationWarning() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2E1E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "يرجى إعداد مفتاح API",
                    color = Color(0xFFFFB74D),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "اذهب للإعدادات > إعدادات نور",
                    color = Color(0xFFFFB74D).copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun WelcomeMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "✨", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "مرحباً بك في نور",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "مساعدك الذكي للإجابة على أسئلتك",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "اكتب سؤالك للبدء...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(message: Message, isLoading: Boolean = false) {
    val isUser = message.isUser
    val clipboardManager = LocalClipboardManager.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    message.content.startsWith("⚠️") -> Color(0xFF4A2020)
                    isUser -> Color(0xFF1E3A5F)
                    else -> Color(0xFF1E1E2E)
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Copy button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "نسخ",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                // Show files if any
                if (message.files.isNotEmpty()) {
                    message.files.forEach { file ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                if (file.isImage()) Icons.Default.Image else Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = file.name,
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                if (isLoading && message.content.isEmpty()) {
                    LoadingDots()
                } else {
                    SelectionContainer {
                        Text(
                            text = message.content,
                            color = if (message.content.startsWith("⚠️")) Color(0xFFFF8A80) else Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha * (1f - index * 0.2f)))
            )
        }
    }
}

@Composable
private fun ChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImage: () -> Unit,
    onAttachFile: () -> Unit,
    onVoiceInput: () -> Unit,
    isLoading: Boolean,
    onStop: () -> Unit,
    onRegenerate: () -> Unit,
    canRegenerate: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1F24))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Attach buttons
        IconButton(onClick = onAttachImage) {
            Icon(Icons.Default.Image, contentDescription = "صورة", tint = Color.White.copy(alpha = 0.7f))
        }
        
        IconButton(onClick = onAttachFile) {
            Icon(Icons.Default.AttachFile, contentDescription = "ملف", tint = Color.White.copy(alpha = 0.7f))
        }
        
        // Voice input button
        IconButton(onClick = onVoiceInput) {
            Icon(Icons.Default.Mic, contentDescription = "تسجيل صوتي", tint = Color(0xFF4CAF50))
        }
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(text = "اكتب رسالتك...", color = Color.White.copy(alpha = 0.5f))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4A90D9),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = Color(0xFF4A90D9)
            ),
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (!isLoading) onSend() }),
            maxLines = 4
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        if (isLoading) {
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
            ) {
                Icon(Icons.Default.Stop, contentDescription = "إيقاف", tint = Color.White)
            }
        } else if (value.isNotBlank()) {
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4A90D9))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = Color.White)
            }
        } else if (canRegenerate) {
            IconButton(
                onClick = onRegenerate,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4A90D9).copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "إعادة", tint = Color.White)
            }
        }
    }
}
