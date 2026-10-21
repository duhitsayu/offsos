package com.example.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

data class OfflineCacheState(
    val tileCount: Int = 0,
    val sizeBytes: Long = 0L,
    val isDownloading: Boolean = false,
    val downloadedTiles: Int = 0,
    val totalTilesToDownload: Int = 0,
    val downloadProgress: Float = 0f,
    val currentTileKey: String = "",
    val sectorName: String = "Sector 7G (Local Mesh)",
    val centerLat: Double = 12.9716,
    val centerLon: Double = 77.5946,
    val isOfflineSimulated: Boolean = false,
    val lastDownloadedTime: Long = 0L
)

class OfflineMapCacheManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var downloadJob: Job? = null
    private val cacheDir = File(context.filesDir, "offline_map_cache")

    private val _state = MutableStateFlow(OfflineCacheState())
    val state: StateFlow<OfflineCacheState> = _state.asStateFlow()

    init {
        refreshCacheStats()
    }

    private fun refreshCacheStats() {
        scope.launch {
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            var count = 0
            var bytes = 0L
            cacheDir.walkTopDown().filter { it.isFile && it.extension == "png" }.forEach {
                count++
                bytes += it.length()
            }
            _state.value = _state.value.copy(
                tileCount = count,
                sizeBytes = bytes
            )
        }
    }

    fun toggleOfflineSimulation(enabled: Boolean? = null) {
        val next = enabled ?: !_state.value.isOfflineSimulated
        _state.value = _state.value.copy(isOfflineSimulated = next)
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _state.value = _state.value.copy(isDownloading = false)
        refreshCacheStats()
    }

    fun clearCache() {
        downloadJob?.cancel()
        downloadJob = null
        scope.launch {
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            }
            _state.value = _state.value.copy(
                tileCount = 0,
                sizeBytes = 0L,
                isDownloading = false,
                downloadedTiles = 0,
                totalTilesToDownload = 0,
                downloadProgress = 0f,
                lastDownloadedTime = 0L
            )
        }
    }

    /**
     * Downloads a small offline cache of the local geographic area around [centerLat], [centerLon]
     * encompassing Zoom levels 13 (district), 14 (suburb), 15 (neighborhood), and 16 (tactical street-level).
     */
    fun startLocalAreaDownload(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double = 3.5,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        if (_state.value.isDownloading) return

        downloadJob = scope.launch {
            try {
                if (!cacheDir.exists()) cacheDir.mkdirs()

                val tilesToFetch = calculateTileRange(centerLat, centerLon, radiusKm)
                val totalCount = tilesToFetch.size

                _state.value = _state.value.copy(
                    isDownloading = true,
                    totalTilesToDownload = totalCount,
                    downloadedTiles = 0,
                    downloadProgress = 0f,
                    centerLat = centerLat,
                    centerLon = centerLon
                )

                var successful = 0
                for ((index, tileCoord) in tilesToFetch.withIndex()) {
                    val (z, x, y) = tileCoord
                    _state.value = _state.value.copy(
                        currentTileKey = "Z$z / X$x / Y$y",
                        downloadedTiles = index,
                        downloadProgress = if (totalCount > 0) index.toFloat() / totalCount else 0f
                    )

                    val targetFile = File(cacheDir, "$z/$x/$y.png")
                    if (!targetFile.exists() || targetFile.length() == 0L) {
                        val downloaded = downloadSingleTile(z, x, y, targetFile)
                        if (downloaded) successful++
                    } else {
                        successful++
                    }
                }

                refreshCacheStats()
                _state.value = _state.value.copy(
                    isDownloading = false,
                    downloadedTiles = totalCount,
                    downloadProgress = 1f,
                    lastDownloadedTime = System.currentTimeMillis()
                )
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, "Offline sector cache ready: $successful tiles secured")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isDownloading = false)
                refreshCacheStats()
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Download failed: ${e.message}")
                }
            }
        }
    }

    private fun downloadSingleTile(z: Int, x: Int, y: Int, targetFile: File): Boolean {
        return try {
            targetFile.parentFile?.mkdirs()
            val tileUrl = "https://tile.openstreetmap.org/$z/$x/$y.png"
            val conn = (URL(tileUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "OFFSOS-OfflineMesh/1.0 (Emergency Offline Tactical Cache)")
                requestMethod = "GET"
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Look up whether a tile is available in the local offline cache.
     */
    fun getOfflineTile(z: Int, x: Int, y: Int): File? {
        val f = File(cacheDir, "$z/$x/$y.png")
        return if (f.exists() && f.length() > 0) f else null
    }

    /**
     * Checks if a tile exists in offline cache
     */
    fun hasOfflineTile(z: Int, x: Int, y: Int): Boolean {
        return getOfflineTile(z, x, y) != null
    }

    /**
     * Synthesizes a clean tactical offline grid tile if offline and tile not yet in cache.
     */
    fun generateSyntheticGridTile(z: Int, x: Int, y: Int): ByteArray {
        val width = 256
        val height = 256
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Obsidian tactical dark background
        canvas.drawColor(Color.parseColor("#0C1115"))

        val gridPaint = Paint().apply {
            color = Color.parseColor("#1C262E")
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
        }

        val borderPaint = Paint().apply {
            color = Color.parseColor("#293844")
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#8BA2B0")
            textSize = 10f
            isAntiAlias = true
            isFakeBoldText = true
        }

        // Draw 4x4 inner grid
        for (i in 1..3) {
            val offset = i * (width / 4f)
            canvas.drawLine(offset, 0f, offset, height.toFloat(), gridPaint)
            canvas.drawLine(0f, offset, width.toFloat(), offset, gridPaint)
        }

        // Draw outer tile boundary
        canvas.drawRect(Rect(0, 0, width, height), borderPaint)

        // Draw crosshair at center
        val centerPaint = Paint().apply {
            color = Color.parseColor("#445A69")
            strokeWidth = 2f
        }
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawLine(cx - 14f, cy, cx + 14f, cy, centerPaint)
        canvas.drawLine(cx, cy - 14f, cx, cy + 14f, centerPaint)

        // Center Coordinate Label
        val label = "OFFLINE GRID • Z$z"
        val coordLabel = "X:$x Y:$y"
        canvas.drawText(label, 12f, 22f, textPaint)
        canvas.drawText(coordLabel, 12f, 38f, textPaint)
        canvas.drawText("P2P MESH SECTOR", 12f, height - 12f, textPaint)

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    /**
     * Calculates the slippy map tile range for zooms 13, 14, 15, and 16.
     * Capped to keep the offline cache small, fast to download, and memory efficient.
     */
    private fun calculateTileRange(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double
    ): List<Triple<Int, Int, Int>> {
        val list = mutableListOf<Triple<Int, Int, Int>>()

        // Delta lat & lon for bounding box
        val deltaLat = radiusKm / 111.0
        val cosLat = max(0.2, cos(Math.toRadians(centerLat)))
        val deltaLon = radiusKm / (111.0 * cosLat)

        val minLat = centerLat - deltaLat
        val maxLat = centerLat + deltaLat
        val minLon = centerLon - deltaLon
        val maxLon = centerLon + deltaLon

        // Zoom levels: 13, 14, 15, 16
        val zooms = listOf(13, 14, 15, 16)
        for (z in zooms) {
            val minX = lon2tileX(minLon, z)
            val maxX = lon2tileX(maxLon, z)
            // Note: Tile Y is inverted (0 is North)
            val minY = lat2tileY(maxLat, z)
            val maxY = lat2tileY(minLat, z)

            // Clamp range to prevent runaway sizes at high zoom
            val xRange = (minX..maxX).toList().take(4)
            val yRange = (minY..maxY).toList().take(4)

            for (x in xRange) {
                for (y in yRange) {
                    list.add(Triple(z, x, y))
                }
            }
        }
        return list
    }

    companion object {
        fun lon2tileX(lon: Double, zoom: Int): Int {
            return Math.floor((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
        }

        fun lat2tileY(lat: Double, zoom: Int): Int {
            val latRad = Math.toRadians(lat)
            return Math.floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * (1 shl zoom)).toInt()
        }
    }
}
