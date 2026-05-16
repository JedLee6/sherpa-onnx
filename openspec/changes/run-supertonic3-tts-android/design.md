## Context

The SherpaOnnxTtsEngine Android app targets text-to-speech via the Android TTS engine API. The Supertonic3 int8 TTS model (released 2026-05-11) is already bundled in `app/src/main/assets/sherpa-onnx-supertonic-3-tts-int8-2026-05-11/`. However, the JNI native libraries (`libonnxruntime.so` and `libsherpa-onnx-jni.so`) are absent from `app/src/main/jniLibs/`, blocking the build.

The SherpaOnnxTts project at `../SherpaOnnxTts/app/src/main/jniLibs/` already contains the pre-built SO files for all 4 ABIs.

## Goals / Non-Goals

**Goals:**
- Copy the pre-built SO libraries to SherpaOnnxTtsEngine
- Build a working debug APK
- Launch the app on the connected Android device

**Non-Goals:**
- Rebuilding SO files from source (use pre-built binaries)
- Modifying the Kotlin/Java source code
- Creating a release/production APK
- Supporting additional ABIs beyond the 4 present in SherpaOnnxTts

## Decisions

**Copy pre-built SO files from SherpaOnnxTts vs. building from source**

The SherpaOnnxTts project already has the exact same SO files needed. Building from source requires Android NDK and a full CMake build (~30+ min). The pre-built binaries from the existing project are identical in function.

**Decision**: Copy SO files from `../SherpaOnnxTts/app/src/main/jniLibs/` to `app/src/main/jniLibs/`.

## Risks / Trade-offs

[Risk] ABI mismatch → Mitigation: Ensure all 4 ABIs (arm64-v8a, armeabi-v7a, x86, x86_64) are copied so the app works across device architectures.

[Risk] SO file version mismatch with onnxruntime version → Mitigation: Both SherpaOnnxTts and SherpaOnnxTtsEngine use onnxruntime v1.24.3, confirmed in build scripts.
