package com.vaav.offsos

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.preference.PreferenceManager
import android.text.method.ScrollingMovementMethod
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.collection.SimpleArrayMap
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import com.vaav.offsos.ui.SettingsBottomSheet
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity(), SettingsBottomSheet.SettingsListener {

    // --- UI ELEMENTS ---
    private lateinit var btnSos: View
    private lateinit var tvSosText: TextView
    private lateinit var tvStatus: TextView
    private lateinit var map: MapView
    private lateinit var cardSos: CardView

    // Settings Menu
    private lateinit var btnMenu: View

    // Tools
    private lateinit var btnDanger: View
    private lateinit var btnStopAlarm: View

    // Chat
    private lateinit var btnToggleChat: View
    private lateinit var btnChatClose: View
    private lateinit var layoutChat: CardView
    private lateinit var tvChatLog: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: View
    private lateinit var btnPlayAudio: View
    private lateinit var btnMic: View
    private lateinit var tvMic: TextView

    // --- LOGIC TOOLS ---
    private lateinit var connectionsClient: ConnectionsClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var lastAudioFile: File? = null
    private var lastAudioUri: android.net.Uri? = null
    private var isRecording = false
    private val incomingFilePayloads = SimpleArrayMap<Long, Payload>()
    private var connectionMediaPlayer: MediaPlayer? = null

    private var myMarker: Marker? = null
    private val activeMarkers = HashMap<String, Marker>()
    private val dangerZones = ArrayList<Polygon>()
    private var myLastLocation: Location? = null

    private val handler = Handler(Looper.getMainLooper())
    private val SERVICE_ID = "com.vaav.offsos"
    private var isBroadcasting = false
    private val connectedEndpoints = ArrayList<String>()
    private var isChatMode = false
    private var isStrobing = false

    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )
    }
    private val PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMDroid Config
        val ctx = applicationContext
        val config = Configuration.getInstance()
        config.load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        config.userAgentValue = packageName
        
        val basePath = File(ctx.cacheDir, "osmdroid")
        basePath.mkdirs()
        val tileCache = File(basePath, "tiles")
        tileCache.mkdirs()
        
        config.osmdroidBasePath = basePath
        config.osmdroidTileCache = tileCache

        setContentView(R.layout.activity_main)

        if (intent.getBooleanExtra("STOP_SIREN", false)) {
            val serviceIntent = Intent(this, SosService::class.java)
            stopService(serviceIntent)
            stopConnectionSiren()
        }

        initViews()
        initMap()
        createNotificationChannel()

        connectionsClient = Nearby.getConnectionsClient(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (hasPermissions(this)) {
            startDiscovery()
            updateMyLocation()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews() {
        btnSos = findViewById(R.id.btnSos)
        tvSosText = findViewById(R.id.tvSosText)
        cardSos = findViewById(R.id.cardSos)
        tvStatus = findViewById(R.id.tvStatus)
        map = findViewById(R.id.map)

        btnMenu = findViewById(R.id.btnMenu)

        btnToggleChat = findViewById(R.id.btnToggleChat)
        btnChatClose = findViewById(R.id.btnChatClose)
        layoutChat = findViewById(R.id.layoutChat)
        tvChatLog = findViewById(R.id.tvChatLog)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnPlayAudio = findViewById(R.id.btnPlayAudio)
        btnMic = findViewById(R.id.btnMic)
        tvMic = findViewById(R.id.tvMic)

        btnDanger = findViewById(R.id.btnDanger)
        btnStopAlarm = findViewById(R.id.btnStopAlarm)

        tvChatLog.movementMethod = ScrollingMovementMethod()

        btnMenu.setOnClickListener {
            val bottomSheet = SettingsBottomSheet()
            bottomSheet.listener = this
            bottomSheet.show(supportFragmentManager, SettingsBottomSheet.TAG)
        }

        btnStopAlarm.setOnClickListener {
            stopConnectionSiren()
            btnStopAlarm.visibility = View.GONE
            Toast.makeText(this, "Alarm Silenced", Toast.LENGTH_SHORT).show()
        }

        btnSos.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            it.playSoundEffect(SoundEffectConstants.CLICK)

            val sharedPrefs = getSharedPreferences("OFFSOS_PREFS", Context.MODE_PRIVATE)
            val name = sharedPrefs.getString("codename", "") ?: ""

            if (name.isEmpty()) {
                Toast.makeText(this, "Codename Required. Please set it in Settings.", Toast.LENGTH_LONG).show()
                val bottomSheet = SettingsBottomSheet()
                bottomSheet.listener = this
                bottomSheet.show(supportFragmentManager, SettingsBottomSheet.TAG)
                return@setOnClickListener
            }

            if (hasPermissions(this)) toggleSosMode() else requestPermissions()
        }

        btnToggleChat.setOnClickListener {
            isChatMode = !isChatMode
            if (isChatMode) {
                layoutChat.visibility = View.VISIBLE
                layoutChat.bringToFront()
            } else {
                layoutChat.visibility = View.GONE
            }
        }

        btnChatClose.setOnClickListener {
            layoutChat.visibility = View.GONE
            isChatMode = false
        }

        btnDanger.setOnClickListener { broadcastDangerZone() }
        btnDanger.setOnLongClickListener {
            broadcastSafeZone()
            true
        }

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString()
            if (msg.isNotEmpty() && connectedEndpoints.isNotEmpty()) {
                val sharedPrefs = getSharedPreferences("OFFSOS_PREFS", Context.MODE_PRIVATE)
                val cbPrivacyChecked = sharedPrefs.getBoolean("incognito_mode", false)
                val etNameText = sharedPrefs.getString("codename", "") ?: ""
                val myName = if (cbPrivacyChecked) "Signal-${(100..999).random()}" else etNameText
                appendChatLog("ME: $msg")
                sendPayloadToAll(Payload.fromBytes("MSG:$myName:$msg".toByteArray(StandardCharsets.UTF_8)))
                etMessage.setText("")
            } else {
                Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show()
            }
        }

        btnPlayAudio.setOnClickListener {
            if (lastAudioFile != null && lastAudioFile!!.exists()) {
                playReceivedAudio(lastAudioFile!!, null)
            } else if (lastAudioUri != null) {
                playReceivedAudio(null, lastAudioUri)
            } else {
                Toast.makeText(this, "No audio available", Toast.LENGTH_SHORT).show()
            }
        }

        btnMic.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    startRecording()
                    tvMic.text = "🔴 REC"
                    appendChatLog("* Recording... *")
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopRecordingAndSend()
                    tvMic.text = "🎙️\nTalk"
                    appendChatLog("* Audio Sent *")
                }
            }
            true
        }
    }

    // --- ALERTS & SIREN ---
    private fun triggerRescuerAlarm(endpointId: String) {
        try {
            if (connectionMediaPlayer == null) {
                val alertUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)

                connectionMediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, alertUri)
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        runOnUiThread {
            btnStopAlarm.visibility = View.VISIBLE
            btnStopAlarm.bringToFront()
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("STOP_SIREN", true)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "OFFSOS_bg_channel")
            .setContentTitle("✅ CONNECTED TO VICTIM!")
            .setContentText("Signal Locked: $endpointId. TAP TO STOP ALARM.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(888, notification)
    }

    private fun stopConnectionSiren() {
        connectionMediaPlayer?.stop()
        connectionMediaPlayer?.release()
        connectionMediaPlayer = null
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(888)

        runOnUiThread {
            if (::btnStopAlarm.isInitialized) btnStopAlarm.visibility = View.GONE
        }
    }

    // --- NON-APP TOOLS ---
    @SuppressLint("MissingPermission")
    private fun startDigitalFlare() {
        if (!hasPermissions(this)) return
        try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null) {
                bluetoothAdapter.name = "🚨 SOS! HELP ME! 🚨"
                val discoverableIntent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(android.bluetooth.BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                }
                startActivity(discoverableIntent)
                Toast.makeText(this, "Flare Active", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Flare Failed: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleStrobe() {
        if (isStrobing) {
            isStrobing = false
            Toast.makeText(this, "Strobe OFF", Toast.LENGTH_SHORT).show()
            return
        }
        isStrobing = true
        Toast.makeText(this, "Strobe ON (S-O-S)", Toast.LENGTH_SHORT).show()

        Thread {
            val camManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            try {
                val cameraId = camManager.cameraIdList[0]
                val morse = arrayOf(100L, 100L, 100L, 300L, 300L, 300L, 100L, 100L, 100L) // S-O-S

                while (isStrobing) {
                    for (duration in morse) {
                        if (!isStrobing) break
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) camManager.setTorchMode(cameraId, true)
                        Thread.sleep(duration)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) camManager.setTorchMode(cameraId, false)
                        Thread.sleep(200)
                    }
                    Thread.sleep(1000)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) camManager.setTorchMode(cameraId, false)
            } catch (e: Exception) { isStrobing = false }
        }.start()
    }

    // --- MAP TOOLS ---
    private fun downloadOfflineRegion() {
        if (myLastLocation == null) {
            Toast.makeText(this, "Waiting for Location...", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Downloading 10km Area...", Toast.LENGTH_LONG).show()
        val lat = myLastLocation!!.latitude
        val lon = myLastLocation!!.longitude
        val boundingBox = BoundingBox(lat + 0.1, lon + 0.1, lat - 0.1, lon - 0.1)
        val cacheManager = CacheManager(map)

        cacheManager.downloadAreaAsync(this, boundingBox, 10, 15, object : CacheManager.CacheManagerCallback {
            override fun onTaskComplete() { runOnUiThread { Toast.makeText(applicationContext, "Offline Map Saved!", Toast.LENGTH_LONG).show() } }
            override fun onTaskFailed(errors: Int) { runOnUiThread { Toast.makeText(applicationContext, "Download Failed. Error: $errors", Toast.LENGTH_SHORT).show() } }
            override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {}
            override fun downloadStarted() {}
            override fun setPossibleTilesInArea(total: Int) {}
        })
    }

    private fun broadcastDangerZone() {
        if (myLastLocation == null) return
        val lat = myLastLocation!!.latitude
        val lon = myLastLocation!!.longitude
        drawDangerZone(lat, lon)
        if (connectedEndpoints.isNotEmpty()) {
            val dangerPayload = "DANGER:$lat,$lon"
            sendPayloadToAll(Payload.fromBytes(dangerPayload.toByteArray(StandardCharsets.UTF_8)))
            Toast.makeText(this, "Danger Zone Broadcasted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Marked locally", Toast.LENGTH_SHORT).show()
        }
    }

    private fun broadcastSafeZone() {
        clearAllDangerZones()
        if (connectedEndpoints.isNotEmpty()) {
            val safePayload = "SAFE:ALL"
            sendPayloadToAll(Payload.fromBytes(safePayload.toByteArray(StandardCharsets.UTF_8)))
            Toast.makeText(this, "Cleared All Danger Zones!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawDangerZone(centerLat: Double, centerLon: Double) {
        val dangerPolygon = Polygon()
        val points = ArrayList<GeoPoint>()
        val radius = 0.005
        for (i in 0..360 step 10) {
            val t = Math.toRadians(i.toDouble())
            points.add(GeoPoint(centerLat + radius * Math.cos(t), centerLon + radius * Math.sin(t)))
        }
        dangerPolygon.points = points
        dangerPolygon.fillPaint.color = Color.parseColor("#40FF0000")
        dangerPolygon.outlinePaint.color = Color.RED
        dangerPolygon.outlinePaint.strokeWidth = 3f
        dangerPolygon.title = "DANGER ZONE"
        map.overlays.add(dangerPolygon)
        dangerZones.add(dangerPolygon)
        map.invalidate()
    }

    private fun clearAllDangerZones() {
        for (polygon in dangerZones) { map.overlays.remove(polygon) }
        dangerZones.clear()
        map.invalidate()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("OFFSOS_bg_channel", "OFFSOS Alerts", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // --- AUDIO ---
    private fun startRecording() {
        if (!hasPermissions(this)) return
        audioFile = File(cacheDir, "voice_msg.3gp")
        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun stopRecordingAndSend() {
        if (isRecording) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Exception) { e.printStackTrace() }
            mediaRecorder = null
            isRecording = false
            if (audioFile != null && audioFile!!.exists() && audioFile!!.length() > 0) {
                lastAudioFile = audioFile
                lastAudioUri = null
                runOnUiThread { btnPlayAudio.visibility = View.VISIBLE }
                if (connectedEndpoints.isNotEmpty()) {
                    try {
                        val pfd = ParcelFileDescriptor.open(audioFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        val filePayload = Payload.fromFile(pfd)
                        sendPayloadToAll(filePayload)
                    } catch (e: Exception) { }
                }
            } else {
                runOnUiThread { Toast.makeText(this, "Recording too short", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun playReceivedAudio(file: File?, uri: android.net.Uri?) {
        try {
            val mp = MediaPlayer()
            if (file != null) {
                val fis = FileInputStream(file)
                mp.setDataSource(fis.fd)
                mp.setOnCompletionListener { try { fis.close() } catch (e: Exception) {} }
            } else if (uri != null) {
                mp.setDataSource(applicationContext, uri)
            } else return
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mp.prepare()
            mp.start()
            appendChatLog("* Playing Audio... *")
        } catch (e: Exception) { 
            e.printStackTrace() 
            runOnUiThread { Toast.makeText(this, "Failed to play audio", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun initMap() {
        val tileSource = org.osmdroid.tileprovider.tilesource.XYTileSource(
            "Mapnik",
            0, 19, 256, ".png", arrayOf(
                "https://a.tile.openstreetmap.org/",
                "https://b.tile.openstreetmap.org/",
                "https://c.tile.openstreetmap.org/"
            )
        )
        map.setTileSource(tileSource)
        
        map.setMultiTouchControls(true)
        map.controller.setZoom(19.0)
        map.controller.setCenter(GeoPoint(28.6139, 77.2090))
    }

    private fun toggleSosMode() {
        if (isBroadcasting) {
            isBroadcasting = false
            tvSosText.text = "SOS"
            cardSos.setCardBackgroundColor(Color.parseColor("#B71C1C"))
            tvStatus.text = "🟢 ONLINE"
            tvStatus.setTextColor(Color.WHITE)
            stopAdvertising()
            startDiscovery()
        } else {
            isBroadcasting = true
            tvSosText.text = "STOP"
            cardSos.setCardBackgroundColor(Color.DKGRAY)
            tvStatus.text = "🔴 BROADCASTING"
            tvStatus.setTextColor(Color.RED)
            stopDiscovery()
            startAdvertising()
        }
    }

    private val sendLocationRunnable = object : Runnable {
        override fun run() {
            updateMyLocation()
            if (connectedEndpoints.isNotEmpty() && myLastLocation != null) {
                val sharedPrefs = getSharedPreferences("OFFSOS_PREFS", Context.MODE_PRIVATE)
                val cbPrivacyChecked = sharedPrefs.getBoolean("incognito_mode", false)
                val etNameText = sharedPrefs.getString("codename", "") ?: ""
                val myName = if (cbPrivacyChecked) "Signal-${(100..999).random()}" else etNameText
                val payload = "LOC:${myLastLocation!!.latitude},${myLastLocation!!.longitude},$myName"
                sendPayloadToAll(Payload.fromBytes(payload.toByteArray(StandardCharsets.UTF_8)))
            }
            handler.postDelayed(this, 3000)
        }
    }

    private fun sendPayloadToAll(payload: Payload) {
        for (endpointId in connectedEndpoints) {
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    private fun updateMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    myLastLocation = location
                    val myPoint = GeoPoint(location.latitude, location.longitude)
                    if (myMarker == null) {
                        myMarker = Marker(map)
                        myMarker?.title = "ME"
                        myMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        map.overlays.add(myMarker)
                        map.controller.animateTo(myPoint)
                    }
                    myMarker?.position = myPoint
                    if (isBroadcasting) map.controller.animateTo(myPoint)
                    map.invalidate()
                }
            }
        }
    }

    private fun updateRemoteUserMarker(id: String, lat: Double, lon: Double, name: String) {
        val remotePoint = GeoPoint(lat, lon)
        if (!isBroadcasting) map.controller.animateTo(remotePoint)
        var title = name
        if (myLastLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(myLastLocation!!.latitude, myLastLocation!!.longitude, lat, lon, results)
            title += " (${results[0].toInt()}m)"
        }
        if (activeMarkers.containsKey(id)) {
            val marker = activeMarkers[id]
            marker?.position = remotePoint
            marker?.title = title
            marker?.showInfoWindow()
        } else {
            val newMarker = Marker(map)
            newMarker.position = remotePoint
            newMarker.title = title
            newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            newMarker.icon = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.marker_default)
            map.overlays.add(newMarker)
            activeMarkers[id] = newMarker
        }
        map.invalidate()
    }

    private fun appendChatLog(msg: String) {
        tvChatLog.append("\n$msg")
    }

    // --- CONNECTION LOGIC ---
    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        val sharedPrefs = getSharedPreferences("OFFSOS_PREFS", Context.MODE_PRIVATE)
        var channel = sharedPrefs.getString("channel", "PUBLIC") ?: "PUBLIC"
        if (channel.isEmpty()) channel = "PUBLIC"
        channel = channel.uppercase().trim()
        val cbPrivacyChecked = sharedPrefs.getBoolean("incognito_mode", false)
        val etNameText = sharedPrefs.getString("codename", "") ?: ""
        val rawName = if (cbPrivacyChecked) "Signal-${(100..999).random()}" else etNameText
        val broadcastName = "$channel|$rawName"
        connectionsClient.startAdvertising(broadcastName, SERVICE_ID, connectionLifecycleCallback, options)
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
    }

    private fun stopAdvertising() = connectionsClient.stopAdvertising()
    private fun stopDiscovery() = connectionsClient.stopDiscovery()

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val foundName = info.endpointName
            Toast.makeText(applicationContext, "Found Signal: $foundName", Toast.LENGTH_SHORT).show()
            val sharedPrefs = getSharedPreferences("OFFSOS_PREFS", Context.MODE_PRIVATE)
            var myChannel = sharedPrefs.getString("channel", "PUBLIC") ?: "PUBLIC"
            if (myChannel.isEmpty()) myChannel = "PUBLIC"
            myChannel = myChannel.uppercase().trim()
            val etNameText = sharedPrefs.getString("codename", "") ?: ""
            val remoteInfo = foundName.split("|", limit = 2)
            if (remoteInfo.size == 2) {
                val remoteChannel = remoteInfo[0].uppercase().trim()
                if (remoteChannel == myChannel) {
                    Toast.makeText(applicationContext, "Match! Connecting...", Toast.LENGTH_SHORT).show()
                    connectionsClient.requestConnection(etNameText, endpointId, connectionLifecycleCallback)
                }
            } else {
                if (myChannel == "PUBLIC") connectionsClient.requestConnection(etNameText, endpointId, connectionLifecycleCallback)
            }
        }
        override fun onEndpointLost(endpointId: String) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpoints.add(endpointId)
                btnSos.playSoundEffect(SoundEffectConstants.NAVIGATION_UP)
                Toast.makeText(applicationContext, "MESH CONNECTED", Toast.LENGTH_SHORT).show()
                handler.post(sendLocationRunnable)
                if (!isBroadcasting) triggerRescuerAlarm(endpointId)
            }
        }
        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            activeMarkers[endpointId]?.let { map.overlays.remove(it) }
            activeMarkers.remove(endpointId)
            map.invalidate()
            Toast.makeText(applicationContext, "Signal Lost", Toast.LENGTH_SHORT).show()
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                runOnUiThread {
                    if (data.startsWith("LOC:")) {
                        try {
                            val parts = data.substring(4).split(",")
                            updateRemoteUserMarker(endpointId, parts[0].toDouble(), parts[1].toDouble(), if (parts.size > 2) parts[2] else "Unknown")
                        } catch (e: Exception) { }
                    } else if (data.startsWith("MSG:")) {
                        try {
                            val parts = data.split(":", limit = 3)
                            appendChatLog("${parts[1]}: ${parts[2]}")
                            if (!isChatMode) Toast.makeText(applicationContext, "Msg from ${parts[1]}", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) { }
                    }
                    else if (data.startsWith("DANGER:")) {
                        try {
                            val parts = data.substring(7).split(",")
                            val lat = parts[0].toDouble()
                            val lon = parts[1].toDouble()
                            drawDangerZone(lat, lon)
                            Toast.makeText(applicationContext, "⚠️ DANGER ZONE RECEIVED!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    else if (data.startsWith("SAFE:")) {
                        clearAllDangerZones()
                        Toast.makeText(applicationContext, "Danger Zone Cleared", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else if (payload.type == Payload.Type.FILE) {
                incomingFilePayloads.put(payload.id, payload)
                runOnUiThread { appendChatLog("* Receiving Audio... *") }
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                val payload = incomingFilePayloads.get(update.payloadId)
                if (payload != null && payload.type == Payload.Type.FILE) {
                    val file = payload.asFile()?.asJavaFile()
                    val uri = payload.asFile()?.asUri()
                    if (file != null && file.exists()) {
                        lastAudioFile = file
                        lastAudioUri = null
                        runOnUiThread {
                            btnPlayAudio.visibility = View.VISIBLE
                            appendChatLog("* Audio Received! *")
                            playReceivedAudio(file, null)
                        }
                    } else if (uri != null) {
                        lastAudioUri = uri
                        lastAudioFile = null
                        runOnUiThread {
                            btnPlayAudio.visibility = View.VISIBLE
                            appendChatLog("* Audio Received! *")
                            playReceivedAudio(null, uri)
                        }
                    } else {
                        runOnUiThread { appendChatLog("* Failed to process audio *") }
                    }
                    incomingFilePayloads.remove(update.payloadId)
                }
            }
        }
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) return false
        }
        return true
    }
    private fun requestPermissions() { ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_CODE) }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startDiscovery()
            updateMyLocation()
        }
    }

    override fun onFlareClicked() {
        startDigitalFlare()
    }

    override fun onStrobeClicked() {
        toggleStrobe()
    }

    override fun onDownloadMapClicked() {
        downloadOfflineRegion()
    }

    override fun onSentinelModeChanged(enabled: Boolean) {
        val intent = Intent(this, SosService::class.java)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
            Toast.makeText(this, "Sentinel Active", Toast.LENGTH_SHORT).show()
        } else {
            stopService(intent)
            Toast.makeText(this, "Sentinel Stopped", Toast.LENGTH_SHORT).show()
        }
    }
}