package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BhoomiDao {
    @Query("SELECT * FROM mesh_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MeshMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MeshMessageEntity)

    @Query("DELETE FROM mesh_messages")
    suspend fun clearMeshMessages()

    @Query("SELECT * FROM mesh_groups")
    fun getAllGroups(): Flow<List<MeshGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: MeshGroupEntity)

    @Query("DELETE FROM mesh_groups")
    suspend fun clearMeshGroups()
}
