## 1. Core Segmenter Utility

- [ ] 1.1 Create `TextSegmenter.kt` object with a main `splitText(input: String): List<String>` method.
- [ ] 1.2 Implement first pass logic in `TextSegmenter`: split text by terminal punctuation (`.`, `?`, `!`, `;`) while explicitly avoiding splits after known abbreviations (e.g., `Mr.`, `Dr.`, `Mrs.`).
- [ ] 1.3 Implement second pass logic: isolate quoted or bracketed strings (e.g., `""`, `''`, `「」`) so they form their own distinct segments.
- [ ] 1.4 Implement third pass logic: detect boundaries between CJK script characters (Han, Hiragana, Katakana, Hangul) and Latin characters/whitespace, and split the segments across these boundaries.

## 2. Integration into App

- [ ] 2.1 Modify `MainActivity.kt` where TTS audio generation happens. Replace `BreakIterator.getSentenceInstance(Locale.getDefault())` and the `while` loop logic with `TextSegmenter.splitText(testText)`.
- [ ] 2.2 Ensure the updated sentence list correctly iterates through the language detection and audio generation loop without breaking.

## 3. Unit Testing

- [ ] 3.1 Add local unit tests for `TextSegmenter` to verify abbreviation preservation (e.g., "Mr. Li").
- [ ] 3.2 Add tests to verify quote extraction (e.g., `""Guten morgen" means good morning in english."`).
- [ ] 3.3 Add tests to verify CJK/Latin splitting (e.g., `"apple有red apple 和 green apple这两种类型"`).
