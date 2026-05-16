## 1. Fix TtsService NPE

- [x] 1.1 In `TtsService.kt`, make `onGetLanguage()` null-safe by replacing `TtsEngine.lang!!` with `TtsEngine.lang ?: "eng"`.
- [x] 1.2 In `TtsService.onCreate()`, guard `onLoadLanguage` calls so they don't fail when `lang` is null.

## 2. Add Asset Pre-Validation

- [x] 2.1 Add a `requiredFiles` list to `ModelConfig` in `Models.kt` that lists the critical asset files each model needs.
- [x] 2.2 Add a `validateModelAssets(context: Context, config: ModelConfig): Boolean` method to `TtsEngine.kt` that checks all required files exist in assets before native init.
- [x] 2.3 Call `validateModelAssets` inside `realInitTts` BEFORE creating the `OfflineTts` instance. Throw an exception with a descriptive message if validation fails.

## 3. Fix Matcha Model Config

- [x] 3.1 Remove or fix the matcha-icefall-zh-baker model from `Models.supportedModels` since `vocos-22khz-univ.onnx` is missing from assets.

## 4. Safe Model Switching in MainActivity

- [x] 4.1 In `MainActivity.kt`, validate the model assets BEFORE persisting the model selection and calling `updateTts()`.
- [x] 4.2 Show a Toast message to the user if validation fails, and keep the current model.

## 5. Robust Startup

- [x] 5.1 In `TtsEngine.initTts`, after the fallback try-catch, if `tts` is still null, set `lang` to a safe default (`"eng"`) so that `TtsService` doesn't crash.
