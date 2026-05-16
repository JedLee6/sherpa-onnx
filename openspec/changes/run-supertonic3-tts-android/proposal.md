## Why

The SherpaOnnxTtsEngine Android app is set up with the Supertonic3 TTS model files in its assets, but the pre-built native libraries (SO files) are missing from `jniLibs/`. Without these, the app cannot be built or run on a device.

## What Changes

- Copy `libonnxruntime.so` and `libsherpa-onnx-jni.so` for all 4 ABIs (arm64-v8a, armeabi-v7a, x86, x86_64) from the SherpaOnnxTts project's pre-built SO files into SherpaOnnxTtsEngine's `app/src/main/jniLibs/`
- Build the SherpaOnnxTtsEngine debug APK
- Install and launch the app on the connected Android phone via ADB

## Capabilities

### New Capabilities
- `supertonic3-tts-android`: Run the SherpaOnnxTtsEngine Android app with the pre-bundled Supertonic3 int8 TTS model on a physical device

### Modified Capabilities
- (none)

## Impact

- **Code**: `android/SherpaOnnxTtsEngine/app/src/main/jniLibs/` — adds SO files for all ABIs
- **Build**: `./gradlew installDebug` to install the APK on the connected device
- **Dependencies**: Requires Android NDK (for cmake), Android SDK, and a connected Android device with USB debugging enabled
