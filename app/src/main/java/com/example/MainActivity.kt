package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mesh.MeshEngine
import com.example.model.AppStrings
import com.example.model.BroadcastingState
import com.example.model.LocationSharingMode
import com.example.model.MessageType
import com.example.ui.components.OffSosBottomBar
import com.example.ui.components.OffSosNavDestination
import com.example.ui.components.OffSosTopBar
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.MeshScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var meshEngine: MeshEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        meshEngine = MeshEngine(this)
        enableEdgeToEdge()

        setContent {
            var isNightVision by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = isNightVision) {
                OffSosApp(
                    meshEngine = meshEngine,
                    isNightVision = isNightVision,
                    onToggleNightVision = { isNightVision = !isNightVision }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::meshEngine.isInitialized) {
            meshEngine.refreshPermissions()
        }
    }
}

@Composable
fun OffSosApp(
    meshEngine: MeshEngine,
    isNightVision: Boolean,
    onToggleNightVision: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDestination by remember { mutableStateOf(OffSosNavDestination.MESH) }

    // List of runtime permissions required for tactical mesh operation
    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        meshEngine.refreshPermissions()
    }

    // Automatically prompt for tactical permissions on app launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    // Collect MeshEngine states
    val peers by meshEngine.peers.collectAsState()
    val broadcastingState by meshEngine.broadcastingState.collectAsState()
    val currentGroup by meshEngine.currentGroup.collectAsState()
    val locationSharingMode by meshEngine.locationSharingMode.collectAsState()
    val appLanguage by meshEngine.appLanguage.collectAsState()
    val localCallsign by meshEngine.localCallsign.collectAsState()
    val gpsLocation by meshEngine.gpsLocation.collectAsState()
    val gpsAccuracy by meshEngine.gpsAccuracy.collectAsState()
    val gpsAltitude by meshEngine.gpsAltitude.collectAsState()
    val batteryLevel by meshEngine.batteryLevel.collectAsState()
    val isLiveMovementActive by meshEngine.isLiveMovement.collectAsState()
    val isScanning by meshEngine.isScanning.collectAsState()
    val hasLocationPermission by meshEngine.hasLocationPermission.collectAsState()
    val hasAudioPermission by meshEngine.hasAudioPermission.collectAsState()

    // Database flow for messages
    val messageEntities by meshEngine.getDao().getAllMessages().collectAsState(initial = emptyList())
    val messages = messageEntities.map { entity ->
        com.example.model.MeshMessage(
            id = entity.id,
            sender = entity.sender,
            senderId = entity.senderId,
            content = entity.content,
            timestamp = entity.timestamp,
            type = try { MessageType.valueOf(entity.type) } catch (_: Exception) { MessageType.TEXT },
            mediaUriOrData = entity.mediaUriOrData,
            audioDurationSec = entity.audioDurationSec,
            isSos = entity.isSos,
            groupId = entity.groupId,
            hops = entity.hops
        )
    }

    // Handle back button on sub-screens
    if (currentDestination != OffSosNavDestination.MESH) {
        BackHandler {
            currentDestination = OffSosNavDestination.MESH
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            OffSosTopBar(
                screenTitle = currentDestination.label,
                broadcastingState = broadcastingState,
                appLanguage = appLanguage,
                isNightVision = isNightVision,
                onToggleNightVision = onToggleNightVision,
                onProfileClick = { currentDestination = OffSosNavDestination.PROFILE },
                onToggleBroadcasting = {
                    meshEngine.toggleBroadcastingMode()
                    val isBroadcastingNow = broadcastingState != BroadcastingState.BROADCASTING
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (isBroadcastingNow) "🚨 EMERGENCY SOS BROADCAST ACTIVE: P2P Beacon live"
                            else "Broadcasting stopped. Returned to LISTENING mode."
                        )
                    }
                }
            )
        },
        bottomBar = {
            OffSosBottomBar(
                currentDestination = currentDestination,
                broadcastingState = broadcastingState,
                onNavigate = { destination ->
                    currentDestination = destination
                },
                onToggleBroadcasting = {
                    meshEngine.toggleBroadcastingMode()
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Status Alert Banner if critical sensor permissions are not granted
            if (!hasLocationPermission || !hasAudioPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF000000))
                        .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(0.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFFFFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SENSORS RESTRICTED // GPS & AUDIO OFFLINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = Color(0xFFFFFFFF)
                            )
                        }
                        Button(
                            onClick = {
                                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                            },
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFFFFF),
                                contentColor = Color(0xFF000000)
                            ),
                            modifier = Modifier.height(28.dp).testTag("grant_permissions_btn")
                        ) {
                            Text(
                                text = "AUTHORIZE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentDestination) {
                    OffSosNavDestination.MESH -> {
                        MeshScreen(
                            peers = peers,
                            localNodeId = meshEngine.localNodeId,
                            localCallsign = localCallsign,
                            broadcastingState = broadcastingState,
                            currentGroup = currentGroup,
                            isScanning = isScanning,
                            appLanguage = appLanguage,
                            onToggleBroadcasting = {
                                meshEngine.toggleBroadcastingMode()
                                val isBroadcastingNow = broadcastingState != BroadcastingState.BROADCASTING
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (isBroadcastingNow) "🚨 EMERGENCY SOS BROADCAST ACTIVE: P2P Beacon live"
                                        else "Broadcasting stopped. Returned to LISTENING mode."
                                    )
                                }
                            },
                            onScanPeers = {
                                meshEngine.scanNearbyPeers()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Scanning Wi-Fi Direct and BLE mesh frequencies...")
                                }
                            },
                            onCreateGroup = { name, pin ->
                                meshEngine.createGroup(name, pin)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Created private mesh circle: $name")
                                }
                            },
                            onJoinGroup = { pin, name ->
                                meshEngine.joinGroupByPin(pin, name)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Joined Group by PIN: $pin")
                                }
                            },
                            onLeaveGroup = {
                                meshEngine.leaveGroup()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Left private group. Operating on Public Mesh.")
                                }
                            },
                            onNavigateToMap = { currentDestination = OffSosNavDestination.MAP },
                            onNavigateToChat = { currentDestination = OffSosNavDestination.CHAT }
                        )
                    }

                    OffSosNavDestination.MAP -> {
                        MapScreen(
                            peers = peers,
                            currentGroup = currentGroup,
                            locationSharingMode = locationSharingMode,
                            isLiveMovementActive = isLiveMovementActive,
                            userLocation = gpsLocation,
                            gpsAccuracy = gpsAccuracy,
                            offlineCacheManager = meshEngine.offlineCacheManager,
                            onToggleLiveMovement = { meshEngine.toggleLiveMovement() },
                            onCreateGroup = { name, pin ->
                                meshEngine.createGroup(name, pin)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Created private group $name (PIN: $pin)")
                                }
                            },
                            onJoinGroup = { pin, name ->
                                meshEngine.joinGroupByPin(pin, name)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Joined group (PIN: $pin). Location shared privately.")
                                }
                            },
                            onLeaveGroup = {
                                meshEngine.leaveGroup()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Left group. Reverted to Public Emergency location mode.")
                                }
                            },
                            onToggleLocationSharingMode = { mode ->
                                meshEngine.setLocationSharingMode(mode)
                            },
                            onSendPing = { peer ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("P2P Mesh Ping sent to ${peer.name}")
                                }
                            }
                        )
                    }

                    OffSosNavDestination.CHAT -> {
                        ChatScreen(
                            currentGroup = currentGroup,
                            messages = messages,
                            localNodeId = meshEngine.localNodeId,
                            appLanguage = appLanguage,
                            audioEngine = meshEngine.audioEngine,
                            hasAudioPermission = hasAudioPermission,
                            onRequestAudioPermission = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                            },
                            onSendMessage = { text ->
                                meshEngine.sendTextMessage(text, isSos = broadcastingState == BroadcastingState.BROADCASTING)
                            },
                            onSendVoiceMessage = { filePath, durationSec ->
                                meshEngine.sendVoiceMessage(filePath, durationSec)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Voice note ($durationSec s) recorded & relayed over P2P mesh")
                                }
                            },
                            onSendImageMessage = { filePath, caption ->
                                meshEngine.sendImageMessage(filePath, caption)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Field recon photo encoded and sent to mesh peers")
                                }
                            }
                        )
                    }

                    OffSosNavDestination.PROFILE -> {
                        ProfileScreen(
                            localNodeId = meshEngine.localNodeId,
                            localCallsign = localCallsign,
                            batteryPercent = batteryLevel,
                            gpsCoordinates = gpsLocation,
                            gpsAltitude = gpsAltitude,
                            currentGroup = currentGroup,
                            locationSharingMode = locationSharingMode,
                            currentLanguage = appLanguage,
                            onUpdateCallsign = { newCallsign ->
                                meshEngine.setCallsign(newCallsign)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Callsign updated to: $newCallsign")
                                }
                            },
                            onSelectLanguage = { language ->
                                meshEngine.setAppLanguage(language)
                            },
                            onToggleLocationSharingMode = { mode ->
                                meshEngine.setLocationSharingMode(mode)
                            },
                            onClearAllData = {
                                meshEngine.clearAllUserData()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("All local mesh messages and groups wiped.")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
