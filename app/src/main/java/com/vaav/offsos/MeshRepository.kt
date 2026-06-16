package com.vaav.offsos

import android.content.Context
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
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = "com.vaav.offsos"

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

    private val _foundEndpoint = MutableLiveData<Pair<String, DiscoveredEndpointInfo>>()
    val foundEndpoint: LiveData<Pair<String, DiscoveredEndpointInfo>> get() = _foundEndpoint

    private val _connectionRequest = MutableLiveData<Pair<String, ConnectionInfo>>()
    val connectionRequest: LiveData<Pair<String, ConnectionInfo>> get() = _connectionRequest

    private val incomingFilePayloads = androidx.collection.SimpleArrayMap<Long, Payload>()

    fun startAdvertising(channel: String, myName: String) {
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        val broadcastName = "$channel|$myName"
        connectionsClient.startAdvertising(broadcastName, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnFailureListener { e -> _systemMessage.postValue("Advertising Failed: ${e.message}") }
    }

    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnFailureListener { e -> _systemMessage.postValue("Discovery Failed: ${e.message}") }
    }

    fun stopAdvertising() = connectionsClient.stopAdvertising()
    fun stopDiscovery() = connectionsClient.stopDiscovery()

    fun requestConnection(myName: String, endpointId: String) {
        connectionsClient.requestConnection(myName, endpointId, connectionLifecycleCallback)
            .addOnFailureListener { e -> _systemMessage.postValue("Connection Failed: ${e.message}") }
    }

    fun acceptConnection(endpointId: String) {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
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

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            _foundEndpoint.postValue(Pair(endpointId, info))
        }
        override fun onEndpointLost(endpointId: String) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            _connectionRequest.postValue(Pair(endpointId, info))
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                val current = _connectedEndpoints.value?.toMutableSet() ?: mutableSetOf()
                current.add(endpointId)
                _connectedEndpoints.postValue(current)
                _systemMessage.postValue("MESH_CONNECTED:$endpointId")
            }
        }

        override fun onDisconnected(endpointId: String) {
            val current = _connectedEndpoints.value?.toMutableSet() ?: mutableSetOf()
            current.remove(endpointId)
            _connectedEndpoints.postValue(current)

            val users = _remoteUsers.value?.toMutableMap() ?: mutableMapOf()
            users.remove(endpointId)
            _remoteUsers.postValue(users)
            
            _systemMessage.postValue("Signal Lost")
        }
    }

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
                    } catch (e: Exception) {}
                } else if (data.startsWith("MSG:")) {
                    try {
                        val parts = data.split(":", limit = 3)
                        val msg = "${parts[1]}: ${parts[2]}"
                        val currentMsgs = _chatMessages.value ?: ""
                        _chatMessages.postValue(currentMsgs + "\n" + msg)
                        _systemMessage.postValue("Msg from ${parts[1]}")
                    } catch (e: Exception) {}
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
