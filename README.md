# OFFSOS

OFFSOS is an offline-first mesh networking emergency SOS application designed for off-grid environments where traditional cellular or Wi-Fi networks are unavailable. By leveraging the **Google Nearby Connections API**, OFFSOS enables devices to communicate, share locations, and send critical SOS alerts entirely peer-to-peer.

## 🚀 Key Features

- **📡 Offline Mesh Networking:** Connects nearby devices without requiring internet access.
- **🗺️ Offline Maps:** Integrated with `osmdroid` (OpenStreetMap). Users can download map regions (e.g. 10km radius) directly to their device for fully offline navigation.
- **🚨 SOS Emergency Alerts:** Instantly broadcast SOS signals to all connected peers in the mesh network.
- **📍 Real-time Location Sharing:** Share your current GPS coordinates seamlessly with nearby users.
- **💾 Local Persistence:** Built-in caching and local data storage powered by Room Database.

## 🛠️ Technology Stack

- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel)
- **Networking:** Google Nearby Connections API
- **Mapping:** osmdroid (OpenStreetMap)
- **Local Storage:** Room Database
- **UI:** Android ViewBinding, Material Components, Modern Glass/Gradient Aesthetics

## 📦 Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/duhitsayu/offsos.git
   ```
2. **Open the project** in Android Studio.
3. **Sync Gradle** to fetch all required dependencies.
4. **Build and run** the app on an Android device (Physical devices are required for testing Nearby Connections effectively).

## 🗺️ Offline Maps Usage
To use the offline maps feature, ensure you grant the required location permissions. You can download a specific region directly from the app. Map tiles are downloaded into the app's internal cache to comply with Android's scoped storage requirements and prevent crashes on newer Android versions.

## 🤝 Contributing
Contributions, issues, and feature requests are welcome! Feel free to check the issues page.

## 📝 License
This project is open-sourced under the MIT License.
