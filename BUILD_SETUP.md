# 🛠️ NetHeal Mobile - Build Setup Guide

This guide explains how to set up your environment to build and run NetHeal Mobile.

---

## 1. Prerequisites

### Rust & NDK
- **Rust:** Install via [rustup](https://rustup.rs/).
- **Android NDK:** Install via Android Studio SDK Manager (Version 25+ recommended).
- **Cargo NDK:** Install the helper tool:
  ```bash
  cargo install cargo-ndk
  ```
- **Rust Targets:** Add the Android targets:
  ```bash
  rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
  ```

### Android Studio
- **Version:** Flamingo (2022.2.1) or newer.
- **Java:** JDK 17+ (usually bundled with Android Studio).

---

## 2. Building the Rust Core

The Rust core must be compiled into shared libraries (`.so`) that Android can load.

1. Navigate to the Rust directory:
   ```bash
   cd rust-core
   ```

2. Build for specific ABIs (e.g., 64-bit ARM):
   ```bash
   cargo ndk -t arm64-v8a build --release
   ```

3. Copy the output to the Android project:
   The compiled library will be at `target/aarch64-linux-android/release/libnetheal_core.so`.
   Copy it to `app/src/main/jniLibs/arm64-v8a/libnetheal.so`.

   *Note: Repeat for other architectures (v7a, x86) if deploying to physical devices or emulators.*

---

## 3. Building the Android App

1. Open the project root in **Android Studio**.
2. Wait for Gradle sync to finish.
3. Ensure the `jniLibs` folder contains the compiled Rust libraries from Step 2.
4. Click **Run** to deploy to your device or emulator.

---

## 4. Troubleshooting

### "Library not found" Error
Ensure the file name in `System.loadLibrary("netheal")` matches the filename in `jniLibs` (without the `lib` prefix and `.so` suffix).

### VPN Permission Denied
When you first click "Enable Protection", Android will show a system dialog. You **must** accept this for the VPNService to start.

### NDK Not Found
Specify your NDK path in `local.properties`:
```properties
ndk.dir=/path/to/your/ndk
```

---

## 5. Development Workflow

Whenever you change Rust code:
1. Rebuild with `cargo ndk`.
2. Copy the new `.so` to `jniLibs`.
3. Re-run the Android app.
