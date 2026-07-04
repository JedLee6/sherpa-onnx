package com.jed.supertonic.tts.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TextSegmenterTest {

    @Test
    fun testPunctuationSplit() {
        val input = "Hello there. How are you, my friend? I am fine! Wait，what about Chinese commas、right? 中文句号测试。阿拉伯语测试؟ 印地语测试। 缅甸语测试။"
        val output = TextSegmenter.splitText(input)
        assertEquals(listOf(
            "Hello there.", "How are you, my friend?", "I am fine!",
            "Wait，what about Chinese commas、", "right?", "中文句号测试。",
            "阿拉伯语测试؟", "印地语测试।", "缅甸语测试။"
        ), output)
    }

    @Test
    fun testAbbreviationPreservation() {
        val input = "Mr. Li went to the store. Dr. Smith is here."
        val output = TextSegmenter.splitText(input)
        assertEquals(listOf("Mr. Li went to the store.", "Dr. Smith is here."), output)
    }

    @Test
    fun testQuoteExtraction() {
        val input = "\"Guten morgen\" means good morning in english."
        val output = TextSegmenter.splitText(input)
        assertEquals(listOf("\"Guten morgen\"", "means good morning in english."), output)
    }

    @Test
    fun testCjkLatinSplitting() {
        val input = "apple有red apple 和 green apple这两种类型"
        val output = TextSegmenter.splitText(input)
        assertEquals(listOf("apple", "有", "red apple", "和", "green apple", "这两种类型"), output)
    }

    @Test
    fun testComplexMixed() {
        val input = "She said \"こんにちは\". Mr. Wang replied \"Hello\"."
        val output = TextSegmenter.splitText(input)
        // Pass 1: "She said "こんにちは"." , "Mr. Wang replied "Hello"."
        // Pass 2: "She said ", ""こんにちは"", "." , "Mr. Wang replied ", ""Hello"", "."
        // Pass 3 (CJK check): "She said", ""こんにちは"", ".", "Mr. Wang replied", ""Hello"", "."
        // Trimming and filtering empty...
        assertEquals(listOf("She said", "\"こんにちは\"", ".", "Mr. Wang replied", "\"Hello\"", "."), output)
    }

    @Test
    fun testNumberMerging() {
        // 1. Prioritize merging with both preceding and succeeding when they share the same language: "我买了3个苹果" -> ["我买了3个苹果"]
        val input1 = "我买了3个苹果"
        val output1 = TextSegmenter.splitText(input1)
        assertEquals(listOf("我买了3个苹果"), output1)

        // 2. If no preceding sentence, merge with succeeding sentence: "3个苹果" -> ["3个苹果"]
        val input2 = "3个苹果"
        val output2 = TextSegmenter.splitText(input2)
        assertEquals(listOf("3个苹果"), output2)

        // 3. Same language on both sides: "苹果 123.45 桔子" -> ["苹果 123.45 桔子"]
        val input3 = "苹果 123.45 桔子"
        val output3 = TextSegmenter.splitText(input3)
        assertEquals(listOf("苹果 123.45 桔子"), output3)

        // 4. Percentage with succeeding: "100% 的人" -> ["100% 的人"]
        val input4 = "100% 的人"
        val output4 = TextSegmenter.splitText(input4)
        assertEquals(listOf("100% 的人"), output4)

        // 5. Different languages on both sides (do not merge together): "苹果 123.45 apple" -> ["苹果", "123.45 apple"]
        val input5 = "苹果 123.45 apple"
        val output5 = TextSegmenter.splitText(input5)
        assertEquals(listOf("苹果", "123.45 apple"), output5)

        // 6. Same language "en" on both sides: "apple 123.45 orange" -> ["apple 123.45 orange"]
        val input6 = "apple 123.45 orange"
        val output6 = TextSegmenter.splitText(input6)
        assertEquals(listOf("apple 123.45 orange"), output6)

        // 7. Recursive merging of multiple numbers: "我买了3或4个苹果" -> ["我买了3或4个苹果"]
        val input7 = "我买了3或4个苹果"
        val output7 = TextSegmenter.splitText(input7)
        assertEquals(listOf("我买了3或4个苹果"), output7)
    }

    @Test
    fun testDirectCjkLanguageDetection() {
        assertEquals("zh", Languages.detectCjkLanguage("你好，世界！"))
        assertEquals("ja", Languages.detectCjkLanguage("こんにちは、世界！"))
        assertEquals("ko", Languages.detectCjkLanguage("안녕하세요, 세계!"))
        assertEquals(null, Languages.detectCjkLanguage("Hello World!"))
    }
}
