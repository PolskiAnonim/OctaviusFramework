package org.octavius.database.type

/** Represents a named parameter found in SQL with its position. */
internal data class ParsedParameter(val name: String, val startIndex: Int, val endIndex: Int)


/**
 * Parses PostgreSQL SQL queries to find named parameters (e.g., `:param`).
 *
 * Implementation is inspired by `org.springframework.jdbc.core.namedparam.NamedParameterUtils`,
 * but has been adapted and extended to correctly handle PostgreSQL-specific constructs.
 *
 * The parser correctly ignores placeholders found inside:
 * - Single-line comments (`-- ...`)
 * - Multi-line comments (`/* ... */`)
 * - String literals (`'...'`)
 * - Escape string literals (`E'...'` or `e'...'`) with backslash escapes
 * - Quoted identifiers (`"..."`)
 * - Dollar-quoted strings (`$$...$$`, `$tag$...$tag$`)
 * - Type casting operators (`::`)
 *
 * **PostgreSQL-Specific Features**:
 * - **Escape Strings**: `E'text with \n newline'` - properly handles backslash escape sequences
 * - **Dollar Quoting**: `$$text$$` or `$tag$text$tag$` - supports custom tags for avoiding quote escaping
 * - **Type Casting**: `column::integer` - distinguishes `::` operator from `:param` syntax
 *
 * This prevents false parameter detection inside string literals, which is critical for
 * complex SQL queries containing PostgreSQL-specific syntax.
 */
internal object PostgresNamedParameterParser {

    private const val PARAMETER_SEPARATORS = "\"':&,;()|=+-*%/\\<>^"
    private val separatorIndex = BooleanArray(128).apply {
        PARAMETER_SEPARATORS.forEach { this[it.code] = true }
    }

    private fun isParameterSeparator(c: Char): Boolean {
        return (c.code < 128 && separatorIndex[c.code]) || c.isWhitespace()
    }

    /**
     * Analyzes the given SQL string and returns a list of found parameters in order of occurrence.
     */
    fun parse(sql: String): List<ParsedParameter> {

        val foundParameters = mutableListOf<ParsedParameter>()
        val statement = sql.toCharArray()
        var i = 0
        while (i < statement.size) {
            when (statement[i]) {
                '\'' -> {
                    // Check if this is an E'...' escape string literal
                    i = if (i > 0 && (statement[i - 1] == 'E' || statement[i - 1] == 'e')) {
                        skipBackslashEscapedLiteral(statement, i)
                    } else {
                        // Regular literal ('...' or U&'...')
                        skipUntil(statement, i, '\'')
                    }
                }
                ':' -> {
                    // Potential parameter or type casting operator `::`
                    if (i + 1 < statement.size && statement[i + 1] == ':') {
                        // This is the type casting operator '::', ignore it
                        i++ // Skip the second ':'
                    } else {
                        var j = i + 1
                        while (j < statement.size && !isParameterSeparator(statement[j])) {
                            j++
                        }
                        if (j - i > 1) { // Found parameter name (longer than 0 characters)
                            val paramName = sql.substring(i + 1, j)
                            foundParameters.add(ParsedParameter(paramName, i, j))
                            i = j - 1 // Set i to the last character of the parameter
                        }
                    }
                }
                '"' -> i = skipUntil(statement, i, '"')
                '-' -> if (i + 1 < statement.size && statement[i + 1] == '-') {
                    i = skipUntil(statement, i, '\n')
                }
                '/' -> if (i + 1 < statement.size && statement[i + 1] == '*') {
                    i = skipComment(statement, i)
                }
                // Check for dollar-quote last, as it's less common
                '$' -> {
                    val endPos = findDollarQuoteEnd(statement, i)
                    if (endPos != -1) {
                        i = endPos
                    }
                }
            }
            i++
        }
        return foundParameters
    }

    /** Skips to the end of a dollar-quoted block. Returns the index of the last character. */
    private fun findDollarQuoteEnd(statement: CharArray, start: Int): Int {
        if (start + 1 >= statement.size) return -1

        // 1. Find the opening tag (e.g., $tag$)
        // Tag must follow unquoted identifier rules: [a-zA-Z_][a-zA-Z0-9_]* (no dollar signs)
        var tagEnd = start
        while (tagEnd + 1 < statement.size && statement[tagEnd + 1] != '$') {
            val char = statement[tagEnd + 1]

            // Validate tag character according to PostgreSQL unquoted identifier rules
            if (!isValidTagCharacter(char, isFirstChar = tagEnd == start)) {
                return -1 // Invalid tag character, not a valid dollar-quote
            }

            tagEnd++
        }

        if (tagEnd + 1 >= statement.size || statement[tagEnd + 1] != '$') {
            return -1 // Complete opening tag not found
        }

        val tagLength = (tagEnd + 1) - start + 1

        // 2. Search for the closing tag
        var searchPos = tagEnd + 2
        while (searchPos + tagLength <= statement.size) {
            if (regionMatches(statement, searchPos, statement, start, tagLength)) {
                return searchPos + tagLength - 1
            }
            searchPos++
        }

        return -1 // Closing tag not found
    }

    /**
     * Checks if a character is valid for a dollar-quote tag.
     * Tags follow PostgreSQL unquoted identifier rules:
     * - First character: [a-zA-Z_]
     * - Subsequent characters: [a-zA-Z0-9_]
     * - Dollar signs are NOT allowed in tags
     */
    private fun isValidTagCharacter(char: Char, isFirstChar: Boolean): Boolean {
        return when {
            char in 'a'..'z' || char in 'A'..'Z' || char == '_' -> true
            !isFirstChar && char in '0'..'9' -> true
            else -> false
        }
    }

    private fun skipBackslashEscapedLiteral(statement: CharArray, start: Int): Int {
        var i = start + 1
        while (i < statement.size) {
            if (statement[i] == '\\') {
                // This is an escape character, ignore the next character
                i++
            } else if (statement[i] == '\'') {
                // This is the end of the literal
                return i
            }
            i++
        }
        return i
    }

    /** Skips to the next occurrence of the specified character. Returns its index. */
    private fun skipUntil(statement: CharArray, start: Int, endChar: Char): Int {
        var i = start + 1
        while (i < statement.size) {
            if (statement[i] == endChar) {
                return i
            }
            i++
        }
        return i
    }

    /** Skips comment. */
    private fun skipComment(statement: CharArray, start: Int): Int {
        var i = start + 2 // skip initial /*
        var depth = 1
        while (i < statement.size && depth > 0) {
            if (i + 1 < statement.size) {
                if (statement[i] == '/' && statement[i + 1] == '*') {
                    depth++
                    i++
                } else if (statement[i] == '*' && statement[i + 1] == '/') {
                    depth--
                    i++
                }
            }
            i++
        }
        return i - 1
    }

    /**
     * Checks whether a region in the `source` array matches a region in the `target` array.
     */
    private fun regionMatches(source: CharArray, sourceOffset: Int, target: CharArray, targetOffset: Int, len: Int): Boolean {
        if (sourceOffset < 0 || targetOffset < 0 || sourceOffset + len > source.size || targetOffset + len > target.size) {
            return false
        }
        for (i in 0 until len) {
            if (source[sourceOffset + i] != target[targetOffset + i]) {
                return false
            }
        }
        return true
    }
}