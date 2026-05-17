# auto-language-tts Specification

## Purpose
TBD - created by archiving change auto-detect-language-tts. Update Purpose after archive.
## Requirements
### Requirement: Mixed-language text playback using auto-detection
When using the `supertonic-3-tts` model, input text containing multiple sentences of different languages MUST be pronounced correctly by automatically switching the engine's language configuration for each sentence. The text MUST be segmented using the custom `TextSegmenter` instead of `BreakIterator`.

#### Scenario: User inputs mixed English and Chinese text
- **WHEN** the active model is `supertonic-3-tts`.
- **AND** the user inputs "Hello world. 你好世界。" and clicks "Start".
- **THEN** the text should be split into "Hello world." and "你好世界。" using the custom `TextSegmenter`.
- **AND** the language of the first sentence should be detected as English (`en`), and the second as Chinese (`zh`).
- **AND** the TTS engine should synthesize the first sentence with `lang="en"` and the second with `lang="zh"`, playing them sequentially.

#### Scenario: Fallback to manually selected language
- **WHEN** the language detector fails to detect a language confidently or the detected language is not supported by `supertonic-3-tts`.
- **THEN** the TTS engine should synthesize that sentence using the user's manually selected language from the dropdown menu (i.e., `TtsEngine.supertonicLang`).

