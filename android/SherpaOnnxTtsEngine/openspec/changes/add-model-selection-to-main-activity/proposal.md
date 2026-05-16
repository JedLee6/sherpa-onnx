## Why

The current implementation has recently been optimized for the supertonic-3-tts model, but the application should be versatile enough to support multiple model architectures (VITS, Matcha, etc.). Users need a way to switch between these models to evaluate different voices and performance characteristics.

## What Changes

- Update `MainActivity` to include a model selection dropdown (ExposedDropdownMenuBox).
- Add support for switching between:
  - supertonic-3-tts (default)
  - vits-piper-zh_CN-xiao_ya-medium
  - vits-piper-zh_CN-chaowen-medium
  - matcha-icefall-zh-baker
- Update `TtsEngine` logic to handle the configuration and initialization of these specific models.
- Ensure the UI reflects the current model's capabilities (e.g., speaker ID range, language support).

## Capabilities

### New Capabilities
- `model-selection`: Provides the user interface and logic to dynamically switch between different TTS model types and specific pre-trained instances.

### Modified Capabilities
- None

## Impact

- `MainActivity`: UI dropdown and event handling.
- `TtsEngine`: Configuration logic and model loading.
- `PreferenceHelper`: Persisting the selected model across sessions.
