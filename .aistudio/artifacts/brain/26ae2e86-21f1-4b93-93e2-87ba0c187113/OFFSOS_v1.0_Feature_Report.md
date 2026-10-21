# OFFSOS — Version 1.0 Feature & System Report
**Tactical Offline Mesh & Emergency Beacon Platform**

---

## Executive Overview
**OFFSOS** is a zero-infrastructure, decentralized Android application designed for disaster relief, wilderness expeditions, search-and-rescue (SAR) operations, and grid-down tactical emergencies. It operates without relying on cellular towers or internet infrastructure, pairing device hardware (GPS, Bluetooth LE, Wi-Fi Direct, microphone, and internal storage) with an offline-first architecture.

The app adheres to a high-contrast **Tactical Monochrome aesthetic** (obsidian black `#000000`, crisp signal white `#FFFFFF`, and high-visibility alert red `#FF1744` for emergency beacons), ensuring immediate visibility under extreme environmental conditions and OLED battery conservation.

---

## 1. Hardware Permissions & Sensor Management

### 1.1 Automated App Launch Permissions Flow
- **Instant System Prompt**: Upon launching the application, the app automatically triggers an Android runtime permission dialog requesting:
  - `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION` (satellite GPS and network telemetry)
  - `RECORD_AUDIO` (tactical voice note capture)
  - `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT` (BLE peer beaconing on Android 12+)
  - `NEARBY_WIFI_DEVICES` (Wi-Fi Direct peer-to-peer discovery on Android 13+)
  - `POST_NOTIFICATIONS` (critical background alerts on Android 13+)
- **Sensors Restricted Warning Banner**: If permissions are denied or revoked, a persistent monochrome alert banner appears at the top of the interface with a 1-tap **"AUTHORIZE"** button to re-trigger permissions at any point without digging through system settings.

### 1.2 Sub-Meter High-Accuracy Telemetry
- **Dual GPS Telemetry Engine**:
  - Combines Google Play Services `FusedLocationProviderClient` (sub-meter priority accuracy) with direct fallback to Android's hardware `LocationManager` (`GPS_PROVIDER` and `NETWORK_PROVIDER`).
  - Capable of acquiring real raw GPS fixes in airplane mode or complete offline scenarios.
  - Telemetry readouts include latitude, longitude, altitude (in meters), and real-time accuracy variance (e.g., `±2.8m GPS`).
- **Barometer & Battery Telemetry**:
  - Real-time device battery monitoring with percentage reporting.
  - Hardware atmospheric pressure listener (`Sensor.TYPE_PRESSURE`) to track barometric altitude shifts.
- **Tactical Haptics & Strobe**:
  - Distinct Morse-code-style SOS waveform vibration pulses (`VibratorManager`) for emergency confirmation.

---

## 2. Real Multimedia Communication (Voice & Photo)

### 2.1 Genuine Voice Notes & Playback Engine (`TacticalAudioEngine.kt`)
- **Native AAC Audio Recording**:
  - Replaces all simulated timers with real hardware audio recording via `MediaRecorder` at 44.1 kHz sampling and 96 kbps bitrate.
  - Encodes directly into sandboxed internal storage (`filesDir/audio_notes/tactical_voice_*.m4a`).
- **Interactive Recording Bar**:
  - Displays a pulsing live second counter (up to 60 seconds) with **"CANCEL"** and **"TRANSMIT"** controls.
- **Embedded Audio Playback**:
  - Built-in `MediaPlayer` integration directly within the chat message bubbles.
  - Tap-to-play with real-time waveform bars and automatic release upon playback completion.

### 2.2 Secure Field Photo Transmission (`TacticalMediaManager.kt`)
- **Zero-Permission Android Photo Picker**:
  - Employs `ActivityResultContracts.PickVisualMedia` to select recon or field photos from the device gallery or camera roll without requiring broad storage permissions.
- **Automated Compression & EXIF Normalization**:
  - Automatically downsamples high-resolution photos to an offline mesh-friendly resolution (max 1280x1280) and compresses to 80% JPEG.
  - Automatically handles EXIF camera orientation (90°, 180°, 270° rotation).
  - Stores output securely in `filesDir/field_photos/`.
- **Coil AsyncImage Rendering & Full-Screen Modal**:
  - Images are rendered within message bubbles via Coil's `AsyncImage`.
  - Tapping on any photo opens a high-resolution, full-screen tactical preview modal with photo metadata and internal file path.

---

## 3. Offline GIS Mapping & Local Area Caching

### 3.1 Local Geographic Area Tile Cache (`OfflineMapCacheManager.kt`)
- **Slippy Tile Bounding Engine**:
  - Automatically computes Web Mercator tile coordinates (`lon2tileX`, `lat2tileY`) covering a ~3.5 km tactical radius around the user's GPS fix.
  - Downloads multi-level zoom sets: Zoom 13 (district), Zoom 14 (suburb), Zoom 15 (neighborhood), and Zoom 16 (tactical street-level).
  - Compact footprint: downloads ~35–50 PNG tiles (~1–2 MB total) to `filesDir/offline_map_cache/`.
- **WebView Tile Request Interception**:
  - Custom `WebViewClient.shouldInterceptRequest` captures tile URLs from OpenStreetMap, CartoDB, and OpenTopoMap and serves them directly from internal storage as `WebResourceResponse`.
  - Local Leaflet assets (`leaflet.js`, `leaflet.css`, and marker assets) are bundled in `assets/leaflet/` for 100% offline map rendering.
- **Synthetic Tactical Fallback Grid**:
  - In simulated offline mode or when a specific tile is not in cache, the engine dynamically generates a dark tactical vector grid tile with crosshairs and coordinates, avoiding grey or broken map tiles.

### 3.2 Live Interactive Leaflet Map Features
- **Accurate Real-Time Positioning**:
  - User marker automatically centers on real GPS coordinates upon receiving a lock.
  - **Recenter on GPS Button**: Floating action button with live accuracy badge (e.g., `±3.2m GPS`) that pans and zooms the viewport directly onto the user's position.
  - **Dynamic Sector Peer Placement**: Initial simulated mesh peers automatically cluster relative to the user's real sector (300m–800m away) rather than distant default coordinates.
- **Map Layers**:
  - Street View (OpenStreetMap Standard)
  - Topographic (OpenTopoMap with contour lines)
  - Tactical Dark (CartoDB Dark Matter)
- **Peer Markers**:
  - Color-coded indicators showing callsign, speed (km/h), heading (degrees), battery level, and pulsating beacon rings during active SOS broadcasts.

---

## 4. Decentralized Mesh Networking & Privacy Circles

### 4.1 One-Tap SOS Broadcasting Controller
- **Instant Mode Switching**: Switch between **LISTENING** (passive radio scanner) and **BROADCASTING** (active beacon).
- **Automated Beacon Dispatch**: Activating broadcast mode immediately:
  - Pulses hardware vibration in SOS cadence.
  - Transmits a high-priority emergency beacon with live coordinates across the local channel.
  - Flags the user's map marker with red pulsating radar waves.

### 4.2 Private Mesh Circles (Groups)
- **PIN-Protected Circles**:
  - Users can create custom mesh circles secured with a 6-digit numeric PIN.
  - Peer devices join by entering the PIN.
- **Tactical QR Code Sharing**:
  - Built-in `TacticalQrCanvas` renders high-contrast QR codes directly on device.
  - Allows zero-network, optical peer pairing in the field.
  - Granular Location Sharing Modes:
    - `PRIVATE_GROUP`: Real-time location is shared strictly with authorized group members.
    - `PUBLIC_EMERGENCY`: In active emergencies, coordinates are broadcast openly to all nearby mesh units.

---

## 5. Security, OPSEC & Multi-Lingual Support

### 5.1 Tactical Profile & Identity
- **Dynamic Node ID**: Auto-generates unique, persistent tactical hardware node identifiers (e.g., `NODE-PIXEL-7821`).
- **Custom Callsign**: Users can set, edit, and update their radio callsign at any time.
- **Emergency Data Wipe**: 1-tap **"WIPE ALL DATA"** action in the profile screen immediately purges Room message tables, group memberships, and logs for complete OPSEC protection.

### 5.2 Multi-Lingual Interface (7 Global Languages)
- Instant localization without internet connectivity across 7 languages:
  1. **English**
  2. **Hindi** (हिन्दी)
  3. **French** (Français)
  4. **Spanish** (Español)
  5. **German** (Deutsch)
  6. **Arabic** (العربية)
  7. **Japanese** (日本語)

---

## 6. Verification & Test Summary
- **Kotlin & Compose Compilation**: All source files compiled successfully with AGP 9.1 and Kotlin 2.2.
- **Robolectric JVM Unit Tests**:
  - `read string from context`: Verified application label is `OFFSOS`.
  - `verify mesh node and broadcasting models`: Validated beaconing states, roles, and message types.
  - `verify group and privacy sharing mode models`: Validated group PIN encryption and location modes.
  - `verify offline map cache slippy tile calculations`: Validated mathematical correctness of `lon2tileX` and `lat2tileY` bounds.
- **Test Execution Result**: `BUILD SUCCESSFUL` across all 33 tasks in 34s.
