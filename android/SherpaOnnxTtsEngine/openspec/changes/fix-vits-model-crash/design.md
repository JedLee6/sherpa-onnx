## Context

The crash occurs because `getOfflineTtsConfig` or the `OfflineTts` constructor throws an exception when specified files (like `espeak-ng-data`) are missing from the assets or the copied location. The VITS models in the assets contain `lexicon.txt` and `tokens.txt` but not the `espeak-ng-data` directory required for Piper-style VITS.

## Goals / Non-Goals

**Goals:**
- Fix the VITS model configuration in `Models.kt`.
- Prevent application-level crashes during engine initialization.
- Provide a recovery path (fallback) when a model is corrupted or missing.

**Non-Goals:**
- Automatically downloading missing `espeak-ng-data`.

## Decisions

- **Models Configuration**: Use `lexicon.txt` for the provided VITS models as they follow the standard VITS asset structure found in the repository.
- **Robust Initialization**:
  - In `TtsEngine.initTts`, wrap the entire initialization logic in a `try { ... } catch (e: Exception) { ... }`.
  - Inside the catch block, log the error and recursively call `initTts` with the default model ID if the failed model wasn't already the default.
  - Reset `PreferenceHelper` to the default model if a fallback occurs.

## Risks / Trade-offs

- **Recursive Fallback**: Must ensure the fallback logic doesn't create an infinite loop if the default model is also broken.
