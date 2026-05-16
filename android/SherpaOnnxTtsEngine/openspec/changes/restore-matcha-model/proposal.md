## Why

The `matcha-icefall-zh-baker` model was commented out from `Models.supportedModels` because its vocoder file `vocos-22khz-univ.onnx` was missing from the app assets. This prevents users from seeing or selecting the Matcha model in the model dropdown.

The vocoder is a **shared file** that lives at the assets root level (not inside the model directory), as expected by the `getOfflineTtsConfig` function in `Tts.kt` (line 346: `vocoder = vocoder` without `$modelDir/` prefix). It must be downloaded separately from the sherpa-onnx releases.

## What Changes

1. **Download the vocoder**: Fetch `vocos-22khz-univ.onnx` from `https://github.com/k2-fsa/sherpa-onnx/releases/download/vocoder-models/vocos-22khz-univ.onnx` and place it at `app/src/main/assets/vocos-22khz-univ.onnx`.
2. **Restore the Matcha model**: Uncomment the `matcha-icefall-zh-baker` entry in `Models.kt` and ensure its `requiredFiles` list includes all necessary files (including the root-level vocoder).
3. **Add ruleFsts**: The Matcha zh-baker model has `date.fst`, `number.fst`, and `phone.fst` rule files in its assets that should be configured for proper Chinese text processing.

## Capabilities

### Modified Capabilities
- `model-selection`: Restores the Matcha zh-baker model to the selectable model list.

## Impact

- `app/src/main/assets/vocos-22khz-univ.onnx`: New file (downloaded).
- `Models.kt`: Uncomment and update Matcha model configuration.
