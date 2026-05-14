# 🛠️ NetHeal Mobile - Final Build Setup

NetHeal Mobile is a high-performance Android security suite. Follow these steps to build the project from source.

## 1. Environment Setup

- **Rust:** Install via [rustup](https://rustup.rs/).
- **Cargo NDK:** \`cargo install cargo-ndk\`
- **Android Targets:** \`rustup target add aarch64-linux-android armv7-linux-androideabi\`
- **Java:** JDK 17 required.

## 2. Compile Security Engine (Rust)

Navigate to the \`rust-core\` directory and build for your target architecture:

\`\`\`bash
cd rust-core
# For 64-bit ARM (Physical devices)
cargo ndk -t arm64-v8a build --release
# For 32-bit ARM
cargo ndk -t armeabi-v7a build --release
\`\`\`

The libraries will be generated in \`rust-core/target/<abi>/release/libnetheal_core.so\`.

## 3. Configure Android App

1. Copy the compiled \`.so\` files to \`app/src/main/jniLibs/<abi>/\` and rename them to \`libnetheal.so\`.
2. Open the project in **Android Studio**.
3. Perform a **Gradle Sync**.

## 4. CI/CD (Optional)

Push your changes to the \`main\` branch on GitHub. The included workflow (\`.github/workflows/android.yml\`) will automatically build both the Rust core and the Android APK, providing a downloadable artifact.

---
*NetHeal Mobile - Ultimate Autonomous Security*
