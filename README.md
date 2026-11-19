# PROXY X

Arabic-first Android proxy/VPN client with a neon-purple cyberpunk interface.

## Supported manual profile formats
HTTP, HTTPS, SOCKS4, SOCKS5, VMess, VLESS, Trojan, Shadowsocks and WireGuard.

## Core
The project is being migrated to sing-box libbox for protocol parsing and tunnel support. sing-box is GPL-3.0; distributing an app that embeds it requires compliance with the applicable GPL terms, including source-code and notices. See https://github.com/singbox-android/libbox.

## Security and reliability
No bundled servers are provided. Users must supply valid server credentials. A successful APK build does not prove that a server is reachable or that every imported URI is valid. Connection testing must be performed with real user-provided profiles and Android VPN permission.

## Build
GitHub Actions builds a debug APK using JDK 17 and Gradle.
