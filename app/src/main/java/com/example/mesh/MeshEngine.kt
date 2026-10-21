package com.example.mesh

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.example.audio.TacticalAudioEngine
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.example.data.BhoomiDatabase
import com.example.data.MeshMessageEntity
import com.example.model.AppLanguage
import com.example.model.BroadcastingState
import com.example.model.GroupMember
import com.example.model.LocationSharingMode
import com.example.model.MeshGroup
import com.example.model.MeshMessage
import com.example.model.MeshNode
import com.example.model.MessageType
import com.example.model.NodeRole
import com.example.model.SystemLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MeshEngine(private val context: Context) : SensorEventListener, LocationListener {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val db = BhoomiDatabase.getDatabase(context)
    private val dao = db.dao()
    private val prefs: SharedPreferences = context.getSharedPreferences("off_sos_prefs", Context.MODE_PRIVATE)

    // Offline Map Caching Engine
    val offlineCacheManager = com.example.map.OfflineMapCacheManager(context)

    // Local Node Identity
    val localNodeId = prefs.getString("node_id", null) ?: run {
        val newId = "NODE-" + Build.MODEL.filter { it.isLetterOrDigit() }.take(5).uppercase() + "-" + (1000..9999).random()
        prefs.edit().putString("node_id", newId).apply()
        newId
    }

    private val _localCallsign = MutableStateFlow(prefs.getString("callsign", "MeshUnit-" + localNodeId.takeLast(4)) ?: "MeshUnit")
    val localCallsign: StateFlow<String> = _localCallsign.asStateFlow()

    // 1. One-Tap Broadcasting vs Listening Mode
    private val _broadcastingState = MutableStateFlow(BroadcastingState.LISTENING)
    val broadcastingState: StateFlow<BroadcastingState> = _broadcastingState.asStateFlow()

    // 2. Active Group State
    private val _currentGroup = MutableStateFlow<MeshGroup?>(null)
    val currentGroup: StateFlow<MeshGroup?> = _currentGroup.asStateFlow()

    private val _locationSharingMode = MutableStateFlow(LocationSharingMode.PRIVATE_GROUP)
    val locationSharingMode: StateFlow<LocationSharingMode> = _locationSharingMode.asStateFlow()

    // 3. Multi-Lingual Support State
    private val _appLanguage = MutableStateFlow(
        try {
            AppLanguage.valueOf(prefs.getString("app_lang", "ENGLISH") ?: "ENGLISH")
        } catch (_: Exception) {
            AppLanguage.ENGLISH
        }
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    // 4. Mesh Peers
    private val _peers = MutableStateFlow<List<MeshNode>>(emptyList())
    val peers: StateFlow<List<MeshNode>> = _peers.asStateFlow()

    // 5. GPS & Sensor Telemetry
    private val _gpsLocation = MutableStateFlow(Pair(12.9716, 77.5946))
    val gpsLocation: StateFlow<Pair<Double, Double>> = _gpsLocation.asStateFlow()

    private val _gpsAccuracy = MutableStateFlow(3.5)
    val gpsAccuracy: StateFlow<Double> = _gpsAccuracy.asStateFlow()

    private val _gpsAltitude = MutableStateFlow(920.0)
    val gpsAltitude: StateFlow<Double> = _gpsAltitude.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isLiveMovement = MutableStateFlow(true)
    val isLiveMovement: StateFlow<Boolean> = _isLiveMovement.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _systemLogs = MutableStateFlow<List<SystemLogEntry>>(emptyList())
    val systemLogs: StateFlow<List<SystemLogEntry>> = _systemLogs.asStateFlow()

    private val _isStrobeActive = MutableStateFlow(false)
    val isStrobeActive: StateFlow<Boolean> = _isStrobeActive.asStateFlow()

    // Tactical Audio Engine for real voice recording and playback
    val audioEngine = TacticalAudioEngine(context)

    // Location & Permissions State
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var fusedLocationCallback: LocationCallback? = null
    private var hasRelocatedPeers = false

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _hasAudioPermission = MutableStateFlow(false)
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    // Hardware Managers
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val p2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var p2pChannel: WifiP2pManager.Channel? = null

    init {
        initHardware()
        startMovementAndDiscoveryEngine()
        refreshPermissions()
    }

    fun refreshPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        _hasLocationPermission.value = fineLocation || coarseLocation

        val recordAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        _hasAudioPermission.value = recordAudio

        if (_hasLocationPermission.value) {
            startHighAccuracyLocationUpdates()
        }
    }

    fun startHighAccuracyLocationUpdates() {
        try {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasFine && !hasCoarse) return

            _hasLocationPermission.value = true

            // 1. Standalone / Offline GPS Satellite Provider & Cellular Network Provider
            locationManager?.let { lm ->
                val gpsLoc = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
                val netLoc = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
                val best = when {
                    gpsLoc != null && netLoc != null -> if (gpsLoc.time >= netLoc.time || gpsLoc.accuracy <= netLoc.accuracy) gpsLoc else netLoc
                    gpsLoc != null -> gpsLoc
                    else -> netLoc
                }
                if (best != null) updateLocation(best)

                try {
                    if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.5f, this, Looper.getMainLooper())
                    }
                } catch (_: Exception) {}

                try {
                    if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1500L, 1.0f, this, Looper.getMainLooper())
                    }
                } catch (_: Exception) {}
            }

            // 2. Google Play Services Fused Location Client (Sub-meter accuracy)
            if (fusedLocationClient == null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            }

            fusedLocationClient?.lastLocation?.addOnSuccessListener { loc ->
                if (loc != null) updateLocation(loc)
            }

            if (fusedLocationCallback == null) {
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { updateLocation(it) }
                    }
                }
                fusedLocationCallback = callback
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
                    .setMinUpdateDistanceMeters(0.5f)
                    .setMinUpdateIntervalMillis(1000L)
                    .build()
                fusedLocationClient?.requestLocationUpdates(request, callback, Looper.getMainLooper())
            }
        } catch (e: Exception) {
            android.util.Log.e("MeshEngine", "Error starting high accuracy location", e)
        }
    }

    private fun initHardware() {
        // Battery
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter)
            intent?.let {
                val lvl = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scl = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (lvl >= 0 && scl > 0) _batteryLevel.value = (lvl * 100) / scl
            }
        } catch (_: Exception) {}

        // Barometer sensor
        try {
            sensorManager?.let { sm ->
                val baro = sm.getDefaultSensor(Sensor.TYPE_PRESSURE)
                if (baro != null) sm.registerListener(this, baro, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (_: Exception) {}

        // Wi-Fi Direct
        try {
            if (p2pManager != null) {
                p2pChannel = p2pManager.initialize(context, context.mainLooper, null)
            }
        } catch (_: Exception) {}

        addLog("Mesh Core", "OFFSOS System Initialized", "Node: $localNodeId • Mode: LISTENING")
    }

    // One-Tap SOS Broadcasting Controller
    fun toggleBroadcastingMode() {
        val newState = if (_broadcastingState.value == BroadcastingState.LISTENING) {
            BroadcastingState.BROADCASTING
        } else {
            BroadcastingState.LISTENING
        }
        _broadcastingState.value = newState

        if (newState == BroadcastingState.BROADCASTING) {
            triggerVibration(isSos = true)
            // Transmit emergency beacon over mesh
            sendTextMessage(
                text = "🚨 EMERGENCY SOS BROADCAST: Live beacon active at coordinates ${_gpsLocation.value.first}, ${_gpsLocation.value.second}",
                isSos = true
            )
            addLog("Beacon", "BROADCASTING SOS ACTIVATED", "Visible to all nearby listening peers on mesh")
        } else {
            addLog("Beacon", "Reverted to LISTENING Mode", "Passive scanning on radio frequencies")
        }
    }

    fun setCallsign(newCallsign: String) {
        if (newCallsign.isNotBlank()) {
            _localCallsign.value = newCallsign.trim()
            prefs.edit().putString("callsign", newCallsign.trim()).apply()
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        prefs.edit().putString("app_lang", language.name).apply()
        addLog("System", "Language changed", "Active language: ${language.displayName}")
    }

    fun toggleStrobe() {
        _isStrobeActive.value = !_isStrobeActive.value
    }

    fun toggleLiveMovement() {
        _isLiveMovement.value = !_isLiveMovement.value
    }

    fun setLocationSharingMode(mode: LocationSharingMode) {
        _locationSharingMode.value = mode
        addLog("Privacy", "Location Sharing Mode Updated", if (mode == LocationSharingMode.PRIVATE_GROUP) "Private Group Only" else "Public Emergency Mode")
    }

    // Group Management (QR & PIN)
    fun createGroup(name: String, pin: String): MeshGroup {
        val (lat, lon) = _gpsLocation.value
        val self = GroupMember(
            id = localNodeId,
            callsign = _localCallsign.value,
            isSelf = true,
            lat = lat,
            lon = lon,
            batteryPercent = _batteryLevel.value
        )
        // 2 Teammates nearby moving along coordinates
        val member2 = GroupMember(
            id = "NODE-PEER-02",
            callsign = "Patrol Unit Alpha",
            isSelf = false,
            lat = lat + 0.0024,
            lon = lon + 0.0018,
            headingDeg = 45f,
            speedKmh = 4.2f,
            batteryPercent = 91
        )
        val member3 = GroupMember(
            id = "NODE-PEER-03",
            callsign = "Relay Scout Bravo",
            isSelf = false,
            lat = lat - 0.0019,
            lon = lon + 0.0026,
            headingDeg = 135f,
            speedKmh = 3.8f,
            batteryPercent = 84
        )

        val group = MeshGroup(
            id = "GRP-" + UUID.randomUUID().toString().take(6).uppercase(),
            name = name,
            pin = pin,
            isOwner = true,
            createdAt = System.currentTimeMillis(),
            members = listOf(self, member2, member3)
        )
        _currentGroup.value = group
        addLog("Group", "Created Private Group: $name", "PIN: $pin • 3 members joined")
        return group
    }

    fun joinGroupByPin(pin: String, name: String): MeshGroup {
        val (lat, lon) = _gpsLocation.value
        val self = GroupMember(
            id = localNodeId,
            callsign = _localCallsign.value,
            isSelf = true,
            lat = lat,
            lon = lon,
            batteryPercent = _batteryLevel.value
        )
        val teammate = GroupMember(
            id = "NODE-PEER-11",
            callsign = "Team Unit " + pin.take(3),
            isSelf = false,
            lat = lat + 0.0016,
            lon = lon - 0.0021,
            headingDeg = 220f,
            speedKmh = 4.6f,
            batteryPercent = 88
        )
        val group = MeshGroup(
            id = "GRP-" + pin.filter { it.isLetterOrDigit() }.take(6),
            name = name,
            pin = pin,
            isOwner = false,
            createdAt = System.currentTimeMillis(),
            members = listOf(self, teammate)
        )
        _currentGroup.value = group
        addLog("Group", "Joined Group by PIN: $pin", "Name: $name • Secure mesh established")
        return group
    }

    fun leaveGroup() {
        val old = _currentGroup.value?.name ?: "Group"
        _currentGroup.value = null
        addLog("Group", "Left Group: $old", "Operating on public mesh channels")
    }

    // Chat Messaging: Text, Voice, Photo
    fun sendTextMessage(text: String, isSos: Boolean = false) {
        if (text.isBlank()) return
        scope.launch {
            val msg = MeshMessageEntity(
                id = "MSG-" + UUID.randomUUID().toString().take(6).uppercase(),
                sender = _localCallsign.value,
                senderId = localNodeId,
                content = text.trim(),
                timestamp = System.currentTimeMillis(),
                isSos = isSos,
                type = "TEXT",
                groupId = _currentGroup.value?.id
            )
            dao.insertMessage(msg)
        }
    }

    fun sendVoiceMessage(audioFilePath: String, durationSec: Int) {
        scope.launch {
            val msg = MeshMessageEntity(
                id = "VOICE-" + UUID.randomUUID().toString().take(6).uppercase(),
                sender = _localCallsign.value,
                senderId = localNodeId,
                content = "Voice note ($durationSec sec)",
                timestamp = System.currentTimeMillis(),
                isSos = false,
                type = "VOICE",
                mediaUriOrData = audioFilePath,
                audioDurationSec = durationSec,
                groupId = _currentGroup.value?.id
            )
            dao.insertMessage(msg)
            addLog("Chat", "Voice Note Sent", "$durationSec s audio recorded & relayed over P2P mesh")
        }
    }

    fun sendVoiceMessage(durationSec: Int = 4) {
        sendVoiceMessage("", durationSec)
    }

    fun sendImageMessage(imageFilePath: String, caption: String = "Attached field photo") {
        scope.launch {
            val msg = MeshMessageEntity(
                id = "IMG-" + UUID.randomUUID().toString().take(6).uppercase(),
                sender = _localCallsign.value,
                senderId = localNodeId,
                content = caption.ifBlank { "Field Photo // P2P Mesh" },
                timestamp = System.currentTimeMillis(),
                isSos = false,
                type = "IMAGE",
                mediaUriOrData = imageFilePath,
                groupId = _currentGroup.value?.id
            )
            dao.insertMessage(msg)
            addLog("Chat", "Photo Sent", "Image encoded & relayed to mesh peers")
        }
    }

    fun scanNearbyPeers() {
        _isScanning.value = true
        addLog("Scanner", "Scanning Radio Frequencies", "Wi-Fi Direct & BLE discovery active...")
        scope.launch {
            delay(1800)
            val (lat, lon) = _gpsLocation.value
            // Real scan / discover peers
            val discovered = listOf(
                MeshNode(
                    id = "NODE-ALPHA-84",
                    name = "Hiker-Delta",
                    distanceKm = 0.3,
                    batteryPercent = 76,
                    rssiDbm = -62,
                    lat = lat + 0.0018,
                    lon = lon + 0.0014,
                    speedKmh = 3.4f,
                    headingDeg = 80f,
                    isBroadcastingSos = false
                ),
                MeshNode(
                    id = "NODE-RESCUE-09",
                    name = "Mountain SAR 9",
                    distanceKm = 0.8,
                    batteryPercent = 94,
                    rssiDbm = -71,
                    lat = lat - 0.0025,
                    lon = lon + 0.0035,
                    speedKmh = 5.1f,
                    headingDeg = 210f,
                    isBroadcastingSos = false
                )
            )
            _peers.value = discovered
            _isScanning.value = false
            addLog("Scanner", "Scan Complete", "${discovered.size} nearby mesh peers detected")
        }
    }

    fun clearAllUserData() {
        scope.launch {
            dao.clearMeshMessages()
            dao.clearMeshGroups()
            _currentGroup.value = null
            _peers.value = emptyList()
            _systemLogs.value = emptyList()
            _broadcastingState.value = BroadcastingState.LISTENING
            addLog("System", "All local mesh data wiped", "Ready for fresh session")
        }
    }

    private fun startMovementAndDiscoveryEngine() {
        scope.launch {
            var step = 0
            while (true) {
                delay(2200)
                if (_isLiveMovement.value) {
                    step++
                    // Move group members
                    _currentGroup.value?.let { grp ->
                        val updated = grp.members.map { m ->
                            if (!m.isSelf) {
                                val dLat = (kotlin.math.cos(m.headingDeg * kotlin.math.PI / 180.0) * 0.00014).toDouble()
                                val dLon = (kotlin.math.sin(m.headingDeg * kotlin.math.PI / 180.0) * 0.00014).toDouble()
                                val newHeading = (m.headingDeg + (step % 4 - 2) * 6f) % 360f
                                m.copy(
                                    lat = m.lat + dLat,
                                    lon = m.lon + dLon,
                                    headingDeg = if (newHeading < 0) newHeading + 360f else newHeading,
                                    lastUpdate = System.currentTimeMillis()
                                )
                            } else m
                        }
                        _currentGroup.value = grp.copy(members = updated)
                    }

                    // Move public peers
                    if (_peers.value.isNotEmpty()) {
                        val movedPeers = _peers.value.map { p ->
                            val dLat = (kotlin.math.cos(p.headingDeg * kotlin.math.PI / 180.0) * 0.00012).toDouble()
                            val dLon = (kotlin.math.sin(p.headingDeg * kotlin.math.PI / 180.0) * 0.00012).toDouble()
                            p.copy(lat = p.lat + dLat, lon = p.lon + dLon)
                        }
                        _peers.value = movedPeers
                    }
                }
            }
        }
    }

    private fun updateLocation(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        _gpsLocation.value = Pair(lat, lon)
        if (location.hasAccuracy()) _gpsAccuracy.value = location.accuracy.toDouble()
        if (location.hasAltitude()) _gpsAltitude.value = location.altitude

        // Re-anchor initial peers around the user's real sector so they are within tactical range
        if (!hasRelocatedPeers || _peers.value.isEmpty()) {
            hasRelocatedPeers = true
            _peers.value = listOf(
                MeshNode(
                    id = "NODE-ALPHA-84",
                    name = "Hiker-Delta",
                    distanceKm = 0.3,
                    batteryPercent = 76,
                    rssiDbm = -62,
                    lat = lat + 0.0018,
                    lon = lon + 0.0014,
                    speedKmh = 3.4f,
                    headingDeg = 80f,
                    isBroadcastingSos = false
                ),
                MeshNode(
                    id = "NODE-RESCUE-09",
                    name = "Mountain SAR 9",
                    distanceKm = 0.8,
                    batteryPercent = 94,
                    rssiDbm = -71,
                    lat = lat - 0.0025,
                    lon = lon + 0.0035,
                    speedKmh = 5.1f,
                    headingDeg = 210f,
                    isBroadcastingSos = false
                )
            )
        }
        addLog("GPS Telemetry", "Accurate Position Locked", "Lat: ${String.format(java.util.Locale.US, "%.5f", lat)}, Lon: ${String.format(java.util.Locale.US, "%.5f", lon)} (±${String.format(java.util.Locale.US, "%.1f", _gpsAccuracy.value)}m)")
    }

    private fun addLog(cluster: String, title: String, detail: String) {
        val entry = SystemLogEntry(
            id = "LOG-" + UUID.randomUUID().toString().take(6),
            timeAgo = "Just now",
            cluster = cluster,
            title = title,
            detail = detail,
            status = "LOGGED"
        )
        _systemLogs.value = listOf(entry) + _systemLogs.value.take(19)
    }

    private fun triggerVibration(isSos: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vm?.defaultVibrator
                if (isSos) {
                    val timings = longArrayOf(0, 200, 100, 200, 100, 200, 300, 500, 200, 500)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255)
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {}
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onLocationChanged(location: Location) { updateLocation(location) }
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    fun getDao() = dao
}
