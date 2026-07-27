package com.vaav.offsos

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.nearby.connection.Payload
import java.nio.charset.StandardCharsets

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MeshRepository(application)

    val connectedEndpoints = repository.connectedEndpoints
    val chatMessages = repository.chatMessages
    val remoteUsers = repository.remoteUsers
    val dangerZones = repository.dangerZones
    val receivedAudioFile = repository.receivedAudioFile
    val systemMessage = repository.systemMessage

    private val _isBroadcasting = MutableLiveData(false)
    val isBroadcasting: LiveData<Boolean> get() = _isBroadcasting

    private val _isChatMode = MutableLiveData(false)
    val isChatMode: LiveData<Boolean> get() = _isChatMode

    private val _isSettingsOpen = MutableLiveData(false)
    val isSettingsOpen: LiveData<Boolean> get() = _isSettingsOpen
    
    private val _isStrobing = MutableLiveData(false)
    val isStrobing: LiveData<Boolean> get() = _isStrobing
    
    private val _myLastLocation = MutableLiveData<Location?>()
    val myLastLocation: LiveData<Location?> get() = _myLastLocation

    fun setLocation(location: Location) {
        _myLastLocation.value = location
    }

    /**
     * Set the channel and codename so MeshRepository knows who we are
     * for channel-matching during discovery. Call this before starting discovery.
     */
    fun setChannelAndName(channel: String, name: String) {
        repository.setIdentity(channel, name)
    }

    fun toggleSosMode(channel: String, myName: String) {
        val currentlyBroadcasting = _isBroadcasting.value ?: false
        if (currentlyBroadcasting) {
            _isBroadcasting.value = false
            repository.stopAdvertising()
            repository.startDiscovery()
        } else {
            _isBroadcasting.value = true
            repository.stopDiscovery()
            repository.startAdvertising(channel, myName)
        }
    }

    fun setChatMode(isOpen: Boolean) {
        _isChatMode.value = isOpen
    }

    fun setSettingsOpen(isOpen: Boolean) {
        _isSettingsOpen.value = isOpen
    }
    
    fun setStrobing(isStrobing: Boolean) {
        _isStrobing.value = isStrobing
    }

    fun sendMessage(msg: String, myName: String) {
        val fullMessage = "MSG:$myName:$msg"
        repository.sendPayloadToAll(Payload.fromBytes(fullMessage.toByteArray(StandardCharsets.UTF_8)))
    }

    fun sendLocation(lat: Double, lon: Double, myName: String) {
        val payload = "LOC:$lat,$lon,$myName"
        repository.sendPayloadToAll(Payload.fromBytes(payload.toByteArray(StandardCharsets.UTF_8)))
    }

    fun broadcastDangerZone(lat: Double, lon: Double) {
        val dangerPayload = "DANGER:$lat,$lon"
        repository.sendPayloadToAll(Payload.fromBytes(dangerPayload.toByteArray(StandardCharsets.UTF_8)))
    }

    fun broadcastSafeZone() {
        val safePayload = "SAFE:ALL"
        repository.sendPayloadToAll(Payload.fromBytes(safePayload.toByteArray(StandardCharsets.UTF_8)))
        repository.clearDangerZones()
    }
    
    fun addLocalDangerZone(lat: Double, lon: Double) {
        repository.addDangerZone(lat, lon)
    }

    fun sendFilePayload(payload: Payload) {
        repository.sendPayloadToAll(payload)
    }
    
    fun startDiscovery(channel: String, name: String) {
        repository.setIdentity(channel, name)
        repository.startDiscovery()
    }

    fun stopAll() {
        repository.stopAll()
    }
}
