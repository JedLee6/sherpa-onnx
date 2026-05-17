## Context

The `supertonic-3-tts` model requires the `lang` parameter to be set in the `GenerationConfig`'s `extra` map (e.g., `genConfig.extra = mapOf("lang" to "en")`). Currently, the app takes the entire text from the input field and generates it in one go using the manually selected language. To support mixed-language text, we need to break the text into smaller chunks (sentences) and detect the language for each chunk. 

We will use `java.text.BreakIterator.getSentenceInstance()` for sentence splitting and the `pemistahl/lingua` library for language detection. Lingua is highly accurate for short texts, which is ideal for sentence-level detection.

## Goals / Non-Goals

**Goals:**
- Split input text into sentences.
- Accurately detect the language of each sentence.
- Generate and queue audio for each sentence sequentially with the correct language configuration.
- Map Lingua's detected `Language` enum to the ISO 639-1 codes used by `supertonic-3-tts` (defined in `Languages.kt`).

**Non-Goals:**
- Applying this feature to models other than `supertonic-3-tts`.
- Real-time streaming generation (we will still generate per-sentence and enqueue, which is pseudo-streaming).

## Decisions

-   **Lingua Integration**: We will add `implementation("com.github.pemistahl:lingua:1.2.2")` to `app/build.gradle.kts`. We will initialize the `LanguageDetector` lazily or on a background thread to avoid blocking the UI, as it takes a moment to load its models.
-   **Sentence Splitting**: We will use `BreakIterator.getSentenceInstance(Locale.getDefault())`. It's built into Android and handles basic punctuation well.
-   **Language Mapping**: We will create an extension function or utility to map `com.github.pemistahl.lingua.api.Language` to our `Languages.kt` codes (e.g., `Language.ENGLISH` -> `"en"`, `Language.CHINESE` -> `"zh"`). If a detected language is not in `Languages.supportedLanguages`, we will fall back to the user's manually selected language.
-   **Sequential Generation**: In `MainActivity.kt`, instead of calling `generateWithConfigAndCallback` once, we will loop over the sentences. For each sentence, we create a new `GenerationConfig` with the detected language, call `generateWithConfigAndCallback`, and append the generated audio to a list (for saving) while the callback handles playing it via the `samplesChannel`.

## Risks / Trade-offs

-   **Initialization Delay**: Lingua's `LanguageDetectorBuilder.fromAllLanguages().build()` can take a few seconds to load data into memory. We should build it with a restricted set of languages (only those supported by `supertonic-3-tts`) to speed up initialization and reduce memory usage.
-   **Latency**: Generating sentence-by-sentence might introduce small gaps in playback if generation is slower than real-time. However, since the audio is streamed via a channel, as long as generation outpaces playback, it should be smooth.
