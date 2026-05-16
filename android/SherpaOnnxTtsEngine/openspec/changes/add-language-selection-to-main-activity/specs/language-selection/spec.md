## ADDED Requirements

### Requirement: Language Selection Interface
The application must provide a clear way for the user to see and select from the list of supported languages.

#### Scenario: Display Supported Languages
- **WHEN** the user opens the application or navigates to the language selection section.
- **THEN** they should see a list containing 31 languages (English, Korean, Japanese, Arabic, etc.).

### Requirement: Update TTS Language
Selecting a language from the UI must update the TTS synthesis output.

#### Scenario: User selects a different language
- **WHEN** the user selects "Japanese (ja)" from the dropdown.
- **THEN** the TtsEngine should be updated to use "ja" for subsequent synthesis requests.

#### Scenario: Persistence of selection
- **WHEN** the user selects a language and restarts the application.
- **THEN** the previously selected language should be automatically loaded and active.
