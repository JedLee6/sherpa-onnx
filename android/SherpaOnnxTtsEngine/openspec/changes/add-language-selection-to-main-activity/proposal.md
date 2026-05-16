## Why

The current implementation of the SherpaOnnxTtsEngine is configured to use a specific model (supertonic-3-tts) which supports 31 languages, but the language code is currently hardcoded or difficult to change via the UI. Users need a way to select their preferred language from the supported list to fully utilize the model's capabilities.

## What Changes

- Update MainActivity to include a language selection input field (e.g., a Dropdown/Spinner).
- Populate the selection field with the 31 languages supported by the supertonic-3-tts model.
- Update the logic in MainActivity to pass the selected language code to the TtsEngine.
- Ensure the TtsEngine correctly applies the selected language for speech synthesis.

## Capabilities

### New Capabilities
- language-selection: Provides a user interface and underlying logic to choose and apply any of the 31 supported language codes for the TTS engine.

### Modified Capabilities
- None

## Impact

- MainActivity: UI layout and logic for the selection field.
- TtsEngine: Configuration logic to handle dynamic language updates.
- strings.xml: (Optional) Localized names for the supported languages.
