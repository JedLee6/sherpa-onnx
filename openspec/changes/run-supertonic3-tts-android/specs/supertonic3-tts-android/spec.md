## ADDED Requirements

### Requirement: SherpaOnnxTtsEngine ships with Supertonic3 TTS SO libraries
The SherpaOnnxTtsEngine Android project SHALL include `libonnxruntime.so` and `libsherpa-onnx-jni.so` in `app/src/main/jniLibs/` for all 4 ABIs: arm64-v8a, armeabi-v7a, x86, x86_64.

### Requirement: SherpaOnnxTtsEngine builds and runs on connected Android device
The SherpaOnnxTtsEngine debug APK SHALL be built and installed on the connected Android phone via `./gradlew installDebug` or `adb install`.

#### Scenario: All SO files present and build succeeds
- **WHEN** all 4 ABIs have their SO files in `jniLibs/` and `./gradlew assembleDebug` is run
- **THEN** the debug APK is successfully generated at `app/build/outputs/apk/debug/app-debug.apk`

#### Scenario: APK installs and launches on device
- **WHEN** `adb install app/build/outputs/apk/debug/app-debug.apk` is run with a device connected
- **THEN** the app appears in the launcher and opens without crashes

#### Scenario: TTS synthesizes speech
- **WHEN** the app is opened and a short text is entered and played
- **THEN** audio is generated and played through the device speaker
