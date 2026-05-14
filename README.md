# 🛡️ NetHeal Mobile

**NetHeal Mobile** is an advanced, autonomous network security system for Android. Powered by a high-performance Rust core and local VPN technology, it provides real-time protection, self-healing capabilities, and deep threat analytics—all without ever leaving your device.

---

## 🧠 Core Architecture

NetHeal Mobile operates as a device-level firewall, ensuring 100% privacy and zero latency by performing all security logic locally.

1.  **Local VPN Interceptor:** Routes all device traffic through a virtual interface to capture packet metadata.
2.  **Rust Security Core (NDK):** A multi-module engine written in memory-safe Rust that processes analysis, rule evaluation, and system healing.
3.  **Autonomous Threat Scoring:** Evaluates connections based on request rates, burst ratios, and domain reputation to assign real-time risk scores (0-100).
4.  **Persistent Intelligence (Room DB):** Stores firewall rules, whitelists, manual blacklists, and detailed threat logs locally.

---

## 🚀 Advanced Features

-   **Intelligent Firewall:** Automatic blocking of DDoS-like behavior, malicious trackers, and bot-driven requests.
-   **Per-App Control:** Discover and restrict internet access for any installed user application.
-   **Security Modes:** Toggle "Military Mode" for ultra-strict filtering and zero-tolerance blocking.
-   **Self-Healing Engine:** Proactively checks system integrity and restores firewall rules if any corruption or bypass is detected.
-   **Live Security Dashboard:** Professional cyber-themed UI featuring real-time health monitoring and threat feeds.
-   **Notification System:** Instant alerts for critical threat interceptions.

---

## 🛠️ Project Structure

-   `app/`: Android Application (Kotlin + Compose)
    -   `vpn/`: VPNService & Traffic Monitoring logic.
    -   `bridge/`: JNI layer for high-speed Rust communication.
    -   `data/`: Room DB persistence layer (Logs, Rules, Whitelist).
    -   `ui/`: Premium Jetpack Compose screens.
-   `rust-core/`: The Security Powerhouse
    -   `firewall.rs`: Static and dynamic rule management.
    -   `analyzer.rs`: Threat classification (DDoS, Bot, Malware).
    -   `healer.rs`: System restoration and integrity logic.

---

## 📦 Build & CI/CD

The project is fully integrated with **GitHub Actions**. Every push automatically:
1.  Sets up the Rust NDK environment.
2.  Compiles the Rust Security Core for multiple Android architectures.
3.  Builds the Android application using Gradle 8.8 with KSP support.
4.  Generates a ready-to-install **Debug APK**.

---

## 🔐 Security & Privacy First

-   **Zero Trust:** Every connection is analyzed before being allowed.
-   **Local Only:** No data is uploaded to any cloud service.
-   **Standard APIs:** Works on any Android 8.0+ device without needing root access.

*Designed for those who demand ultimate control over their mobile privacy.*
