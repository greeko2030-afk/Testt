# Testt - Production-Grade Relay VPN Prototype

This repository contains a production-style prototype for a secure remote internet relay system.

Important:
- A regular Android app cannot fully replace OS-level tethering without Android VPN APIs and proper tunnel routing.
- This project implements the production architecture pattern: Android VPN service + cloud relay backend + encrypted control channel.
- It is intended as a real-world prototype and starting point for production development.

## Architecture

- Android app with VPNService for tunnel establishment
- Relay server on cloud or local machine
- Secure WebSocket layer for session registration and peer matching
- Source/client pairing for remote relay testing

## Components

- `android/` - Android app (Kotlin + Compose)
- `server/` - Node.js relay backend
- `.github/workflows/build-apk.yml` - GitHub Actions APK build
- `DEPLOYMENT.md` - cloud deploy instructions

## Quick Start

### 1) Start the relay backend

```bash
cd server
npm install
npm start
```

### 2) Build the Android APK

Open `android` in Android Studio and run the app or build APK.

### 3) Connect devices

- Source device: has internet connection
- Client device: needs relay access
- Use the same server URL for both devices

## Production Notes

For a real production deployment:
- run the relay server on a cloud VM or container
- use TLS/HTTPS and authentication
- use persistent state and monitoring
- move to a real VPN tunnel implementation for packet forwarding

## License

MIT
