## Why

The SherpaOnnxTtsEngine Android app has the Supertonic3 TTS model files pre-bundled in `app/src/main/assets/sherpa-onnx-supertonic-3-tts-int8-2026-05-11/` and the native SO libraries in `app/src/main/jniLibs/`. However, the TTS engine configuration code in `TtsEngine.kt` has Example 13 (Supertonic3) fully written but commented out, preventing the app from using the bundled model.

## What Changes

- Uncomment Example 13 configuration block in `TtsEngine.kt` to enable the Supertonic3 TTS model
- Build the debug APK
- Install and run on the connected Android device

## Capabilities

### New Capabilities
- `supertonic3-tts-android`: SherpaOnnxTtsEngine runs the bundled Supertonic3 int8 TTS model

### Modified Capabilities
- (none)

## Impact

- **Code**: `TtsEngine.kt` — uncomment ~12 lines in the `init` block
- **Build**: `./gradlew assembleDebug` → install via `./gradlew installDebug`
- **Dependencies**: Supertonic3 model in assets, SO libraries in jniLibs (already present)
