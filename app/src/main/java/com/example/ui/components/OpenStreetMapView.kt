package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.map.OfflineMapCacheManager
import com.example.model.GroupMember
import com.example.model.MeshNode
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.regex.Pattern

enum class MapTileLayer(val label: String, val urlTemplate: String, val attribution: String) {
    OSM_STANDARD("Street View", "https://tile.openstreetmap.org/{z}/{x}/{y}.png", "© OpenStreetMap contributors"),
    OSM_TOPO("Topographic", "https://tile.opentopomap.org/{z}/{x}/{y}.png", "© OpenStreetMap, SRTM | OpenTopoMap"),
    CARTO_DARK("Tactical Dark", "https://basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}.png", "© OpenStreetMap, CARTO")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OpenStreetMapView(
    userLocation: Pair<Double, Double>,
    peers: List<MeshNode>,
    groupMembers: List<GroupMember>,
    isPublicEmergency: Boolean,
    offlineCacheManager: OfflineMapCacheManager,
    currentTileLayer: MapTileLayer = MapTileLayer.OSM_STANDARD,
    selectedPeerId: String? = null,
    recenterTrigger: Int = 0,
    onPeerSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                loadWithOverviewMode = true
                useWideViewPort = true
                allowContentAccess = true
                allowFileAccess = true
            }
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                private val tilePattern = Pattern.compile(".*/(\\d+)/(\\d+)/(\\d+)\\.png.*")

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url ?: return super.shouldInterceptRequest(view, request)
                    val urlStr = url.toString()

                    // 1. Local Leaflet Engine Interception (100% offline support)
                    if (urlStr.contains("leaflet.css")) {
                        try {
                            val stream = context.assets.open("leaflet/leaflet.css")
                            return WebResourceResponse("text/css", "UTF-8", stream)
                        } catch (_: Exception) {}
                    }
                    if (urlStr.contains("leaflet.js")) {
                        try {
                            val stream = context.assets.open("leaflet/leaflet.js")
                            return WebResourceResponse("application/javascript", "UTF-8", stream)
                        } catch (_: Exception) {}
                    }
                    if (urlStr.contains("marker-icon.png")) {
                        try {
                            val stream = context.assets.open("leaflet/images/marker-icon.png")
                            return WebResourceResponse("image/png", null, stream)
                        } catch (_: Exception) {}
                    }
                    if (urlStr.contains("marker-shadow.png")) {
                        try {
                            val stream = context.assets.open("leaflet/images/marker-shadow.png")
                            return WebResourceResponse("image/png", null, stream)
                        } catch (_: Exception) {}
                    }

                    // 2. Offline Map Tile Cache Interception
                    val matcher = tilePattern.matcher(urlStr)
                    if (matcher.matches()) {
                        try {
                            val z = matcher.group(1).toInt()
                            val x = matcher.group(2).toInt()
                            val y = matcher.group(3).toInt()

                            // Check local downloaded cache first
                            val cachedTile = offlineCacheManager.getOfflineTile(z, x, y)
                            if (cachedTile != null) {
                                return WebResourceResponse("image/png", null, FileInputStream(cachedTile))
                            }

                            // If simulated offline mode is active or tile missing in cache
                            if (offlineCacheManager.state.value.isOfflineSimulated) {
                                val gridBytes = offlineCacheManager.generateSyntheticGridTile(z, x, y)
                                return WebResourceResponse("image/png", null, ByteArrayInputStream(gridBytes))
                            }
                        } catch (_: Exception) {}
                    }

                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                }
            }
        }
    }

    // Initialize HTML on first load
    LaunchedEffect(Unit) {
        val initialHtml = getLeafletMapHtml(
            lat = if (userLocation.first != 0.0) userLocation.first else 12.9716,
            lon = if (userLocation.second != 0.0) userLocation.second else 77.5946,
            tileUrl = currentTileLayer.urlTemplate,
            attribution = currentTileLayer.attribution
        )
        webView.loadDataWithBaseURL("https://offsos.local/", initialHtml, "text/html", "UTF-8", null)
    }

    // Auto-center on user's real GPS coordinates when first acquired
    var hasCenteredOnRealLocation by remember { mutableStateOf(false) }

    LaunchedEffect(userLocation) {
        if (userLocation.first != 0.0 && userLocation.first != 12.9716 && !hasCenteredOnRealLocation) {
            hasCenteredOnRealLocation = true
            val script = "recenterMap(${userLocation.first}, ${userLocation.second}, 16);"
            webView.evaluateJavascript(script, null)
        }
    }

    // Manual Recenter Trigger from UI Button
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            val userLat = if (userLocation.first != 0.0) userLocation.first else 12.9716
            val userLon = if (userLocation.second != 0.0) userLocation.second else 77.5946
            val script = "recenterMap($userLat, $userLon, 16);"
            webView.evaluateJavascript(script, null)
        }
    }

    // Switch Tile Layer if user selects a different style
    LaunchedEffect(currentTileLayer) {
        val script = "setTileLayer('${currentTileLayer.urlTemplate}', '${currentTileLayer.attribution}');"
        webView.evaluateJavascript(script, null)
    }

    // Push real-time peer & user positions to Leaflet
    LaunchedEffect(userLocation, peers, groupMembers, isPublicEmergency) {
        val userLat = if (userLocation.first != 0.0) userLocation.first else 12.9716
        val userLon = if (userLocation.second != 0.0) userLocation.second else 77.5946

        val peersJson = JSONArray()

        // 1. Group Members (if in private group)
        groupMembers.filter { !it.isSelf }.forEach { member ->
            val obj = JSONObject().apply {
                put("id", member.id)
                put("name", member.callsign)
                put("lat", member.lat)
                put("lon", member.lon)
                put("isEmergency", false)
                put("isGroupMember", true)
                put("speed", "${String.format(java.util.Locale.US, "%.1f", member.speedKmh)} km/h")
                put("heading", member.headingDeg)
                put("battery", "${member.batteryPercent}%")
            }
            peersJson.put(obj)
        }

        // 2. Mesh Nodes (filtered by group or public emergency)
        peers.forEach { node ->
            val obj = JSONObject().apply {
                put("id", node.id)
                put("name", node.name)
                put("lat", node.lat)
                put("lon", node.lon)
                put("isEmergency", node.isPublicEmergency)
                put("isGroupMember", node.groupId != null)
                put("speed", "${String.format(java.util.Locale.US, "%.1f", node.speedKmh)} km/h")
                put("heading", node.headingDeg)
                put("battery", "${node.batteryPercent}%")
            }
            peersJson.put(obj)
        }

        val updateScript = """
            updatePositions($userLat, $userLon, $isPublicEmergency, ${peersJson.toString()});
        """.trimIndent()
        webView.evaluateJavascript(updateScript, null)
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            webView.destroy()
        }
    }
}

private fun getLeafletMapHtml(lat: Double, lon: Double, tileUrl: String, attribution: String): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://offsos.local/leaflet/leaflet.css" />
    <script src="https://offsos.local/leaflet/leaflet.js"></script>
    <style>
        html, body, #map {
            width: 100%;
            height: 100%;
            margin: 0;
            padding: 0;
            background: #000000;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
        }
        .user-marker {
            width: 24px;
            height: 24px;
            background: #000000;
            border: 2px solid #ffffff;
            border-radius: 50%;
            box-shadow: 0 0 12px rgba(255,255,255,0.4);
            position: relative;
        }
        .user-marker::after {
            content: '';
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 8px;
            height: 8px;
            background: #ffffff;
            border-radius: 50%;
        }
        .peer-marker {
            background: #ffffff;
            border: 2px solid #000000;
            border-radius: 50%;
            width: 24px;
            height: 24px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 10px;
            font-weight: bold;
            color: #000000;
            box-shadow: 0 2px 6px rgba(0,0,0,0.6);
            transition: all 0.8s ease-out;
        }
        .emergency-marker {
            background: #D32F2F !important;
            color: #ffffff !important;
            border: 2px solid #ffffff !important;
            animation: pulse-sos 1.2s infinite;
        }
        .group-marker {
            background: #2E7D32 !important;
            color: #ffffff !important;
            border: 2px solid #ffffff !important;
        }
        @keyframes pulse-sos {
            0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(211, 47, 47, 0.7); }
            70% { transform: scale(1.18); box-shadow: 0 0 0 12px rgba(211, 47, 47, 0); }
            100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(211, 47, 47, 0); }
        }
        .leaflet-popup-content-wrapper {
            background: #0e1317;
            color: #ffffff;
            border-radius: 2px;
            font-family: monospace;
            border: 1px solid #334442;
        }
        .leaflet-popup-tip {
            background: #0e1317;
        }
        .offline-notice {
            position: absolute;
            bottom: 18px;
            left: 12px;
            background: rgba(0,0,0,0.85);
            color: #e0e0e0;
            padding: 4px 8px;
            font-size: 10px;
            z-index: 1000;
            border: 1px solid #444444;
            border-radius: 2px;
            pointer-events: none;
            letter-spacing: 0.5px;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <div class="offline-notice" id="offline-banner">OPENSTREETMAP • LOCAL OFFLINE CACHE CAPABLE</div>
    <script>
        var map = L.map('map', {
            zoomControl: false,
            attributionControl: true
        }).setView([$lat, $lon], 15);

        L.control.zoom({ position: 'topright' }).addTo(map);

        var currentLayer = L.tileLayer('$tileUrl', {
            maxZoom: 19,
            attribution: '$attribution'
        }).addTo(map);

        // User Marker
        var userIcon = L.divIcon({
            className: 'user-marker',
            iconSize: [24, 24],
            iconAnchor: [12, 12]
        });

        var userMarker = L.marker([$lat, $lon], { icon: userIcon }).addTo(map);
        userMarker.bindPopup("<b>YOU // Sector 7G</b><br>Local Mesh Node Beacon");

        // Peer markers cache
        var peerMarkers = {};

        function setTileLayer(url, attr) {
            if (currentLayer) {
                map.removeLayer(currentLayer);
            }
            currentLayer = L.tileLayer(url, {
                maxZoom: 19,
                attribution: attr
            }).addTo(map);
        }

        function updatePositions(userLat, userLon, isEmergency, peersList) {
            userMarker.setLatLng([userLat, userLon]);

            var activeIds = {};

            for (var i = 0; i < peersList.length; i++) {
                var p = peersList[i];
                activeIds[p.id] = true;

                var className = 'peer-marker';
                if (p.isEmergency) {
                    className += ' emergency-marker';
                } else if (p.isGroupMember) {
                    className += ' group-marker';
                }

                var icon = L.divIcon({
                    className: className,
                    iconSize: [24, 24],
                    iconAnchor: [12, 12],
                    html: p.isEmergency ? '!' : (p.name.substring(0, 2).toUpperCase())
                });

                if (peerMarkers[p.id]) {
                    peerMarkers[p.id].setLatLng([p.lat, p.lon]);
                    peerMarkers[p.id].setIcon(icon);
                } else {
                    var m = L.marker([p.lat, p.lon], { icon: icon }).addTo(map);
                    peerMarkers[p.id] = m;
                }

                var popupHtml = "<b>" + p.name + "</b><br>" +
                                "Speed: " + p.speed + "<br>" +
                                "Heading: " + p.heading + "°<br>" +
                                "Battery: " + p.battery + "<br>" +
                                (p.isEmergency ? "<span style='color:#FF5252;'>🚨 CRITICAL SOS ACTIVE</span>" : "🔒 Group Verified");
                peerMarkers[p.id].bindPopup(popupHtml);
            }

            for (var id in peerMarkers) {
                if (!activeIds[id]) {
                    map.removeLayer(peerMarkers[id]);
                    delete peerMarkers[id];
                }
            }
        }

        function recenterMap(lat, lon, zoom) {
            if (map) {
                map.setView([lat, lon], zoom || 16, { animate: true });
                if (userMarker) {
                    userMarker.setLatLng([lat, lon]);
                    userMarker.openPopup();
                }
            }
        }
    </script>
</body>
</html>
    """.trimIndent()
}
