# NetHeal Mobile - Engineering Directives (Absolute Omega)

## 🦀 Rust Core
- **Fast-Path DPI**: Use `performance_mode` to skip redundant L7 checks during high load.
- **Watchdog**: Kernel must fail-safe (lockdown) if Kotlin heartbeat is missing > 30s.
- **Kernel GC**: Periodic pruning of `active_connections` and `cache` is mandatory.

## 📱 Android Layer
- **Battery Intelligence**: Auto-trigger Stamina Mode in Rust when battery is low.
- **Posture Awareness**: Dynamic security levels based on SSID trust and captive portal status.
- **Persistence**: Foreground priority + WakeLocks to prevent OS suppression.

## 📊 Telemetry
- Dashboard must sync every 2s for "live" feel.
- PCAP Export must be filtered by the current Search query.
