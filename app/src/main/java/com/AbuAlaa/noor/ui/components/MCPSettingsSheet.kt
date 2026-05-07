package com.AbuAlaa.noor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.AbuAlaa.noor.mcp.MCPConnectionState
import com.AbuAlaa.noor.mcp.MCPManager
import com.AbuAlaa.noor.mcp.MCPServerConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MCPSettingsSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val servers by MCPManager.servers.collectAsState()
    val serverStatuses by MCPManager.serverStatuses.collectAsState()
    val tools by MCPManager.tools.collectAsState()
    
    val surfaceColor = Color(0xFF1A1F24)
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.7f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Extension,
                        contentDescription = null,
                        tint = textPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "MCP Servers",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                
                Text(
                    "${tools.size} أداة",
                    color = textSecondary,
                    fontSize = 14.sp
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Servers List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(servers) { server ->
                    MCPServerItem(
                        server = server,
                        status = serverStatuses[server.id],
                        onToggle = { MCPManager.toggleServerEnabled(context, server.id) }
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MCPServerItem(
    server: MCPServerConfig,
    status: com.AbuAlaa.noor.mcp.MCPServerStatus?,
    onToggle: () -> Unit
) {
    val surfaceVariant = Color(0xFF2D333B)
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.7f)
    
    val stateColor = when (status?.state) {
        MCPConnectionState.CONNECTED -> Color(0xFF4CAF50)
        MCPConnectionState.CONNECTING -> Color(0xFFFF9800)
        MCPConnectionState.ERROR -> Color(0xFFF44336)
        MCPConnectionState.STANDBY -> Color(0xFF2196F3)
        else -> Color.Gray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(stateColor)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        server.name,
                        color = textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    if (status?.toolCount ?: 0 > 0) {
                        Text(
                            "${status?.toolCount} أداة",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Switch(
                checked = server.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4A90D9),
                    checkedTrackColor = Color(0xFF4A90D9).copy(alpha = 0.5f)
                )
            )
        }
    }
}
