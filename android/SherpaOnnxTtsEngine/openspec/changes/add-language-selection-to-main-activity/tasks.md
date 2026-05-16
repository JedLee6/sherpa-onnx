## 1. Data Preparation

- [x] 1.1 Create a `Languages` object in a new file `com/k2fsa/sherpa/onnx/tts/engine/Languages.kt` containing a list of the 31 supported languages and their codes.
- [x] 1.2 Add string resources for the language names if necessary (or keep them in the `Languages` object for simplicity).

## 2. TtsEngine Updates

- [x] 2.1 Modify `TtsEngine.kt` to allow external updates to the `supertonicLang` property.
- [x] 2.2 Implement a method in `TtsEngine` to re-initialize the TTS engine when the language is changed.

## 3. UI Implementation

- [x] 3.1 Update `app/src/main/res/layout/activity_main.xml` to include a dropdown/selection field for languages (Implemented in MainActivity.kt Compose UI).
- [x] 3.2 Add styling to the new UI components to match the existing design.

## 4. Integration and Logic

- [x] 4.1 In `MainActivity.kt`, initialize the language selection field with the list from `Languages.kt`.
- [x] 4.2 Implement a selection listener that updates `TtsEngine.supertonicLang` and triggers a re-initialization.
- [x] 4.3 Add `SharedPreferences` logic to persist and load the user's language choice across sessions.
- [x] 4.4 Test the implementation with several languages (e.g., English, Korean, Japanese) to ensure correct behavior.
