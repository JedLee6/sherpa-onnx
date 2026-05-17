## Why

The current standard `BreakIterator.getSentenceInstance` is insufficient for accurately parsing complex, mixed-language texts into segmented sentences suitable for multi-language TTS playback. We need a robust, custom text segmenter capable of splitting text based on punctuation (periods, quotes, parentheses), preventing erroneous splits on special cases (e.g., abbreviations like "Mr."), extracting quoted phrases, and intelligently segmenting mixed text between Latin characters and CJK (Chinese, Japanese, Korean) characters.

## What Changes

- Remove reliance on `BreakIterator.getSentenceInstance` for sentence splitting in `MainActivity.kt`.
- Develop a custom global "Multilingual TTS Text Segmenter" component/method.
- Introduce smart logic for handling abbreviations that use periods (e.g., "Mr.", "Dr.") to avoid premature splitting.
- Support segmenting text at boundaries like quotes (e.g., extracting `"Guten morgen"`).
- Implement intelligent boundary detection based on character sets (specifically between CJK scripts and other character types) to split distinct linguistic segments properly (e.g., "apple有red apple" -> "apple", "有", "red apple").

## Capabilities

### New Capabilities
- `multilingual-text-segmenter`: A comprehensive text splitting utility designed to handle punctuation rules, edge cases (abbreviations), and CJK/Latin character boundary detection.

### Modified Capabilities
- `auto-language-tts`: Update the requirement to use the custom segmenter instead of `BreakIterator`.

## Impact

- `MainActivity.kt`: Code related to TTS generation looping will change its text segmentation dependency.
- New utility class (e.g., `TextSegmenter.kt`) added for text processing.
- Will require extensive unit tests to guarantee proper splitting logic for edge cases and CJK/Latin variations.
