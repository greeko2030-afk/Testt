# Test App - Internet Relay Prototype

Dhaka theke Chattogram - long distance-e ekta mobile theke onno mobile-e internet share korte parbe.

## Architecture

- **Device A (Source)**: Internet ache, relay source
- **Device B (Client)**: Internet nai, relay client
- **Relay Server**: Cloud-e chole, duto device-er moddhe traffic relay kore

## Features

✅ Android app (Kotlin + Jetpack Compose)
✅ WebSocket-based relay server (Node.js + Socket.IO)
✅ Source/Client role selector
✅ Real-time connection status
✅ Live logs and debugging

## Quick Start

### Server Setup

```bash
cd server
npm install
npm start
```

Server runs on port 3000.

### Android App

1. Clone the repo
2. Open `android` folder in Android Studio
3. Sync Gradle
4. Run on emulator or real device
5. Enter server URL:
   - Emulator: `http://10.0.2.2:3000`
   - Real device: `http://<your-pc-ip>:3000`
6. Click "Source" or "Client" to connect

## Project Structure

```
Test/
├── server/              # Node.js relay server
│   ├── index.js        # Main server logic
│   ├── package.json    # Dependencies
│   └── .env.example    # Environment template
├── android/            # Android app project
│   ├── app/            # App module
│   │   ├── src/
│   │   │   └── main/
│   │   │       ├── AndroidManifest.xml
│   │   │       ├── java/com/example/test/MainActivity.kt
│   │   │       └── res/
│   │   └── build.gradle.kts
│   ├── settings.gradle.kts
│   └── build.gradle.kts
└── README.md
```

## How It Works

1. **Device A connects as Source**
   - App connects to relay server
   - Registers as "source" (has internet)
   - Waits for client connection

2. **Device B connects as Client**
   - App connects to relay server
   - Registers as "client" (needs internet)
   - Server matches with source

3. **Relay Established**
   - Server sends relay request to source
   - Source approves or rejects
   - Once approved, both devices get connected peer info
   - Data flows Device B → Server → Device A → Internet

## Technical Stack

- **Frontend**: Android (Kotlin + Jetpack Compose)
- **Backend**: Node.js + Express + Socket.IO
- **Communication**: WebSocket (Socket.IO)
- **Build**: Gradle (Android)

## Important Notes

⚠️ **This is a prototype/demo app**
- Not a full OS-level tethering solution
- Demonstrates relay architecture concept
- For testing and learning purposes

⚠️ **For production use, you need:**
- Real VPN/tunnel implementation
- System-level network routing
- Proper encryption and authentication
- Scalable backend infrastructure

## Environment Variables

Server `.env` file:
```
PORT=3000
```

## License

MIT
