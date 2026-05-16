## Context

The crash involves three layers: the UI (MainActivity), the engine (TtsEngine/TtsService), and the native JNI layer (OfflineTts). The native layer crashes with SIGSEGV when files are missing, which is uncatchable by Java try-catch. The existing fallback mechanism only protects against Java-level exceptions.

## Goals / Non-Goals

**Goals:**
- Eliminate all crash paths when switching models.
- Ensure the app always starts successfully regardless of persisted model state.
- Add pre-validation of model assets before native initialization.

**Non-Goals:**
- Auto-downloading missing model files.
- Fixing the vocoder file packaging (that's a build/release concern).

## Decisions

1. **Asset Pre-Validation**: Add a `validateAssets(context, config)` method to `TtsEngine` that checks all required files exist in the asset manager before attempting native initialization. This catches missing files BEFORE they reach the native layer.

2. **Matcha Model Handling**: Since `vocos-22khz-univ.onnx` is missing from assets, remove the matcha model from `Models.supportedModels` until the file is available. Alternatively, if the vocoder should be inside the model directory, update the path.

3. **TtsService Null-Safety**: Change `onGetLanguage()` to handle null `lang` gracefully. Initialize `TtsEngine.lang` with a default value.

4. **Model Switch Flow**: Change the model switching flow to:
   a. Validate assets for the new model first.
   b. Only persist to SharedPreferences if validation passes.
   c. Only then call `updateTts()`.

## Risks / Trade-offs

- Removing the Matcha model from the UI means users can't select it, but this is better than a crash.
- Asset validation adds a small overhead on model switch, but it's negligible compared to model loading time.
