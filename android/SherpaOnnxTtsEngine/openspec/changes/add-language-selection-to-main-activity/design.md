## Context

The SherpaOnnxTtsEngine Android application provides a user interface to interact with the Sherpa-ONNX TTS engine. The engine recently switched to the supertonic-3-tts model, which supports 31 different languages. Currently, the language is set to a default (English) and cannot be easily changed by the end-user through the UI.

## Goals / Non-Goals

**Goals:**
- Implement a user-friendly language selection interface in MainActivity.
- Ensure the selection updates the TtsEngine configuration dynamically.
- Support all 31 languages provided by the supertonic-3-tts model.

**Non-Goals:**
- Adding support for other models besides supertonic-3-tts.
- Implementing multi-voice selection for each language (if applicable) in this phase.

## Decisions

- **UI Component**: Use a Spinner or a TextInputLayout with a MaterialAutoCompleteTextView (Dropdown) for a modern Material Design look.
- **Language List**: Create a data structure (e.g., a List of Pairs or a Map) containing the language names and their corresponding ISO 639-1 codes.
- **Integration**: Update TtsEngine.kt to allow setting the supertonicLang property dynamically.
- **Persistence**: Save the selected language in SharedPreferences so it persists across application restarts.

## Risks / Trade-offs

- **Model Reloading**: Depending on how the underlying C++ engine handles language changes, it might require re-initializing the TTS instance. This could introduce a small delay when switching languages.
- **UI Space**: Adding a new dropdown might clutter the MainActivity if not placed carefully.
