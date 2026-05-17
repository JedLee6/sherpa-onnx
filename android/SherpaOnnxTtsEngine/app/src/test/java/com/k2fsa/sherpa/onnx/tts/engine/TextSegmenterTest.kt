package com.k2fsa.sherpa.onnx.tts.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TextSegmenterTest {

    @Test
    fun testPunctuationSplit() {
        val input = "Hello there. How are you, my friend? I am fine! Wait，what about Chinese commas、right? 中文句号测试。阿拉伯语测试؟"
        val output = TextSegmenter.splitText(input)
        assertEquals(listOf("Hello there.", "How are you,", "my friend?", "I am fine!", "Wait，", "what about Chinese commas、", "right?", "中文句号测试。", "阿拉伯语测试؟"), output)
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
}
