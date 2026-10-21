package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mesh_messages")
data class MeshMessageEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val senderId: String = "",
    val content: String,
    val timestamp: Long,
    val isSos: Boolean = false,
    val channel: String = "MESH",
    val hops: Int = 1,
    val type: String = "TEXT", // "TEXT", "IMAGE", "VOICE"
    val mediaUriOrData: String? = null,
    val audioDurationSec: Int? = null,
    val groupId: String? = null
)

@Entity(tableName = "mesh_groups")
data class MeshGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val pin: String,
    val isOwner: Boolean,
    val createdAt: Long
)
