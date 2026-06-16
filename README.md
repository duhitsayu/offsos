<div align="center">

```
 ██████╗ ███████╗███████╗███████╗ ██████╗ ███████╗
██╔═══██╗██╔════╝██╔════╝██╔════╝██╔═══██╗██╔════╝
██║   ██║█████╗  █████╗  ███████╗██║   ██║███████╗
██║   ██║██╔══╝  ██╔══╝  ╚════██║██║   ██║╚════██║
╚██████╔╝██║     ██║     ███████║╚██████╔╝███████║
 ╚═════╝ ╚═╝     ╚═╝     ╚══════╝ ╚═════╝ ╚══════╝
```

### Offline Mesh Emergency SOS — No Signal Required

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-FF3B30?style=for-the-badge)](LICENSE)
[![API](https://img.shields.io/badge/Min_API-26-FF9500?style=for-the-badge&logo=android)](https://developer.android.com)
[![Stars](https://img.shields.io/github/stars/duhitsayu/offsos?style=for-the-badge&color=FFD60A)](https://github.com/duhitsayu/offsos)

<br>

> **When every second counts and no signal exists — OffSOS keeps you connected.**

<br>

```
Device A ──────────── Device B ──────────── Device C
   │      Bluetooth /        │   WiFi Direct      │
   │      WiFi Direct        │                    │
 [SOS]                   [RELAY]              [ALERT]

  No towers. No internet. No limits.
```

</div>

---

## The Problem

When disaster strikes — a flood, an earthquake, a trekking emergency — the first thing that fails is the network. Cell towers go down. WiFi disappears. The people who need help the most lose the ability to call for it.

**OffSOS exists for exactly that moment.**

---

## How It Works

OffSOS builds a peer-to-peer mesh network directly between Android devices using Bluetooth and WiFi Direct. No router. No tower. No server. Just devices talking directly to each other — and relaying messages further down the chain.

```
┌─────────────────────────────────────────────────────┐
│                   OFFSOS MESH                       │
│                                                     │
│   [You] ←──50m──→ [Peer A] ←──80m──→ [Peer B]     │
│                      │                              │
│                   [Peer C] ←──60m──→ [Rescue]      │
│                                                     │
│   Your SOS travels through every node               │
│   until it reaches someone who can help             │
└─────────────────────────────────────────────────────┘
```

---

## Features

### Emergency Core
| Feature | Description |
|---|---|
| **SOS Broadcast** | Hold to activate — sends alert to every peer in the mesh instantly |
| **Danger Markers** | Drop a pin on hazardous locations — syncs to all connected peers |
| **Dead Man's Switch** | Auto-triggers SOS if you don't check in within a set time |
| **SOS Templates** | One-tap messages — Injured / Lost / Need Water / Need Medical Help |
| **Emergency Card** | Blood group, allergies, name visible to rescuers on your device |

### Mesh Network
| Feature | Description |
|---|---|
| **Multi-hop Relay** | Your device forwards messages from others — extends range exponentially |
| **Auto Protocol Switch** | Switches between Bluetooth and WiFi Direct for best range |
| **Hop Count Display** | See how many devices a message travelled through |
| **Delivery Confirmation** | Know your SOS was received, not just sent |
| **Private Key Groups** | Encrypted groups — only devices with matching key see your data |

### Maps & Navigation
| Feature | Description |
|---|---|
| **Offline Maps** | OpenStreetMap tiles downloaded to device — works with zero data |
| **Route Recording** | GPS path logged every 10 seconds, shared with your group |
| **Shared Waypoints** | Drop pins that sync across all group members in real time |
| **Peer Location Markers** | See where every connected device is on the map |
| **Elevation Data** | Terrain awareness for mountain and hill rescue scenarios |

### Tactical Tools
| Feature | Description |
|---|---|
| **Flare Mode** | Screen strobe for visual signalling in low visibility |
| **Offline Compass** | Magnetometer-based, no GPS or internet required |
| **Survival Timer** | Tracks time elapsed since emergency began |
| **Battery Saver Mode** | Reduces broadcast frequency to extend device life |
| **QR Key Sharing** | Share private group key via QR code — no manual typing |

---

## Technology Stack

```
┌──────────────────────────────────────────────┐
│                  OFFSOS STACK                │
├──────────────────┬───────────────────────────┤
│ Language         │ Kotlin                    │
│ Architecture     │ MVVM + ViewBinding        │
│ Mesh Networking  │ Google Nearby Connections │
│ Mapping          │ osmdroid (OpenStreetMap)  │
│ Local Storage    │ Room Database             │
│ Encryption       │ AES-256                   │
│ UI Components    │ Material Design 3         │
│ QR Generation    │ ZXing (offline)           │
│ Audio            │ Opus Codec                │
└──────────────────┴───────────────────────────┘
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Physical Android device (API 26+)
- Two or more devices for mesh testing

> Nearby Connections requires physical devices — emulators will not work for mesh testing.

### Installation

```bash
# Clone the repository
git clone https://github.com/duhitsayu/offsos.git

# Open in Android Studio
cd offsos

# Sync Gradle dependencies
./gradlew dependencies

# Build and install on device
./gradlew installDebug
```

### Required Permissions

```xml
<!-- OffSOS needs these to build the mesh -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

---

## Mesh Range Reference

```
Direct Connection (2 devices)
├── Bluetooth:    10 –  30 m
└── WiFi Direct:  50 – 200 m

Mesh Relay (5 devices spread out)
└── Estimated coverage: 300 – 500 m

Mesh Relay (10 devices spread out)
└── Estimated coverage: 1 km+

Note: Walls, trees, and terrain reduce effective range.
Open environments provide maximum range.
```

---

## Target Use Cases

- **Trekkers & Hikers** — remote areas with no signal coverage
- **Disaster Response** — floods, earthquakes, cyclones where towers fail
- **Search & Rescue Teams** — coordinated offline group communication
- **Rural Communities** — areas with permanently poor connectivity
- **Large Outdoor Events** — network congestion makes cellular unreliable

---

## Offline Maps Setup

Map tiles are stored in the app's internal cache to comply with Android scoped storage requirements.

```
1. Open OffSOS → Settings → Download Offline Map
2. Pan to your region on the map
3. Select download radius (5 km / 10 km / 25 km)
4. Tap Download — tiles save to internal storage
5. Map works fully offline from this point
```

---

## Architecture Overview

```
┌─────────────────────────────────────────┐
│              UI LAYER                   │
│   Activities / Fragments / ViewBinding  │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│           VIEWMODEL LAYER               │
│   MeshViewModel / MapViewModel /        │
│   SOSViewModel / RouteViewModel         │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│           REPOSITORY LAYER              │
│   MeshRepository / LocationRepository  │
│   RouteRepository / SOSRepository      │
└──────────┬───────────────┬─────────────┘
           │               │
┌──────────▼───┐   ┌───────▼─────────────┐
│  ROOM DB     │   │  NEARBY CONNECTIONS │
│  Routes      │   │  Mesh P2P Layer     │
│  Waypoints   │   │  BT + WiFi Direct   │
│  Peers       │   │  AES-256 Encrypted  │
└──────────────┘   └─────────────────────┘
```

---

## Contributing

Contributions, issues, and feature requests are welcome.

```bash
# Fork the repository
# Create your feature branch
git checkout -b feature/your-feature-name

# Commit your changes
git commit -m "Add: your feature description"

# Push to your branch
git push origin feature/your-feature-name

# Open a Pull Request
```

Please follow the existing MVVM architecture and ensure all features work completely offline before submitting.

---

## Roadmap

- [x] Offline mesh networking via Nearby Connections
- [x] OpenStreetMap offline tiles
- [x] SOS broadcast to peers
- [x] Real-time location sharing
- [x] Danger marker with peer sync
- [x] Private key groups with AES-256
- [x] Route recording with group sharing
- [x] Shared waypoints
- [ ] Voice messages over mesh
- [ ] Dead man's switch
- [ ] iOS support investigation
- [ ] Web dashboard for rescue coordinators

---

## License

```
MIT License

Copyright (c) 2025 Ayush — OffSOS

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software to use, copy, modify, merge, publish, and
distribute without restriction, provided the above copyright notice
appears in all copies.
```

---

<div align="center">

**Built for the moments when everything else fails.**

[Report a Bug](https://github.com/duhitsayu/offsos/issues) · [Request a Feature](https://github.com/duhitsayu/offsos/issues) · [Discussions](https://github.com/duhitsayu/offsos/discussions)

<br>

*OffSOS — Because emergencies don't wait for signal.*

</div>
