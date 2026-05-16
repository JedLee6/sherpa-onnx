## Context

SherpaOnnxTtsEngine is a TTS engine Android app that registers as a system TTS engine. It supports multiple TTS model types (VITS, Matcha, Kokoro, Supertonic) via commented-out example blocks in `TtsEngine.kt`'s `init` block. The Supertonic3 model and SO files are already in the project; only the configuration code needs to be enabled.

## Goals / Non-Goals

**Goals:**
- Enable Supertonic3 model by uncommenting Example 13 in `TtsEngine.kt`
- Build and run on the connected Android device

**Non-Goals:**
- Modifying the TTS synthesis logic (already correct)
- Adding new model types
- Changing SO library loading

## Decisions

**Single-file change**: Example 13 in `TtsEngine.kt` is the right place. The `init` block sets all the `OfflineTts` config fields (`modelDir`, `isSupertonic`, `durationPredictor`, `textEncoder`, `vectorEstimator`, `supertonicVocoder`, `ttsJson`, `unicodeIndexer`, `voiceStyle`, `supertonicLang`). All values map directly to files in the assets directory.

## Risks / Trade-offs

[Risk] Wrong path for model files → Mitigation: Model files in `assets/sherpa-onnx-supertonic-3-tts-int8-2026-05-11/` match `modelDir = "sherpa-onnx-supertonic-3-tts-int8-2026-05-11"` exactly.

[Risk] ABI mismatch → Mitigation: SO files are present for arm64-v8a (the dominant Android ABI); x86/armeabi are optional.
