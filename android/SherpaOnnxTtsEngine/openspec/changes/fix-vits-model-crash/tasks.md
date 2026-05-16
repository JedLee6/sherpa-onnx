## 1. Model Configuration Fix

- [x] 1.1 Update `com/k2fsa/sherpa/onnx/tts/engine/Models.kt` to remove `dataDir` and add `lexicon` for VITS and Matcha models.

## 2. Robust Initialization

- [x] 2.1 Update `TtsEngine.kt` to wrap `initTts` in a try-catch block.
- [x] 2.2 Implement fallback logic in `initTts` to attempt loading the default model if the current one fails.
- [ ] 2.3 Ensure `PreferenceHelper` is updated to the fallback model ID when a recovery occurs.

## 3. Testing

- [ ] 3.1 Verify that switching to VITS models no longer causes a crash.
- [ ] 3.2 Verify that a simulated crash (e.g., by providing a non-existent model ID) triggers the fallback to Supertonic.
