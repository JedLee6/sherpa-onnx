## ADDED Requirements

### Requirement: Model Selection Interface
The application must allow users to choose from a list of available TTS models.

#### Scenario: Display available models
- **WHEN** the user interacts with the model selection dropdown.
- **THEN** they should see options for "supertonic-3-tts", "xiao_ya-medium", "chaowen-medium", and "zh-baker".

### Requirement: Dynamic Model Loading
Changing the selected model must re-initialize the TTS engine with the correct configuration for that model's architecture.

#### Scenario: Switch from Supertonic to VITS
- **WHEN** the user selects "xiao_ya-medium".
- **THEN** the engine should load the VITS architecture with the specified piper model and data directory.

#### Scenario: Switch from VITS to Matcha
- **WHEN** the user selects "zh-baker".
- **THEN** the engine should load the Matcha architecture with the specified acoustic model and vocos vocoder.

### Requirement: Model Persistence
The selected model must be remembered across application sessions.

#### Scenario: App restart
- **WHEN** the user selects a non-default model and restarts the app.
- **THEN** the app should initialize the engine with that model automatically.
