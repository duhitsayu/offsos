package com.vaav.offsos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.vaav.offsos.data.AppDatabase
import com.vaav.offsos.data.RoutePoint
import com.vaav.offsos.util.CryptoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SosService : Service() {

    private val CHANNEL_ID = "OFFSOS_bg_channel"
    private val SERVICE_ID = "com.vaav.offsos"
    private lateinit var connectionsClient: ConnectionsClient
    private var mediaPlayer: android.media.MediaPlayer? = null
    // SOS Vibration Pattern: 3 short, 3 long, 3 short
    private val vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 200, 100, 500, 100, 500, 100, 500, 100, 200, 100, 200, 100, 200)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var channelName: String = "PUBLIC"
    private var myName: String = ""

    override fun onCreate() {
        super.onCreate()
        connectionsClient = Nearby.getConnectionsClient(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        
        val sharedPrefs = getSharedPreferences("OFFSOS_PREFS", Context.MODE_PRIVATE)
        channelName = sharedPrefs.getString("channel", "PUBLIC") ?: "PUBLIC"
        myName = sharedPrefs.getString("codename", "") ?: ""
        
        setupLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If the intent tells us to STOP, we kill the service
        if (intent?.getBooleanExtra("STOP_SIREN", false) == true) {
            stopSelf()
            return START_NOT_STICKY
        }

        // 1. Create the persistent notification
        val notification = createNotification("Sentinel Mode Active", "Tracking Location & Scanning...")
        startForeground(1, notification)

        // 2. Start listening for signals in the background
        startDiscovery()
        
        // 3. Start location tracking
        startLocationTracking()

        return START_STICKY
    }
    
    private var lastLocationTimestamp = 0L

    private fun setupLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val currentTime = System.currentTimeMillis()
                    val isGap = (lastLocationTimestamp != 0L) && (currentTime - lastLocationTimestamp > 30000)
                    
                    val groupId = CryptoUtils.getGroupId(channelName)
                    val point = RoutePoint(
                        peerId = myName,
                        groupId = groupId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = currentTime,
                        isGap = isGap
                    )
                    
                    lastLocationTimestamp = currentTime
                    
                    serviceScope.launch {
                        AppDatabase.getDatabase(applicationContext).routeDao().insertPoints(listOf(point))
                    }
                }
            }
        }
    }
    
    private fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()
            
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(SERVICE_ID, object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                // If a signal is found while backgrounded, trigger the alarm
                triggerAlert(info.endpointName)
            }
            override fun onEndpointLost(endpointId: String) {}
        }, options)
    }

    private fun triggerAlert(name: String) {
        // 1. Play Loud Alarm Sound
        try {
            if (mediaPlayer == null) {
                val alertUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)

                mediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(applicationContext, alertUri)
                    setAudioStreamType(android.media.AudioManager.STREAM_ALARM)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // 2. Show High Priority Notification
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("STOP_SIREN", true)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val alert = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚨 SOS DETECTED: $name")
            .setContentText("TAP TO RESPOND AND STOP ALARM")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setVibrate(vibrationPattern)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(999, alert)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OFFSOS Sentinel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Re-use an existing icon, but since it's missing, let's use standard android icon
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        connectionsClient.stopDiscovery()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}