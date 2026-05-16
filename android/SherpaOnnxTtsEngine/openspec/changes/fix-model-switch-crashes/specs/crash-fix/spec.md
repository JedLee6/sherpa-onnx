## MODIFIED Requirements

### Requirement: Safe Model Loading
The engine must validate model assets before attempting native initialization.

#### Scenario: Model with missing files
- **WHEN** a model configuration references files not present in assets.
- **THEN** the validation should fail before any native code is invoked.
- **AND** the engine should log the error and remain on the current working model.

### Requirement: Null-safe TtsService
The TtsService must handle null or uninitialized TtsEngine state gracefully.

#### Scenario: TtsService starts before TtsEngine is initialized
- **WHEN** `TtsService.onGetLanguage()` is called and `TtsEngine.lang` is null.
- **THEN** the service should return a safe default (e.g., `["eng", "", ""]`).
- **AND** the service should NOT throw a NullPointerException.

### Requirement: Crash-safe model switching
- **WHEN** the user selects a model from the dropdown.
- **THEN** assets are validated BEFORE the selection is persisted to SharedPreferences.
- **AND** if validation fails, the user is informed and the current model is kept.
