## MODIFIED Requirements

### Requirement: Dynamic Model Loading
The engine must be robust against incorrect model configurations or missing assets.

#### Scenario: User selects a broken model
- **WHEN** the user selects a model that fails to initialize (e.g., missing files).
- **THEN** the application should not crash.
- **AND** the engine should attempt to fall back to the default "supertonic-3-tts" model.

#### Scenario: Application Startup with broken model
- **WHEN** the app starts and the persisted model configuration is invalid.
- **THEN** the app should successfully start (no white screen) by falling back to the default model.
