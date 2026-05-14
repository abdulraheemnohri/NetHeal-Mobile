# 🛡️ NetHeal Mobile: Absolute Edition (v4.5.0)

**NetHeal Mobile** is an advanced, autonomous network security system for Android. Powered by a high-performance **Rust security core** and local VPN technology, it provides real-time Deep Packet Inspection (DPI), autonomous threat mitigation, and deep privacy analytics—all operating 100% locally on your device.

---

## 🧠 Absolute Core Architecture

NetHeal operates at the network layer, ensuring zero-latency protection by performing complex analysis within the device's NDK boundary.

1.  **L7 Deep Packet Inspection (DPI):** Extracts target domains from both plain-text DNS (UDP) and encrypted TLS SNI (TCP) handshakes.
2.  **Rust Absolute Engine (NDK):** A multi-threaded powerhouse that handles raw packet parsing, Shannon entropy scoring, and zero-trust rule execution.
3.  **Autonomous Immune System:** Continuous self-healing logic that monitors policy integrity and automatically restores firewall states.
4.  **Persistent Intelligence:** Fully transactional Room DB (v8) architecture for managing thousands of concurrent policies and detailed telemetry logs.

---

## 🚀 Professional Arsenal

-   **Intelligent Firewall:** Multi-layer blocking for DGA (Domain Generation Algorithms), botnets, and data-exfiltration syndicates.
-   **Emergency Kill Switch:** A true 'zero-link' mode that instantly terminates all ingress/egress traffic during critical system compromise.
-   **3-State App Isolation:** Granular control over connectivity for every app: *Allowed*, *WiFi-Only* (Simulated), or *Absolute Block*.
-   **Live Security Dashboard:** Interactive 'Cyber' interface featuring node topology maps, live traffic graphs, and a real-time security terminal.
-   **Intelligent Privacy Audit:** Heuristic scoring of installed apps to identify high-risk background services and potential spyware.
-   **Custom Upstream DNS:** Toggle between Cloudflare, AdGuard, or Google encrypted resolvers with integrated DNS intercept.

---

## 🛠️ Technical Implementation

### Android Layer (Kotlin / Jetpack Compose)
-   **VpnService:** High-throughput non-blocking TUN interface implementation.
-   **Persistence:** Max-priority foreground service with PowerManager WakeLocks for 24/7 background stability.
-   **UI:** Reactive Compose layouts with high-frequency Canvas visualizations.

### Rust Security Layer (NDK / JNI)
-   **firewall.rs:** Dynamic L7 policy engine with IP/Domain CIDR-aware filtering.
-   **analyzer.rs:** Shannon entropy calculator and AI heuristics for threat classification.
-   **packet.rs:** Hand-optimized IPv4/UDP/TCP/DNS/TLS protocol parsers.
-   **bridge:** Low-latency JNI channel for high-frequency telemetry sync.

---

## 📦 Build & Deployment

The project is fully integrated with **GitHub Actions**. Every push triggers:
1.  **Rust NDK Compilation:** Generates optimized `.so` binaries for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.
2.  **Android Build:** Compiles the Kotlin/Compose frontend using Gradle 8.8.
3.  **Artifact Generation:** Produces a ready-to-deploy **Absolute Edition APK**.

---

## 🔐 Zero-Export Privacy

-   **Zero Cloud dependency:** No metrics or logs ever leave the local database.
-   **Memory Safety:** Rust core prevents 100% of common buffer-overflow and use-after-free vulnerabilities in the network stack.
-   **Transparency:** All intercepted packets are visible in the real-time terminal logs.

*Designed for the most demanding security environments.*
