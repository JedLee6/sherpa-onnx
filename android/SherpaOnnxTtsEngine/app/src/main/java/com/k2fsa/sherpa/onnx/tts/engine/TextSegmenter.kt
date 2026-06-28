package com.k2fsa.sherpa.onnx.tts.engine

object TextSegmenter {
    // Abbreviations that should not trigger a sentence split
    private val abbreviations = listOf("Mr", "Mrs", "Ms", "Dr", "Prof", "Sr", "Jr", "vs", "etc", "i.e", "e.g")
    
    // CJK ranges: Han ideographs, Hiragana, Katakana, Hangul syllables
    private const val CJK_PATTERN = "[\\u4e00-\\u9fa5\\u3040-\\u309f\\u30a0-\\u30ff\\uac00-\\ud7a3]"
    private const val LATIN_PATTERN = "[a-zA-Z0-9]"
    
    // Regex for quoting/brackets
    private val quoteRegex = Regex("(\"[^\"]*\"|'[^']*'|「[^」]*」|\\([^)]*\\))")

    fun splitText(input: String): List<String> {
        if (input.isBlank()) return emptyList()

        // Pass 1: Punctuation-based splitting excluding abbreviations
        val step1 = splitByPunctuation(input)

        // Pass 2: Quote/Bracket extraction
        val step2 = mutableListOf<String>()
        for (sentence in step1) {
            step2.addAll(splitByQuotes(sentence))
        }

        // Pass 3: CJK and Latin script boundary detection
        val step3 = mutableListOf<String>()
        for (sentence in step2) {
            step3.addAll(splitByScriptBoundary(sentence))
        }

        // Pass 4: Merge numbers if adjacent segments have the same language
        val step4 = mergeNumbersAndLanguages(step3)

        // Pass 5: Merge remaining standalone numbers with adjacent segments
        val step5 = mergeNumbers(step4)

        return step5.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun isNumber(s: String): Boolean {
        val trimmed = s.trim()
        if (trimmed.isEmpty()) return false
        val hasDigit = trimmed.any { it.isDigit() }
        val isAllNumberChars = trimmed.all { it.isDigit() || it.isWhitespace() || it in ".,+-%$￥" }
        return hasDigit && isAllNumberChars
    }

    private fun mergeNumbersAndLanguages(segments: List<String>): List<String> {
        if (segments.size < 3) return segments
        val result = ArrayList<String>(segments)
        
        var i = 1
        while (i < result.size - 1) {
            val current = result[i]
            if (isNumber(current)) {
                val prev = result[i - 1]
                val next = result[i + 1]
                val prevLang = Languages.detectCjkLanguage(prev) ?: "en"
                val nextLang = Languages.detectCjkLanguage(next) ?: "en"
                if (prevLang == nextLang) {
                    val merged = prev + current + next
                    result[i - 1] = merged
                    result.removeAt(i + 1)
                    result.removeAt(i)
                    i = Math.max(1, i - 1)
                    continue
                }
            }
            i++
        }
        return result
    }

    private fun mergeNumbers(segments: List<String>): List<String> {
        if (segments.isEmpty()) return segments
        val result = mutableListOf<String>()
        
        for (seg in segments) {
            if (isNumber(seg)) {
                if (result.isNotEmpty()) {
                    // Merge with the preceding segment
                    val lastIdx = result.size - 1
                    result[lastIdx] = result[lastIdx] + seg
                } else {
                    // No preceding segment, add it to result
                    result.add(seg)
                }
            } else {
                if (result.isNotEmpty() && isNumber(result.last())) {
                    // If the preceding segment was a standalone number (because it was the first element),
                    // merge it with the current segment
                    val lastIdx = result.size - 1
                    result[lastIdx] = result[lastIdx] + seg
                } else {
                    result.add(seg)
                }
            }
        }
        return result
    }

    // Global punctuation marks to split by
    private val splitChars = setOf(
        '.', '?', '!', ';', ',', '\n', // Latin/English
        '。', '？', '！', '；', '，', '、', // CJK (Chinese, Japanese, Korean)
        '؟', '؛', '،', // Arabic / Persian / Urdu
        '\u0964', '\u0965', // Devanagari (Hindi, Sanskrit, Nepali, etc.) Danda & Double Danda
        '\u104b', '\u104a', // Burmese Full Stop & Comma
        '\u0f0d', // Tibetan Shad
        '\u1362', // Ethiopic Full Stop
        '\u0589', // Armenian Full Stop
        '\u1803'  // Mongolian Full Stop
    )

    private val commas = setOf(',', '，', '、', '،', '\u104a')

    private fun countWords(text: String): Int {
        var count = 0
        val latinWordRegex = Regex("[a-zA-Z0-9]+")
        count += latinWordRegex.findAll(text).count()
        
        val cjkRegex = Regex(CJK_PATTERN)
        count += cjkRegex.findAll(text).count()
        return count
    }

    private fun splitByPunctuation(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        
        var i = 0
        while (i < input.length) {
            val c = input[i]
            sb.append(c)
            
            if (c in splitChars) {
                var shouldSplit = true
                
                // If it is a period or comma, check if it's flanked by digits (decimal point or thousands separator)
                if (c == '.' || c == ',') {
                    val isNumChar = i > 0 && i < input.length - 1 && input[i - 1].isDigit() && input[i + 1].isDigit()
                    if (isNumChar) {
                        shouldSplit = false
                    }
                }
                
                if (shouldSplit && c == '.') {
                    val currentStr = sb.toString().trim()
                    val lastWord = currentStr.substringBeforeLast(".").split(Regex("\\s+")).lastOrNull()?.trimEnd('.')
                    if (lastWord != null && abbreviations.any { it.equals(lastWord, ignoreCase = true) }) {
                        shouldSplit = false
                    }
                }
                
                if (shouldSplit) {
                    if (c in commas) {
                        val segmentText = sb.toString()
                        if (countWords(segmentText) > 4) {
                            tokens.add(segmentText)
                            sb.clear()
                        }
                    } else {
                        // End of sentence
                        tokens.add(sb.toString())
                        sb.clear()
                    }
                }
            }
            i++
        }
        if (sb.isNotEmpty()) {
            tokens.add(sb.toString())
        }
        
        return tokens
    }

    private fun splitByQuotes(input: String): List<String> {
        val results = mutableListOf<String>()
        var lastMatchEnd = 0
        
        val matches = quoteRegex.findAll(input)
        for (match in matches) {
            if (match.range.first > lastMatchEnd) {
                results.add(input.substring(lastMatchEnd, match.range.first))
            }
            results.add(match.value)
            lastMatchEnd = match.range.last + 1
        }
        
        if (lastMatchEnd < input.length) {
            results.add(input.substring(lastMatchEnd))
        }
        
        return results
    }

    private fun splitByScriptBoundary(input: String): List<String> {
        // Find boundaries between CJK and Latin/Digits
        // CJK followed by optional spaces then Latin: (?<=CJK)(?=\s*Latin)
        // Latin followed by optional spaces then CJK: (?<=Latin)(?=\s*CJK)
        
        val regex = Regex("(?<=$CJK_PATTERN)(?=\\s*$LATIN_PATTERN)|(?<=$LATIN_PATTERN)(?=\\s*$CJK_PATTERN)")
        return input.split(regex)
    }
}
