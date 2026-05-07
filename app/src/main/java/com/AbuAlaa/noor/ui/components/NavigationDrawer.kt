package com.AbuAlaa.noor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.AbuAlaa.noor.data.Conversation
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NoorNavigationDrawer(
    conversations: List<Conversation>,
    currentConversation: Conversation?,
    onConversationClick: (Conversation) -> Unit,
    onNewChat: () -> Unit,
    onDeleteConversation: (Conversation) -> Unit,
    onSettingsClick: () -> Unit = {},
    onMCPClick: () -> Unit = {},
    onModelsClick: () -> Unit = {}
) {
    val surfaceColor = Color(0xFF1A1F24)
    val surfaceVariant = Color(0xFF2D333B)
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.7f)
    val textMuted = Color.White.copy(alpha = 0.5f)
    val primary = Color(0xFF4A90D9)

    ModalDrawerSheet(
        modifier = Modifier.width(290.dp),
        drawerContainerColor = surfaceColor
    ) {
        Column(modifier = Modifier.fillMaxHeight().padding(vertical = 16.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✨",
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نور",
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "مساعدك الذكي",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick Action Icons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // New Chat
                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "محادثة جديدة",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Models
                IconButton(
                    onClick = onModelsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewModule,
                        contentDescription = "الموديلات",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // MCP Settings
                IconButton(
                    onClick = onMCPClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = "MCP",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Settings
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "الإعدادات",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Conversations List Header
            Text(
                text = "المحادثات الأخيرة",
                color = textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Conversations List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceVariant)
            ) {
                if (conversations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد محادثات بعد",
                                color = textMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(conversations) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            isSelected = currentConversation?.id == conversation.id,
                            onClick = { onConversationClick(conversation) },
                            onDelete = { onDeleteConversation(conversation) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Bottom info
            Text(
                text = "نور v1.0",
                color = textMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val surfaceVariant = Color(0xFF3D4449)
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.7f)
    val textMuted = Color.White.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) surfaceVariant else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = if (isSelected) textPrimary else textSecondary,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.title,
                color = if (isSelected) textPrimary else textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatTimestamp(conversation.timestamp),
                color = textMuted,
                fontSize = 11.sp
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "المزيد",
                    tint = textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("حذف") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "الآن"
        diff < 3600000 -> "${diff / 60000} دقيقة"
        diff < 86400000 -> "${diff / 3600000} ساعة"
        diff < 604800000 -> "${diff / 86400000} يوم"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
