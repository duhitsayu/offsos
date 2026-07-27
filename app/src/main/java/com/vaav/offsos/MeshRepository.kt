package com.vaav.offsos

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.io.File
import java.nio.charset.StandardCharsets

data class RemoteUser(val id: String, val lat: Double, val lon: Double, val name: String)
data class DangerZone(val lat: Double, val lon: Double)

class MeshRepository(private val context: Context) {
    companion object {
        private const val TAG = "MeshRepository"
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = "com.vaav.offsos"
    private val retryHandler = Handler(Looper.getMainLooper())

    // --- Connection identity (set before advertising/discovery) ---
    private var myChannel: String = "PUBLIC"
    private var myName: String = ""

    // --- Connection state tracking ---
    private val pendingConnections = mutableSetOf<String>()    // endpoints we're currently connecting to
    private val retryQueue = mutableMapOf<String, Int>()       // endpointId → attempt count
    private val discoveredEndpoints = mutableMapOf<String, DiscoveredEndpointInfo>()  // still-visible endpoints

    // --- Observable state ---
    private val _connectedEndpoints = MutableLiveData<Set<String>>(emptySet())
    val connectedEndpoints: LiveData<Set<String>> get() = _connectedEndpoints

    private val _chatMessages = MutableLiveData<String>("")
    val chatMessages: LiveData<String> get() = _chatMessages

    private val _remoteUsers = MutableLiveData<Map<String, RemoteUser>>(emptyMap())
    val remoteUsers: LiveData<Map<String, RemoteUser>> get() = _remoteUsers

    private val _dangerZones = MutableLiveData<List<DangerZone>>(emptyList())
    val dangerZones: LiveData<List<DangerZone>> get() = _dangerZones

    private val _receivedAudioFile = MutableLiveData<File?>()
    val receivedAudioFile: LiveData<File?> get() = _receivedAudioFile

    private val _systemMessage = MutableLiveData<String>()
    val systemMessage: LiveData<String> get() = _systemMessage

    private val incomingFilePayloads = androidx.collection.SimpleArrayMap<Long, Payload>()

    // =========================================================================
    // Public API
    // =========================================================================

    fun setIdentity(channel: String, name: String) {
        myChannel = channel.uppercase().trim().ifEmpty { "PUBLIC" }
        myName = name
    }

    fun startAdvertising(channel: String, name: String) {
        setIdentity(channel, name)
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        val broadcastName = "$myChannel|$myName"
        connectionsClient.startAdvertising(broadcastName, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnSuccessListener { Log.d(TAG, "Advertising started as: $broadcastName") }
            .addOnFailureListener { e ->
                Log.e(TAG, "Advertising failed", e)
                _systemMessage.postValue("Advertising Failed: ${e.message}")
            }
    }

    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener { Log.d(TAG, "Discovery started") }
            .addOnFailureListener { e ->
                Log.e(TAG, "Discovery failed", e)
                _systemMessage.postValue("Discovery Failed: ${e.message}")
            }
    }

    fun stopAdvertising() = connectionsClient.stopAdvertising()
    fun stopDiscovery() = connectionsClient.stopDiscovery()

    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        pendingConnections.clear()
        retryQueue.clear()
        discoveredEndpoints.clear()
        retryHandler.removeCallbacksAndMessages(null)
        _connectedEndpoints.postValue(emptySet())
        _remoteUsers.postValue(emptyMap())
    }

    fun sendPayloadToAll(payload: Payload) {
        val endpoints = _connectedEndpoints.value ?: emptySet()
        for (endpointId in endpoints) {
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    fun clearDangerZones() {
        _dangerZones.postValue(emptyList())
    }

    fun addDangerZone(lat: Double, lon: Double) {
        val zones = _dangerZones.value?.toMutableList() ?: mutableListOf()
        zones.add(DangerZone(lat, lon))
        _dangerZones.postValue(zones)
    }

    // =========================================================================
    // Safe connection request — deduplicates and retries
    // =========================================================================

    private fun requestConnectionSafe(endpointId: String) {
        val connected = _connectedEndpoints.value ?: emptySet()
        if (endpointId in connected) {
            Log.d(TAG, "Already connected to $endpointId, skipping")
            return
        }
        if (endpointId in pendingConnections) {
            Log.d(TAG, "Already connecting to $endpointId, skipping")
            return
        }

        pendingConnections.add(endpointId)
        Log.d(TAG, "Requesting connection to $endpointId (pending: ${pendingConnections.size})")

        connectionsClient.requestConnection(myName, endpointId, connectionLifecycleCallback)
            .addOnFailureListener { e ->
                Log.e(TAG, "requestConnection failed for $endpointId: ${e.message}")
                pendingConnections.remove(endpointId)

                // If the endpoint is still visible, schedule a retry
                if (endpointId in discoveredEndpoints) {
                    scheduleRetry(endpointId)
                }
            }
    }

    private fun scheduleRetry(endpointId: String) {
        val attempts = retryQueue.getOrDefault(endpointId, 0)
        if (attempts >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Max retries reached for $endpointId, giving up")
            retryQueue.remove(endpointId)
            _systemMessage.postValue("Connection Failed after $MAX_RETRY_ATTEMPTS attempts")
            return
        }
        retryQueue[endpointId] = attempts + 1
        val delay = (attempts + 1) * 2000L  // 2s, 4s, 6s backoff
        Log.d(TAG, "Scheduling retry #${attempts + 1} for $endpointId in ${delay}ms")
        retryHandler.postDelayed({
            // Only retry if endpoint is still visible and not connected
            val connected = _connectedEndpoints.value ?: emptySet()
            if (endpointId in discoveredEndpoints && endpointId !in connected) {
                requestConnectionSafe(endpointId)
            } else {
                retryQueue.remove(endpointId)
            }
        }, delay)
    }

    // =========================================================================
    // Discovery callback — auto-connects matching endpoints
    // =========================================================================

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: $endpointId name=${info.endpointName}")
            discoveredEndpoints[endpointId] = info

            val foundName = info.endpointName
            val remoteInfo = foundName.split("|", limit = 2)

            val shouldConnect = if (remoteInfo.size == 2) {
                val remoteChannel = remoteInfo[0].uppercase().trim()
                remoteChannel == myChannel
            } else {
                myChannel == "PUBLIC"
            }

            if (shouldConnect) {
                _systemMessage.postValue("Found Signal: $foundName")
                requestConnectionSafe(endpointId)
            } else {
                Log.d(TAG, "Channel mismatch: remote=${remoteInfo.getOrNull(0)} mine=$myChannel")
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            discoveredEndpoints.remove(endpointId)
            retryQueue.remove(endpointId)
        }
    }

    // =========================================================================
    // Connection lifecycle — auto-accepts, tracks state
    // =========================================================================

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with $endpointId (${info.endpointName})")
            // Mark as pending if not already (happens when we're the advertiser receiving a connection)
            pendingConnections.add(endpointId)
            // Auto-accept ALL connections — this is critical for multi-device
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            _systemMessage.postValue("Connecting: ${info.endpointName}")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            pendingConnections.remove(endpointId)

            if (result.status.isSuccess) {
                Log.d(TAG, "Connected to $endpointId!")
                retryQueue.remove(endpointId)  // Clear any pending retries

                val current = _connectedEndpoints.value?.toMutableSet() ?: mutableSetOf()
                current.add(endpointId)
                _connectedEndpoints.postValue(current)
                _systemMessage.postValue("MESH_CONNECTED:$endpointId")
            } else {
                Log.w(TAG, "Connection to $endpointId failed: status=${result.status.statusCode}")
                _systemMessage.postValue("Connection rejected: $endpointId")

                // Retry if endpoint is still visible
                if (endpointId in discoveredEndpoints) {
                    scheduleRetry(endpointId)
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
            pendingConnections.remove(endpointId)

            val current = _connectedEndpoints.value?.toMutableSet() ?: mutableSetOf()
            current.remove(endpointId)
            _connectedEndpoints.postValue(current)

            val users = _remoteUsers.value?.toMutableMap() ?: mutableMapOf()
            users.remove(endpointId)
            _remoteUsers.postValue(users)

            _systemMessage.postValue("Signal Lost")

            // If endpoint is still visible, try to reconnect
            if (endpointId in discoveredEndpoints) {
                retryQueue.remove(endpointId) // Reset retry count for reconnection
                retryHandler.postDelayed({
                    val connected = _connectedEndpoints.value ?: emptySet()
                    if (endpointId in discoveredEndpoints && endpointId !in connected) {
                        Log.d(TAG, "Attempting to reconnect to $endpointId")
                        requestConnectionSafe(endpointId)
                    }
                }, 3000L)
            }
        }
    }

    // =========================================================================
    // Payload handling
    // =========================================================================

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                if (data.startsWith("LOC:")) {
                    try {
                        val parts = data.substring(4).split(",")
                        val lat = parts[0].toDouble()
                        val lon = parts[1].toDouble()
                        val name = if (parts.size > 2) parts[2] else "Unknown"
                        val currentUsers = _remoteUsers.value?.toMutableMap() ?: mutableMapOf()
                        currentUsers[endpointId] = RemoteUser(endpointId, lat, lon, name)
                        _remoteUsers.postValue(currentUsers)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse LOC", e)
                    }
                } else if (data.startsWith("MSG:")) {
                    try {
                        val parts = data.split(":", limit = 3)
                        val msg = "${parts[1]}: ${parts[2]}"
                        val currentMsgs = _chatMessages.value ?: ""
                        _chatMessages.postValue(currentMsgs + "\n" + msg)
                        _systemMessage.postValue("Msg from ${parts[1]}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse MSG", e)
                    }
                } else if (data.startsWith("DANGER:")) {
                    try {
                        val parts = data.substring(7).split(",")
                        val lat = parts[0].toDouble()
                        val lon = parts[1].toDouble()
                        val zones = _dangerZones.value?.toMutableList() ?: mutableListOf()
                        zones.add(DangerZone(lat, lon))
                        _dangerZones.postValue(zones)
                        _systemMessage.postValue("DANGER_ZONE_RECEIVED")
                    } catch (e: Exception) { e.printStackTrace() }
                } else if (data.startsWith("SAFE:")) {
                    _dangerZones.postValue(emptyList())
                    _systemMessage.postValue("Danger Zone Cleared")
                }
            } else if (payload.type == Payload.Type.FILE) {
                incomingFilePayloads.put(payload.id, payload)
                val currentMsgs = _chatMessages.value ?: ""
                _chatMessages.postValue(currentMsgs + "\n* Receiving Audio... *")
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                val payload = incomingFilePayloads.get(update.payloadId)
                if (payload != null && payload.type == Payload.Type.FILE) {
                    val file = payload.asFile()?.asJavaFile()
                    if (file != null) {
                        val currentMsgs = _chatMessages.value ?: ""
                        _chatMessages.postValue(currentMsgs + "\n* Audio Received! *")
                        _receivedAudioFile.postValue(file)
                    }
                    incomingFilePayloads.remove(update.payloadId)
                }
            }
        }
    }
}
