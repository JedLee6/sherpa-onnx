## Context

The `matcha-icefall-zh-baker` model in the assets contains a `dict` directory with jieba dictionary files. The native `OfflineTts` engine requires the path to this directory to correctly process Chinese text.

## Goals / Non-Goals

**Goals:**
- Correctly configure and load the Matcha model.
- Ensure the `dictDir` is accessible to the native engine.

## Decisions

- **ModelConfig Update**: Add `dictDir` field.
- **TtsEngine Logic**:
  - Implement `dictDir` copying in `realInitTts`. Since it's a directory, we can reuse or adapt the `copyDataDir` logic.
  - Update `getOfflineTtsConfig` call to pass the resolved `dictDir`.
- **Matcha Configuration**: Set `dictDir = "matcha-icefall-zh-baker/dict"`.

## Risks / Trade-offs

- **Copying Overhead**: Copying the dictionary directory adds a small delay to the first-time initialization of the Matcha model.
