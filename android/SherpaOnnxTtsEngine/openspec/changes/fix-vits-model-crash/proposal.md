## Why

Users have reported that switching to the VITS models (e.g., xiao_ya-medium) causes an immediate application crash. Furthermore, since the selected model is persisted, the application continues to crash on startup, leading to a "white screen" state that the user cannot recover from without clearing app data. This is caused by incorrect model configurations and lack of error handling during engine initialization.

## What Changes

- Correct the `ModelConfig` for VITS and Matcha models in `Models.kt` to use `lexicon.txt` instead of non-existent `espeak-ng-data` directories.
- Implement a safety mechanism in `TtsEngine.initTts` to catch initialization exceptions.
- Add a fallback mechanism: if the selected model fails to load, the engine will attempt to load the default `supertonic-3-tts` model.
- If all initialization attempts fail, the engine will remain in an uninitialized state rather than crashing the application.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `model-selection`: Improved stability and error recovery for model switching.

## Impact

- `Models.kt`: Updated configurations for VITS and Matcha models.
- `TtsEngine.kt`: Added try-catch blocks and fallback logic in `initTts`.
