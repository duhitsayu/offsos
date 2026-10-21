package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.AppStrings
import com.example.model.BroadcastingState
import com.example.model.MeshGroup
import com.example.model.MeshNode
import com.example.model.NodeRole
import com.example.ui.components.CreateGroupDialog
import com.example.ui.components.GroupQrCodeDialog
import com.example.ui.components.JoinGroupDialog
import com.example.ui.components.OffSosLogo
import com.example.ui.theme.TacticalAlertRed
import com.example.ui.theme.TacticalAmber

@Composable
fun MeshScreen(
    peers: List<MeshNode>,
    localNodeId: String,
    localCallsign: String,
    broadcastingState: BroadcastingState,
    currentGroup: MeshGroup?,
    isScanning: Boolean,
    appLanguage: AppLanguage,
    onToggleBroadcasting: () -> Unit,
    onScanPeers: () -> Unit,
    onCreateGroup: (String, String) -> Unit,
    onJoinGroup: (String, String) -> Unit,
    onLeaveGroup: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBroadcasting = broadcastingState == BroadcastingState.BROADCASTING

    var showJoinDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "beacon_alert")
    val beaconAlpha by pulseTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. ONE-TAP SOS BROADCASTING HERO BANNER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = if (isBroadcasting) TacticalAlertRed else MaterialTheme.colorScheme.onBackground,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .background(
                        if (isBroadcasting) TacticalAlertRed.copy(alpha = beaconAlpha * 0.15f) else MaterialTheme.colorScheme.surface
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OffSosLogo(size = 38.dp, pulsing = isBroadcasting)
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            if (isBroadcasting) TacticalAlertRed else Color(0xFF2E7D32),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBroadcasting) AppStrings.get("broadcasting_mode", appLanguage) else AppStrings.get("listening_mode", appLanguage),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = if (isBroadcasting) TacticalAlertRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isBroadcasting) "SOS DISTRESS ACTIVE" else "STEALTH MONITORING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = if (isBroadcasting) TacticalAlertRed else TacticalAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isBroadcasting) {
                            AppStrings.get("broadcasting_sub", appLanguage)
                        } else {
                            AppStrings.get("listening_sub", appLanguage)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onToggleBroadcasting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBroadcasting) TacticalAlertRed else MaterialTheme.colorScheme.onBackground,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("mesh_toggle_broadcast_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBroadcasting) AppStrings.get("tap_to_listen", appLanguage) else AppStrings.get("tap_to_broadcast", appLanguage),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        }

        // 2. PRIVATE GROUP MANAGEMENT CARD (Join PIN or Scan QR)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentGroup != null) Icons.Default.Lock else Icons.Default.Group,
                            contentDescription = null,
                            tint = if (currentGroup != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentGroup != null) "PRIVATE GROUP: ${currentGroup.name.uppercase()}" else "PRIVATE GROUP // ENCRYPTED MESH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (currentGroup != null) {
                        Text(
                            text = "PIN: ${currentGroup.pin} • ${currentGroup.members.size} Member(s) active. Location shared only in this group.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showQrDialog = true },
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.height(32.dp).testTag("show_group_qr_mesh_btn")
                            ) {
                                Icon(Icons.Default.QrCode, "QR", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SHOW QR", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                            }
                            OutlinedButton(
                                onClick = onLeaveGroup,
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.height(32.dp).testTag("leave_group_mesh_btn")
                            ) {
                                Text("LEAVE GROUP", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                            }
                        }
                    } else {
                        Text(
                            text = "Add peers by QR code or secure PIN. Only group members can view positions and private chat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showJoinDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.height(34.dp).testTag("join_pin_btn")
                            ) {
                                Text(AppStrings.get("join_group", appLanguage).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                            }
                            OutlinedButton(
                                onClick = { showCreateDialog = true },
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.height(34.dp).testTag("create_group_btn")
                            ) {
                                Text(AppStrings.get("create_group", appLanguage).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace))
                            }
                        }
                    }
                }
            }
        }

        // 3. SCAN NEARBY PEERS HEADER & CONTROLS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WifiTethering, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PEERS DETECTED NEARBY (${peers.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Button(
                    onClick = onScanPeers,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.height(32.dp).testTag("scan_peers_button")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SCANNING...", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace))
                    } else {
                        Icon(Icons.Default.Refresh, "Scan", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SCAN PEERS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // 4. DETECTED PEERS LIST
        if (peers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Radar, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = AppStrings.get("no_peers", appLanguage),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tap 'SCAN PEERS' to discover nearby Wi-Fi Direct and BLE nodes.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        } else {
            items(peers, key = { it.id }) { peer ->
                PeerCard(
                    peer = peer,
                    onViewOnMap = onNavigateToMap,
                    onSendMessage = onNavigateToChat
                )
            }
        }
    }

    // Dialogs
    if (showJoinDialog) {
        JoinGroupDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { pin, name ->
                onJoinGroup(pin, name)
                showJoinDialog = false
            }
        )
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, pin ->
                onCreateGroup(name, pin)
                showCreateDialog = false
            }
        )
    }

    if (showQrDialog && currentGroup != null) {
        GroupQrCodeDialog(
            group = currentGroup,
            onDismiss = { showQrDialog = false }
        )
    }
}

@Composable
private fun PeerCard(
    peer: MeshNode,
    onViewOnMap: () -> Unit,
    onSendMessage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (peer.isBroadcastingSos) 1.5.dp else 1.dp,
                color = if (peer.isBroadcastingSos) TacticalAlertRed else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(2.dp)
            )
            .background(
                if (peer.isBroadcastingSos) TacticalAlertRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            )
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .background(
                            if (peer.isBroadcastingSos) TacticalAlertRed else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (peer.isBroadcastingSos) Icons.Default.Emergency else Icons.Default.Hub,
                        contentDescription = null,
                        tint = if (peer.isBroadcastingSos) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = peer.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (peer.isBroadcastingSos) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(TacticalAlertRed, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SOS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 8.sp,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = "ID: ${peer.id} • ${peer.role.name} • ${peer.lastPingAgo}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }

                Text(
                    text = "${peer.distanceKm} km",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Peer Telemetry Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryStd, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${peer.batteryPercent}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(Icons.Default.SignalCellularAlt, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${peer.rssiDbm} dBm",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (peer.speedKmh > 0f) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${peer.speedKmh} km/h",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onViewOnMap,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Map, "Map", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MAP", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                    }
                    Button(
                        onClick = onSendMessage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("CHAT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace))
                    }
                }
            }
        }
    }
}
