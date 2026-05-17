# multilingual-text-segmenter Specification

## Purpose
TBD - created by archiving change multilingual-text-segmenter. Update Purpose after archive.
## Requirements
### Requirement: Punctuation-based splitting with abbreviation preservation
The segmenter MUST split sentences using standard terminal punctuation (`.`, `?`, `!`, `;`) but SHALL NOT split when the period is part of a standard abbreviation (e.g., `Mr.`, `Dr.`, `Mrs.`).

#### Scenario: Split with normal punctuation
- **WHEN** the input is `"Hello there. How are you?"`
- **THEN** it should be split into `["Hello there.", "How are you?"]`

#### Scenario: Do not split on abbreviation
- **WHEN** the input is `"Mr. Li went to the store."`
- **THEN** it should remain a single segment `["Mr. Li went to the store."]`

### Requirement: Quote extraction
The segmenter MUST isolate text contained within quotes (`""`, `''`, `「」`) or brackets as a standalone segment.

#### Scenario: Sentence with quotes
- **WHEN** the input is `""Guten morgen" means good morning in english."`
- **THEN** it should be split into `["\"Guten morgen\"", " means good morning in english."]`

### Requirement: CJK and Latin script boundary detection
The segmenter MUST intelligently split strings at the boundaries between CJK (Chinese, Japanese, Korean) blocks and Latin/Whitespace blocks.

#### Scenario: Mixed CJK and Latin text
- **WHEN** the input is `"apple有red apple 和 green apple这两种类型"`
- **THEN** it should be split into `["apple", "有", "red apple ", "和 ", "green apple", "这两种类型"]` (or trimmed equivalents).

