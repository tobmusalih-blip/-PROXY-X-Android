# PROXY X

Android proxy/VPN client foundation built with Kotlin and Jetpack Compose.

## Current prototype
- Manual configuration entry only; no bundled or fake servers.
- Recognizes URI schemes for HTTP, HTTPS, SOCKS4/5, VMess, VLESS, Trojan, Shadowsocks and WireGuard.
- Local in-memory profile list and dark neon interface.
- GitHub Actions builds a debug APK.

## Important status
This first build is a UI/configuration prototype, **not yet a working VPN tunnel**. Protocol URI recognition and profile display do not establish a proxy connection. Protocol engines (such as sing-box and a WireGuard implementation) still need integration, as do secure profile persistence, robust import validation and connection lifecycle handling. Do not use this prototype as a privacy/security tool.

## Build
GitHub Actions runs `gradle assembleDebug` and uploads the APK as a workflow artifact.
