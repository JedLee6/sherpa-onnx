## Why

Switching to any non-supertonic model (VITS or Matcha) causes an immediate crash, and the app stays on a white screen on subsequent launches. Deep code analysis reveals **three distinct root causes**:

### Bug 1: Matcha vocoder file missing from assets
The `matcha-icefall-zh-baker` configuration references `vocos-22khz-univ.onnx` as its vocoder, but **this file does not exist in the assets directory**. The native OfflineTts constructor will crash (native SIGSEGV, uncatchable by Java try-catch) when it cannot find this file.

### Bug 2: TtsService.onGetLanguage() crashes with NPE
`TtsService.onGetLanguage()` (line 86) uses `TtsEngine.lang!!` which throws a NullPointerException. Since the `init` block was cleared (models are now dynamic), `TtsEngine.lang` is `null` until `initTts()` runs. But `TtsService.onCreate()` calls `onLoadLanguage(TtsEngine.lang, ...)` before `initTts` has a chance to set `lang`, creating a chicken-and-egg problem.

### Bug 3: White screen on restart (persistence loop)
When the selected model crashes, it's already been persisted to SharedPreferences. On the next app launch, `MainActivity.onCreate()` calls `TtsEngine.createTts()` which tries to load the same broken model. Even with the try-catch fallback, a **native crash** (SIGSEGV from missing files) cannot be caught by Java exceptions, so the process is killed and the user sees a white screen indefinitely.

## What Changes

1. **Fix Matcha vocoder path**: The vocoder file must be placed inside the matcha model directory in assets, OR the model must be removed from the supported list until its vocoder is available. The simplest fix is to validate model files exist before attempting to load them.
2. **Fix TtsService NPE**: Make `onGetLanguage()` null-safe and ensure `TtsEngine.lang` is initialized to a sensible default before `TtsService.onCreate()` is called.
3. **Pre-validate model files**: Before calling the native `OfflineTts` constructor, validate that all required asset files exist. This prevents native crashes that Java try-catch cannot handle.
4. **Safe model switching**: Validate assets before persisting the model selection to SharedPreferences, so a broken model is never saved as the user's preference.

## Capabilities

### Modified Capabilities
- `model-selection`: Crash-safe model switching with asset pre-validation.

## Impact

- `Models.kt`: Add required file lists per model; remove or fix Matcha config.
- `TtsEngine.kt`: Add asset validation before initialization; fix startup flow.
- `TtsService.kt`: Make `onGetLanguage()` null-safe.
- `MainActivity.kt`: Validate model before persisting selection.
