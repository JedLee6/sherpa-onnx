## 1. Add Lingua Dependency

- [x] 1.1 In `app/build.gradle.kts`, add `implementation("com.github.pemistahl:lingua:1.2.2")` to the dependencies block. Sync the project.

## 2. Set Up Language Mapping

- [x] 2.1 In `Languages.kt`, create a utility function `mapLinguaToIso1(language: com.github.pemistahl.lingua.api.Language): String?` to map detected languages to the `supertonic-3-tts` ISO 639-1 codes. Include common languages like English, Chinese, Japanese, Korean, German, Spanish, French, etc.

## 3. Implement Auto-Detection TTS Logic

- [x] 3.1 In `MainActivity.kt`, initialize a `LanguageDetector` lazily at the class level (to avoid blocking the UI on startup) built with the languages supported by `supertonic-3-tts`.
- [x] 3.2 In the "Start" button's `onClick` logic (inside `CoroutineScope(Dispatchers.Default).launch`), check if `TtsEngine.isSupertonic` is true.
- [x] 3.3 If true, use `BreakIterator.getSentenceInstance()` to split `testText` into a list of sentences.
- [x] 3.4 For each sentence, use `LanguageDetector.detectLanguageOf(sentence)`. Map the result to an ISO code using the utility from step 2.1. If mapping fails or returns null, fallback to `TtsEngine.supertonicLang`.
- [x] 3.5 Loop through the sentences sequentially. Create a new `GenerationConfig` for each sentence with its detected language, call `TtsEngine.tts!!.generateWithConfigAndCallback`, and append the generated audio samples to a list so they can be saved to the WAV file at the end.
- [x] 3.6 If `TtsEngine.isSupertonic` is false, keep the original behavior (generating the entire text at once).
- [x] 3.7 Ensure the generated combined audio can still be saved properly and the RTF metrics reflect the total time and audio duration for all sentences.
