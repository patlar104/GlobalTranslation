package com.example.globaltranslation.core.util

import com.example.globaltranslation.core.provider.BoundingBox
import com.example.globaltranslation.core.provider.TextBlock as CoreTextBlock

/**
 * Utility for filtering noise and grouping text blocks intelligently.
 * Pure Kotlin logic with no Android dependencies - fully testable.
 * Supports multi-script text (Latin, CJK, Devanagari, etc.)
 */
object TextBlockGroupingUtil {

    /**
     * Filters noise and groups nearby text blocks for better translation accuracy.
     * Uses advanced heuristics to filter out technical jargon, code, and gibberish.
     * Script-aware for non-Latin text (Chinese, Japanese, Korean, Devanagari).
     */
    fun filterAndGroup(blocks: List<CoreTextBlock>): List<CoreTextBlock> {
        // Step 1: Advanced noise filtering
        val filteredBlocks = blocks.filter { block ->
            val text = block.text.trim()

            // Detect the script of the text
            val script = detectScript(text)

            // Basic size checks
            if (text.length < 3) return@filter false
            if (block.boundingBox.width < 30 || block.boundingBox.height < 15) return@filter false

            // Must contain letters (or CJK characters)
            if (!text.any { it.isLetter() || isCJKCharacter(it) || isDevanagariCharacter(it) }) return@filter false

            // For non-Latin scripts, skip Latin-specific filters
            if (script == Script.CJK || script == Script.DEVANAGARI) {
                // Only apply basic filters for non-Latin scripts
                val symbolRatio = text.count {
                    !it.isLetterOrDigit() && !it.isWhitespace() && !isCJKCharacter(it) && !isDevanagariCharacter(it)
                } / text.length.toFloat()
                return@filter symbolRatio <= 0.3
            }

            // Latin-specific advanced filters:
            // 1. Filter code-like patterns (CamelCase, snake_case, file paths)
            if (isCodeLike(text)) return@filter false

            // 2. Filter technical gibberish (too many consonants, no vowels)
            if (isGibberish(text)) return@filter false

            // 3. Filter excessive punctuation or symbols
            val symbolRatio = text.count { !it.isLetterOrDigit() && !it.isWhitespace() } / text.length.toFloat()
            if (symbolRatio > 0.3) return@filter false

            // 4. Filter all-caps technical terms (unless it's a short acronym or normal sentence)
            if (isAllCapsNoise(text)) return@filter false

            // 5. Calculate text quality score
            val qualityScore = calculateTextQuality(text)
            qualityScore > 0.5 // Only keep high-quality text
        }

        if (filteredBlocks.isEmpty()) {
            return emptyList()
        }

        // Step 2: Sort blocks by reading order (top to bottom, left to right)
        val sortedBlocks = filteredBlocks.sortedWith(
            compareBy<CoreTextBlock> { it.boundingBox.top }
                .thenBy { it.boundingBox.left }
        )

        // Step 3: Group nearby blocks that likely belong together
        val groupedBlocks = mutableListOf<CoreTextBlock>()
        var currentGroup = mutableListOf(sortedBlocks.first())
        var currentBounds = sortedBlocks.first().boundingBox

        for (i in 1 until sortedBlocks.size) {
            val block = sortedBlocks[i]
            val blockBounds = block.boundingBox

            // Calculate if this block is close to the current group
            val verticalDistance = blockBounds.top - currentBounds.bottom
            val horizontalOverlap = calculateHorizontalOverlap(currentBounds, blockBounds)

            // Group if:
            // - Vertically close (within 1.5x line height)
            // - Has horizontal overlap OR very close horizontally
            val averageHeight = (currentBounds.height + blockBounds.height) / 2
            val shouldGroup = verticalDistance < averageHeight * 1.5 &&
                    (horizontalOverlap > 0.3 || isHorizontallyClose(currentBounds, blockBounds))

            if (shouldGroup) {
                // Add to current group
                currentGroup.add(block)
                currentBounds = expandBounds(currentBounds, blockBounds)
            } else {
                // Start new group
                if (currentGroup.isNotEmpty()) {
                    groupedBlocks.add(mergeTextBlocks(currentGroup, currentBounds))
                }
                currentGroup = mutableListOf(block)
                currentBounds = blockBounds
            }
        }

        // Add last group
        if (currentGroup.isNotEmpty()) {
            groupedBlocks.add(mergeTextBlocks(currentGroup, currentBounds))
        }

        return groupedBlocks
    }

    /**
     * Detects code-like patterns that shouldn't be translated.
     */
    private fun isCodeLike(text: String): Boolean {
        // Check for common code patterns
        val codePatterns = listOf(
            Regex("\\w+\\.\\w+"), // file.extension or class.method
            Regex("\\w+::\\w+"), // namespace::function
            Regex("[a-z]+[A-Z]"), // camelCase (but allow first word capitalized like "Hello")
            Regex("_[a-z]+_"), // _snake_case_
            Regex("\\w+\\.kt|\\w+\\.java|\\w+\\.xml"), // file extensions
            Regex("fun |class |val |var |import "), // code keywords
            Regex("[{}\\[\\]<>;]"), // code brackets
            Regex("@\\w+"), // annotations
            Regex("\\w+\\(\\)") // function calls
        )

        return codePatterns.any { it.containsMatchIn(text) }
    }

    /**
     * Detects gibberish text (no vowels, too many consonants).
     */
    private fun isGibberish(text: String): Boolean {
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) return true

        val vowels = letters.count { it.lowercaseChar() in "aeiouäöüáéíóúàèìòùâêîôû" }
        val consonantRatio = (letters.length - vowels) / letters.length.toFloat()

        // If more than 80% consonants, likely gibberish
        return consonantRatio > 0.8
    }

    /**
     * Detects all-caps noise (technical terms, variable names).
     */
    private fun isAllCapsNoise(text: String): Boolean {
        if (text.length <= 4) return false // Short acronyms are OK (USA, NATO)

        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) return false

        val upperRatio = letters.count { it.isUpperCase() } / letters.length.toFloat()

        // If text is all caps and longer than 4 chars, likely technical noise
        // Unless it contains spaces (then it's probably a sign/title)
        return upperRatio > 0.9 && !text.contains(' ')
    }

    /**
     * Calculates text quality score (0.0 to 1.0).
     * Higher score = more likely to be natural language.
     */
    private fun calculateTextQuality(text: String): Float {
        var score = 1.0f

        // Penalize very short text
        if (text.length < 5) score -= 0.2f

        // Penalize text without spaces (unless it's a single word)
        if (text.length > 15 && !text.contains(' ')) score -= 0.3f

        // Reward balanced upper/lowercase
        val letters = text.filter { it.isLetter() }
        if (letters.isNotEmpty()) {
            val upperRatio = letters.count { it.isUpperCase() } / letters.length.toFloat()
            if (upperRatio in 0.1f..0.3f) score += 0.2f // Good capitalization pattern
        }

        // Reward presence of vowels
        val vowelRatio = letters.count { it.lowercaseChar() in "aeiou" } / letters.length.toFloat()
        if (vowelRatio in 0.2f..0.5f) score += 0.2f // Natural vowel distribution

        // Penalize excessive numbers
        val digitRatio = text.count { it.isDigit() } / text.length.toFloat()
        if (digitRatio > 0.3) score -= 0.3f

        return score.coerceIn(0.0f, 1.0f)
    }

    /**
     * Calculates horizontal overlap between two bounding boxes (0.0 to 1.0).
     */
    private fun calculateHorizontalOverlap(box1: BoundingBox, box2: BoundingBox): Float {
        val overlapStart = maxOf(box1.left, box2.left)
        val overlapEnd = minOf(box1.right, box2.right)
        val overlap = maxOf(0, overlapEnd - overlapStart)
        val minWidth = minOf(box1.width, box2.width)
        return if (minWidth > 0) overlap.toFloat() / minWidth else 0f
    }

    /**
     * Checks if two bounding boxes are horizontally close (within reasonable distance).
     */
    private fun isHorizontallyClose(box1: BoundingBox, box2: BoundingBox): Boolean {
        val horizontalGap = if (box1.right < box2.left) {
            box2.left - box1.right
        } else if (box2.right < box1.left) {
            box1.left - box2.right
        } else {
            0
        }
        val averageWidth = (box1.width + box2.width) / 2
        return horizontalGap < averageWidth * 0.5 // Within half the average width
    }

    /**
     * Expands a bounding box to include another.
     */
    private fun expandBounds(box1: BoundingBox, box2: BoundingBox): BoundingBox {
        return BoundingBox(
            left = minOf(box1.left, box2.left),
            top = minOf(box1.top, box2.top),
            right = maxOf(box1.right, box2.right),
            bottom = maxOf(box1.bottom, box2.bottom)
        )
    }

    /**
     * Merges multiple text blocks into one.
     */
    private fun mergeTextBlocks(blocks: List<CoreTextBlock>, bounds: BoundingBox): CoreTextBlock {
        val combinedText = blocks.joinToString(" ") { it.text.trim() }
        val combinedLines = blocks.flatMap { it.lines }
        return CoreTextBlock(
            text = combinedText,
            boundingBox = bounds,
            lines = combinedLines
        )
    }

    /**
     * Enum representing different text scripts.
     */
    private enum class Script {
        LATIN,
        CJK,         // Chinese, Japanese, Korean
        DEVANAGARI,  // Hindi, Bengali, etc.
        OTHER
    }

    /**
     * Detects the primary script of the text.
     */
    private fun detectScript(text: String): Script {
        var cjkCount = 0
        var devanagariCount = 0
        var latinCount = 0

        for (char in text) {
            when {
                isCJKCharacter(char) -> cjkCount++
                isDevanagariCharacter(char) -> devanagariCount++
                char.isLetter() -> latinCount++
            }
        }

        val total = cjkCount + devanagariCount + latinCount
        if (total == 0) return Script.OTHER

        return when {
            cjkCount > total / 2 -> Script.CJK
            devanagariCount > total / 2 -> Script.DEVANAGARI
            latinCount > total / 2 -> Script.LATIN
            else -> Script.OTHER
        }
    }

    /**
     * Checks if a character is CJK (Chinese, Japanese, Korean).
     */
    private fun isCJKCharacter(char: Char): Boolean {
        val codePoint = char.code
        return codePoint in 0x4E00..0x9FFF ||  // CJK Unified Ideographs
                codePoint in 0x3400..0x4DBF ||  // CJK Unified Ideographs Extension A
                codePoint in 0x3040..0x309F ||  // Hiragana
                codePoint in 0x30A0..0x30FF ||  // Katakana
                codePoint in 0xAC00..0xD7AF     // Hangul Syllables
    }

    /**
     * Checks if a character is Devanagari script (Hindi, Bengali, etc.).
     */
    private fun isDevanagariCharacter(char: Char): Boolean {
        val codePoint = char.code
        return codePoint in 0x0900..0x097F ||  // Devanagari
                codePoint in 0x0980..0x09FF      // Bengali
    }
}

