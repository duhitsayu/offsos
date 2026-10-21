package com.example.model

enum class NodeRole {
    CLIENT,
    RELAY,
    GATEWAY
}

data class MeshNode(
    val id: String,
    val name: String,
    val role: NodeRole = NodeRole.CLIENT,
    val distanceKm: Double,
    val batteryPercent: Int = 100,
    val rssiDbm: Int = -65,
    val hopsToInternet: Int = 1,
    val lat: Double,
    val lon: Double,
    val isOnline: Boolean = true,
    val lastPingAgo: String = "Just now",
    val groupId: String? = null,
    val headingDeg: Float = 0f,
    val speedKmh: Float = 0f,
    val isBroadcastingSos: Boolean = false
) {
    val isPublicEmergency: Boolean get() = isBroadcastingSos
}

enum class MessageType {
    TEXT,
    IMAGE,
    VOICE
}

data class MeshMessage(
    val id: String,
    val sender: String,
    val senderId: String = "",
    val content: String,
    val timestamp: Long,
    val type: MessageType = MessageType.TEXT,
    val mediaUriOrData: String? = null,
    val audioDurationSec: Int? = null,
    val isSos: Boolean = false,
    val groupId: String? = null,
    val hops: Int = 1
)

enum class BroadcastingState {
    LISTENING,    // Passive listening: scanning for signals, receiving beacons
    BROADCASTING  // Active beaconing: your coordinates, identity, and SOS are transmitted
}

enum class LocationSharingMode {
    PRIVATE_GROUP,     // Location only shared with private group members
    PUBLIC_EMERGENCY   // In emergency, broadcast location publicly to all nearby peers
}

data class MeshGroup(
    val id: String,
    val name: String,
    val pin: String,
    val isOwner: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val members: List<GroupMember> = emptyList()
)

data class GroupMember(
    val id: String,
    val callsign: String,
    val isSelf: Boolean = false,
    val lat: Double,
    val lon: Double,
    val headingDeg: Float = 0f,
    val speedKmh: Float = 0f,
    val batteryPercent: Int = 100,
    val isBroadcasting: Boolean = false,
    val lastUpdate: Long = System.currentTimeMillis()
)

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    SPANISH("es", "Spanish", "Español"),
    HINDI("hi", "Hindi", "हिन्दी"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    JAPANESE("ja", "Japanese", "日本語"),
    ARABIC("ar", "Arabic", "العربية")
}

data class SystemLogEntry(
    val id: String,
    val timeAgo: String,
    val cluster: String,
    val title: String,
    val detail: String,
    val status: String = "ACTIVE"
)
