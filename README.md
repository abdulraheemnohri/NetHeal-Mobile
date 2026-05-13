# 🛡️ NetHeal Mobile

**NetHeal Mobile** is a self-healing Android firewall system powered by a local VPN engine and a high-performance Rust security core. It provides device-level autonomous security by intercepting, analyzing, and blocking malicious network traffic in real time.

---

## 🧠 Architecture

NetHeal Mobile follows an "All-in-Android" architecture to ensure maximum privacy and speed without relying on external servers.

1.  **Android VPNService (Intercept):** Captures all outgoing and incoming device traffic.
2.  **Rust Security Engine (Analyze):** A memory-safe core (via NDK) that performs deep packet inspection metadata analysis, risk scoring, and rule evaluation.
3.  **Self-Healing Logic (Repair):** Automatically restores corrupted firewall rules or restarts the VPN tunnel if system integrity is compromised.
4.  **Jetpack Compose Dashboard (Visualize):** A modern, dark-themed UI providing live threat feeds and security status.

---

## 🚀 Key Features

-   **Autonomous Firewall:** Automatically blocks high-risk domains and suspicious IP addresses.
-   **Rust-Powered Performance:** Heavy security logic runs in a memory-safe Rust environment for near-zero latency.
-   **Self-Healing Engine:** Proactively repairs firewall configurations and ensures the protection layer remains active.
-   **Live Security Dashboard:** Real-time visualization of blocked connections and threat risk scores.
-   **Zero Trust by Default:** Every connection is evaluated based on request rate, domain reputation, and behavior.

---

## 🛠️ Project Structure

-   `app/`: Android application (Kotlin + Jetpack Compose).
    -   `vpn/`: VPNService implementation for traffic interception.
    -   `bridge/`: JNI Bridge to communicate with Rust core.
    -   `ui/`: Security dashboard and rule management screens.
-   `rust-core/`: High-performance security engine.
    -   `firewall.rs`: Logic for blocking/allowing domains.
    -   `analyzer.rs`: Threat scoring and behavior analysis.
    -   `healer.rs`: System integrity restoration logic.

---

## 📦 Build Instructions

1.  **Rust Core:** Navigate to `rust-core/` and build using the Android NDK (requires `cargo-ndk`).
2.  **Android App:** Open the root directory in Android Studio. Ensure NDK is installed.
3.  **JNI Libs:** The project expects `libnetheal.so` to be present in `app/src/main/jniLibs/`.

---

## 🔐 Security & Privacy

-   **On-Device Processing:** No traffic metadata ever leaves your device.
-   **No Root Required:** Uses the standard Android VPN API.
-   **Memory Safety:** Leveraging Rust to prevent common vulnerabilities like buffer overflows in network processing.

---

*Note: This project is a specialized security tool and requires VPN permissions to function.*
