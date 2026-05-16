## Why

Switching to the `matcha-icefall-zh-baker` model causes the application to crash. This is because the Matcha model for Chinese requires a dictionary directory (`dictDir`) for text tokenization (jieba), which is currently missing from the configuration. Without this directory, the engine fails to initialize or crashes during text processing.

## What Changes

- Update `ModelConfig` in `Models.kt` to include a `dictDir` property.
- Configure the `matcha-icefall-zh-baker` model with its corresponding `dict` directory.
- Update `TtsEngine.kt` to handle and copy the `dictDir` to external storage, ensuring the native layer can access it.
- Pass the `dictDir` to the `getOfflineTtsConfig` during initialization.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `model-selection`: Added support for Matcha models requiring dictionary directories.

## Impact

- `Models.kt`: Updated schema and configuration.
- `TtsEngine.kt`: Updated initialization logic to support `dictDir`.
