## 1. Infrastructure

- [x] 1.1 Create a new file `com/k2fsa/sherpa/onnx/tts/engine/Models.kt` to define supported models and their specific configurations (architecture, paths, etc.).
- [ ] 1.2 Add `setModel` and `getModel` methods to `PreferenceHelper.kt`.

## 2. TtsEngine Implementation

- [ ] 2.1 Update `TtsEngine.kt` to include a `modelState` and logic to set initialization parameters based on the current model.
- [ ] 2.2 Modify `initTts` in `TtsEngine.kt` to eliminate hardcoded model parameters and use the dynamic configuration.

## 3. UI Implementation

- [x] 3.1 Add a model selection `ExposedDropdownMenuBox` to the top of the `MainActivity.kt` layout.
- [x] 3.2 Connect the dropdown to `TtsEngine.modelState` and ensure `updateTts` is called on change.
- [x] 3.3 Add logic to automatically refresh the `numSpeakers` and test text when the model changes.

## 4. Testing and Integration

- [x] 4.1 Verify that each of the four models (Supertonic, 2x VITS, 1x Matcha) loads and synthesizes speech correctly.
- [x] 4.2 Confirm that the selected model is persisted and reloaded after closing and reopening the app.
