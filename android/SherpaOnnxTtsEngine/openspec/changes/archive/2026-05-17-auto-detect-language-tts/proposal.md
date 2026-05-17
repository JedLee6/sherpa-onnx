## Why

The `supertonic-3-tts` model supports 31 languages. Currently, the user has to manually select the language from a dropdown, and the entire input text is synthesized using that single language setting. If the text contains multiple languages (e.g., an English sentence followed by a Korean sentence), the pronunciation of the non-selected language will be very poor. 

To provide a seamless multilingual experience, the app should automatically detect the language of each sentence in the text and configure the TTS engine dynamically before playing it.

## What Changes

1.  **Add Lingua Dependency**: Add `com.github.pemistahl:lingua:1.2.2` to the app's `build.gradle.kts` for offline language detection.
2.  **Sentence Splitting**: In `MainActivity.kt`, when the user clicks "Start" and the active model is `supertonic-3-tts`, use `BreakIterator.getSentenceInstance()` to split the input text into individual sentences.
3.  **Language Detection & Mapping**: For each sentence, use the Lingua `LanguageDetector` to detect its language. Map the Lingua `Language` enum to the corresponding ISO 639-1 code used by `supertonic-3-tts`.
4.  **Sequential Generation and Playback**: Iterate through the sentences, generate the audio for each sentence with its specifically detected language configuration, and send the audio samples to the playback channel sequentially.
5.  **Fallback Mechanism**: If the language is not confidently detected or not supported by the model, fall back to the user's selected language in the dropdown.

## Capabilities

### New Capabilities
- `auto-language-detection`: Automatically detects the language of input text per sentence.
- `mixed-language-tts`: Supports generating TTS for text containing multiple languages by switching the engine's language configuration on the fly.

### Modified Capabilities
- `tts-generation`: Modified to handle a queue of sentences with dynamic configurations instead of a single block of text for the `supertonic` model.

## Impact

-   **`build.gradle.kts`**: New dependency added.
-   **`MainActivity.kt`**: Substantial changes to the "Start" button's coroutine logic to handle splitting, detection, and sequential generation.
-   **Performance**: Language detection is fast, but adding it will slightly increase the initial processing time before the first audio chunk is played. Lingua's models will also add a small amount of memory overhead.
