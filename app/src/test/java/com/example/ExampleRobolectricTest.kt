package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.map.OfflineMapCacheManager
import com.example.model.BroadcastingState
import com.example.model.LocationSharingMode
import com.example.model.MeshGroup
import com.example.model.MeshNode
import com.example.model.MessageType
import com.example.model.NodeRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OFFSOS", appName)
  }

  @Test
  fun `verify mesh node and broadcasting models`() {
    val node = MeshNode(
      id = "NODE-101",
      name = "RescueAlpha",
      role = NodeRole.GATEWAY,
      distanceKm = 0.4,
      batteryPercent = 88,
      lat = 12.9716,
      lon = 77.5946,
      isBroadcastingSos = true
    )
    assertEquals("NODE-101", node.id)
    assertTrue(node.isPublicEmergency)
    assertEquals(BroadcastingState.BROADCASTING, BroadcastingState.valueOf("BROADCASTING"))
    assertEquals(BroadcastingState.LISTENING, BroadcastingState.valueOf("LISTENING"))
    assertEquals(MessageType.VOICE, MessageType.valueOf("VOICE"))
  }

  @Test
  fun `verify group and privacy sharing mode models`() {
    val group = MeshGroup(
      id = "GRP-01",
      name = "SAR Alpha",
      pin = "849-210",
      isOwner = true,
      createdAt = System.currentTimeMillis()
    )
    assertEquals("849-210", group.pin)
    assertEquals(LocationSharingMode.PRIVATE_GROUP, LocationSharingMode.valueOf("PRIVATE_GROUP"))
    assertEquals(LocationSharingMode.PUBLIC_EMERGENCY, LocationSharingMode.valueOf("PUBLIC_EMERGENCY"))
  }

  @Test
  fun `verify offline map cache slippy tile calculations`() {
    val lat = 12.9716
    val lon = 77.5946
    val zoom = 15
    val tileX = OfflineMapCacheManager.lon2tileX(lon, zoom)
    val tileY = OfflineMapCacheManager.lat2tileY(lat, zoom)

    assertTrue(tileX > 0)
    assertTrue(tileY > 0)
    // Zoom 15 max tile index is 2^15 - 1 = 32767
    assertTrue(tileX < (1 shl zoom))
    assertTrue(tileY < (1 shl zoom))
  }
}
