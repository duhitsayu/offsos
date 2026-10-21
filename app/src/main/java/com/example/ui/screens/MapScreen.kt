package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.OfflineMapCacheManager
import com.example.model.GroupMember
import com.example.model.LocationSharingMode
import com.example.model.MeshGroup
import com.example.model.MeshNode
import com.example.ui.components.CreateGroupDialog
import com.example.ui.components.GroupQrCodeDialog
import com.example.ui.components.JoinGroupDialog
import com.example.ui.components.MapTileLayer
import com.example.ui.components.OfflineMapCacheDialog
import com.example.ui.components.OpenStreetMapView
import com.example.ui.theme.TacticalAlertRed
import com.example.ui.theme.TacticalAmber

@Composable
fun MapScreen(
    peers: List<MeshNode>,
    currentGroup: MeshGroup?,
    locationSharingMode: LocationSharingMode,
    isLiveMovementActive: Boolean,
    userLocation: Pair<Double, Double>,
    gpsAccuracy: Double = 3.5,
    offlineCacheManager: OfflineMapCacheManager,
    onToggleLiveMovement: () -> Unit,
    onCreateGroup: (name: String, pin: String) -> Unit,
    onJoinGroup: (pin: String, name: String) -> Unit,
    onLeaveGroup: () -> Unit,
    onToggleLocationSharingMode: (LocationSharingMode) -> Unit,
    onSendPing: (MeshNode) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTileLayer by remember { mutableStateOf(MapTileLayer.OSM_STANDARD) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    var selectedPeerId by remember { mutableStateOf<String?>(null) }
    var showLayerSelector by remember { mutableStateOf(false) }
    var recenterTrigger by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    val cacheState by offlineCacheManager.state.collectAsState()

    val isPublicEmergency = locationSharingMode == LocationSharingMode.PUBLIC_EMERGENCY
    val groupMembers = currentGroup?.members ?: emptyList()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Real OpenStreetMap Leaflet View with Offline Interception
        OpenStreetMapView(
            userLocation = userLocation,
            peers = peers,
            groupMembers = groupMembers,
            isPublicEmergency = isPublicEmergency,
            offlineCacheManager = offlineCacheManager,
            currentTileLayer = currentTileLayer,
            selectedPeerId = selectedPeerId,
            recenterTrigger = recenterTrigger,
            onPeerSelected = { selectedPeerId = it },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Group Privacy & Location Sharing Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, Color(0xFFFFFFFF), RoundedCornerShape(2.dp))
                    .background(Color(0xFF000000).copy(alpha = 0.95f))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentGroup != null) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private Group",
                                tint = Color(0xFFFFFFFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentGroup.name.uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color(0xFFFFFFFF)
                                )
                                Text(
                                    text = "PIN: ${currentGroup.pin} • ${groupMembers.size} Member(s) in Mesh",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color(0xFFAAAAAA)
                                )
                            }

                            // Show QR Code Button
                            IconButton(
                                onClick = { showQrDialog = true },
                                modifier = Modifier.size(32.dp).testTag("show_group_qr_btn")
                            ) {
                                Icon(Icons.Default.QrCode, "Show Group QR", tint = Color(0xFFFFFFFF))
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Leave Group Button
                            OutlinedButton(
                                onClick = onLeaveGroup,
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.height(28.dp).testTag("leave_group_btn")
                            ) {
                                Text("LEAVE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color(0xFFFFFFFF))
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = "Public Emergency",
                                tint = TacticalAlertRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PUBLIC EMERGENCY MODE",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color(0xFFFFFFFF)
                                )
                                Text(
                                    text = "Not in a group. Coordinates shared publicly for rescue.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFAAAAAA)
                                )
                            }

                            OutlinedButton(
                                onClick = { showJoinDialog = true },
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.height(30.dp).testTag("join_group_btn")
                            ) {
                                Text("JOIN PIN", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = Color(0xFFFFFFFF))
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Button(
                                onClick = { showCreateDialog = true },
                                shape = RoundedCornerShape(2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFFFFF),
                                    contentColor = Color(0xFF000000)
                                ),
                                modifier = Modifier.height(30.dp).testTag("create_group_btn")
                            ) {
                                Text("+ CREATE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location Privacy Guarantee Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentGroup != null) "🔒 Private Mesh: Only ${groupMembers.size} verified member(s) track you" else "🚨 Public Mesh: Coordinates open to all nearby responders",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = if (currentGroup != null) Color(0xFFFFFFFF) else TacticalAlertRed
                        )

                        // Live Movement Toggle Button
                        Row(
                            modifier = Modifier
                                .border(1.dp, Color(0xFF555555), RoundedCornerShape(2.dp))
                                .clickable { onToggleLiveMovement() }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isLiveMovementActive) Icons.Default.PlayArrow else Icons.Default.Stop,
                                contentDescription = null,
                                tint = Color(0xFFFFFFFF),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLiveMovementActive) "LIVE MOVEMENT ON" else "MOVEMENT PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }
                }
            }
        }

        // 3. Floating Tactical Bar (Top Right): Offline Map Cache, GPS Recenter & Layer Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 115.dp, end = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // My GPS Location Recenter Button
            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = { recenterTrigger++ },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF000000), RoundedCornerShape(2.dp))
                        .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(2.dp))
                        .testTag("recenter_gps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Recenter on My Location",
                        tint = Color(0xFFFFFFFF)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF000000).copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                        .border(1.dp, Color(0xFF444444), RoundedCornerShape(2.dp))
                        .clickable { recenterTrigger++ }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "±${String.format(java.util.Locale.US, "%.1f", gpsAccuracy)}m GPS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        ),
                        color = Color(0xFFFFFFFF)
                    )
                }
            }
            // Offline Cache Manager Button
            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = { showCacheDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF000000), RoundedCornerShape(2.dp))
                        .border(1.dp, if (cacheState.tileCount > 0 || cacheState.isOfflineSimulated) Color(0xFFFFFFFF) else Color(0xFF666666), RoundedCornerShape(2.dp))
                        .testTag("offline_cache_button")
                ) {
                    Icon(
                        imageVector = if (cacheState.isOfflineSimulated) Icons.Default.Flight else Icons.Default.CloudDownload,
                        contentDescription = "Offline Map Cache",
                        tint = Color(0xFFFFFFFF)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF000000).copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                        .border(1.dp, Color(0xFF444444), RoundedCornerShape(2.dp))
                        .clickable { showCacheDialog = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (cacheState.isOfflineSimulated) "OFFLINE MODE" else if (cacheState.tileCount > 0) "${cacheState.tileCount} TILES" else "CACHE MAP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        ),
                        color = Color(0xFFFFFFFF)
                    )
                }
            }

            // Map Layer Selector Button
            IconButton(
                onClick = { showLayerSelector = !showLayerSelector },
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFF000000), RoundedCornerShape(2.dp))
                    .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(2.dp))
                    .testTag("map_layers_button")
            ) {
                Icon(Icons.Default.Layers, "Change Map Layer", tint = Color(0xFFFFFFFF))
            }

            if (showLayerSelector) {
                Column(
                    modifier = Modifier
                        .background(Color(0xFF000000), RoundedCornerShape(2.dp))
                        .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(2.dp))
                        .padding(4.dp)
                ) {
                    MapTileLayer.values().forEach { layer ->
                        val isSelected = currentTileLayer == layer
                        Text(
                            text = layer.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 9.sp
                            ),
                            color = if (isSelected) Color(0xFF000000) else Color(0xFFFFFFFF),
                            modifier = Modifier
                                .background(if (isSelected) Color(0xFFFFFFFF) else Color.Transparent, RoundedCornerShape(2.dp))
                                .clickable {
                                    currentTileLayer = layer
                                    showLayerSelector = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 4. Bottom Active Peer Count Badge & Offline Notice
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(2.dp))
                    .background(Color(0xFF000000).copy(alpha = 0.9f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (currentGroup != null) "OPENSTREETMAP • ${groupMembers.size} GROUP PEERS MOVING" else "OPENSTREETMAP • ${peers.size} MESH PEERS ONLINE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFFFFFFF)
                )
            }

            if (cacheState.tileCount > 0 || cacheState.isOfflineSimulated) {
                Box(
                    modifier = Modifier
                        .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(2.dp))
                        .background(Color(0xFF000000).copy(alpha = 0.9f))
                        .clickable { showCacheDialog = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (cacheState.isOfflineSimulated) "✈ OFFLINE SIMULATION" else "✓ LOCAL CACHE ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFFFFFFF)
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCacheDialog) {
        OfflineMapCacheDialog(
            state = cacheState,
            userLocation = userLocation,
            onDownloadCache = { lat, lon ->
                offlineCacheManager.startLocalAreaDownload(lat, lon)
            },
            onCancelDownload = { offlineCacheManager.cancelDownload() },
            onClearCache = { offlineCacheManager.clearCache() },
            onToggleOfflineSimulation = { offlineCacheManager.toggleOfflineSimulation(it) },
            onDismiss = { showCacheDialog = false }
        )
    }

    if (showQrDialog && currentGroup != null) {
        GroupQrCodeDialog(
            group = currentGroup,
            onDismiss = { showQrDialog = false },
            onCopyPinNotice = { }
        )
    }

    if (showJoinDialog) {
        JoinGroupDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { pin, name ->
                onJoinGroup(pin, name)
            }
        )
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, pin ->
                onCreateGroup(name, pin)
                showQrDialog = true
            }
        )
    }
}
