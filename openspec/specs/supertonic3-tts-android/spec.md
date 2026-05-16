## ADDED Requirements

### Requirement: Supertonic3 TTS model is enabled in TtsEngine
The SherpaOnnxTtsEngine SHALL load and use the Supertonic3 int8 TTS model from `assets/sherpa-onnx-supertonic-3-tts-int8-2026-05-11/` when the app starts.

#### Scenario: Example 13 uncommented enables Supertonic3
- **WHEN** Example 13 in `TtsEngine.kt`'s `init` block is uncommented
- **THEN** `TtsEngine.createTts()` creates an `OfflineTts` instance with Supertonic3 configuration

### Requirement: APK builds and installs on connected device
The debug APK SHALL be built and installed on the connected Android phone.

#### Scenario: Gradle build succeeds
- **WHEN** `./gradlew assembleDebug` is run from the SherpaOnnxTtsEngine directory
- **THEN** `app/build/outputs/apk/debug/app-debug.apk` is generated without errors

#### Scenario: App launches on device
- **WHEN** `./gradlew installDebug` installs the APK and the user opens the app
- **THEN** the app starts without crashing and shows the TTS configuration UI
