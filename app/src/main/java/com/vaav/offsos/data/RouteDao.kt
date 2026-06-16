package com.vaav.offsos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<RoutePoint>)

    @Query("SELECT * FROM route_points WHERE groupId = :groupId ORDER BY timestamp ASC")
    suspend fun getPointsForGroup(groupId: String): List<RoutePoint>

    @Query("SELECT * FROM route_points WHERE groupId = :groupId AND timestamp > :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getPointsForGroupSince(groupId: String, sinceTimestamp: Long): List<RoutePoint>

    @Query("DELETE FROM route_points WHERE groupId = :groupId AND id IN (:ids)")
    suspend fun deletePoints(groupId: String, ids: List<Long>)
    
    @Query("SELECT * FROM route_points WHERE peerId = :peerId AND groupId = :groupId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPointForPeer(peerId: String, groupId: String): RoutePoint?
    
    @Query("DELETE FROM route_points WHERE groupId = :groupId")
    suspend fun clearGroup(groupId: String)
}
