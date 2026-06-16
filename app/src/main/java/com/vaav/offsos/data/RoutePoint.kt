package com.vaav.offsos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_points")
data class RoutePoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val peerId: String,
    val groupId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isGap: Boolean = false
)
