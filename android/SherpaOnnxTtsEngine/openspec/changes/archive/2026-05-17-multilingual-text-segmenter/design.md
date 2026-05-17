## Context

The `auto-language-tts` feature dynamically reads mixed-language sentences. However, the standard `BreakIterator` lacks the nuance needed to handle complex punctuation (such as quotes), abbreviations ("Mr.", "Dr."), and boundaries between distinct character sets (like Chinese/Japanese/Korean and Latin characters). Without a custom segmenter, sentences are either split incorrectly (causing TTS to sound broken) or grouped together with wrong language detection logic.

## Goals / Non-Goals

**Goals:**
- Provide a robust method to split mixed-language text into proper sub-sentences.
- Handle common punctuation without breaking on abbreviations.
- Separate quoted or parenthetical text into its own sub-sentence.
- Intelligently split text boundaries between CJK (Chinese, Japanese, Korean) characters and Latin/ASCII characters.

**Non-Goals:**
- Provide perfect semantic NLP sentence parsing. The split relies on regex and structural rules rather than a deep learning language model.

## Decisions

**1. Rule-based Multi-pass Segmentation Pipeline**
- **Rationale**: A multi-pass approach using Regex is fast, lightweight, and easy to adjust. 
- **Passes**:
  1. **Punctuation & Exception Handling**: Split by end-of-sentence punctuation (`.!?;\n`) but avoid splitting when preceded by known abbreviations (e.g., `(?<!\b(?:Mr|Mrs|Ms|Dr|Prof|Sr|Jr))\s*\.\s*`).
  2. **Quote/Bracket Isolation**: Iterate over sentences and further split them when finding quotes (`""`, `''`, `「」`, `()`) so the quoted text becomes its own standalone segment.
  3. **Script Boundary Detection**: For each resulting segment, use regex to find boundaries between CJK scripts and Latin scripts. CJK blocks can be identified using Unicode ranges (`\p{IsHan}`, `\p{IsHiragana}`, `\p{IsKatakana}`, `\p{IsHangul}`). Latin words and spaces will be grouped separately.

**2. Creation of `TextSegmenter` Utility Class**
- **Rationale**: Keeps `MainActivity.kt` clean and allows the segmenter to be easily tested through Unit Tests. The function signature will be simple: `fun splitText(input: String): List<String>`.

## Risks / Trade-offs

- **Risk: Over-splitting on script boundaries** -> Text like "iPhone 15" could be split if numbers aren't grouped properly with Latin text. Mitigation: Ensure digits and spaces are grouped with Latin blocks when splitting against CJK blocks.
- **Risk: Abbreviations outside the predefined list** -> Unlisted abbreviations might still cause incorrect splits. Mitigation: Start with a common set of English abbreviations and allow easy expansion of the list if issues arise.
