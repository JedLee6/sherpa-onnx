## Context

The current `TtsEngine` initialization logic uses a series of commented-out examples. While effective for manual testing, it doesn't allow for runtime model switching. The goal is to move these configurations into a structured format and expose them via the UI.

## Goals / Non-Goals

**Goals:**
- Allow users to select from a list of predefined models in `MainActivity`.
- Automatically re-initialize the TTS engine upon model change.
- Persist the selected model using `PreferenceHelper`.

**Non-Goals:**
- Supporting arbitrary model paths provided by the user.
- Automatic downloading of missing models (this design assumes models are already present in assets).

## Decisions

- **Predefined Models**: Define a list of models with their specific configurations (modelDir, modelName, etc.).
- **TtsEngine Logic**: Update `initTts` to use the selected model's parameters instead of hardcoded values.
- **UI Component**: Add an `ExposedDropdownMenuBox` at the top of the `MainActivity` layout.
- **Integration**:
  - Add `modelState` to `TtsEngine` to track the current selection.
  - Call `updateTts` when a new model is selected.
  - Update `PreferenceHelper` to include `setModel` and `getModel`.

## Risks / Trade-offs

- **Memory Usage**: Re-initializing models might cause temporary spikes in memory usage.
- **Initialization Delay**: Switching between architectures (e.g., VITS to Matcha) might take longer than just switching languages within the same model.
