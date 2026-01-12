package org.octavius.database.type

/** Reprezentuje znaleziony w SQL nazwany parametr wraz z jego pozycją. */
internal data class ParsedParameter(val name: String, val startIndex: Int, val endIndex: Int)


/**
 * Parsuje zapytanie SQL w dialekcie PostgreSQL w celu znalezienia nazwanych parametrów (np. `:param`).
 *
 * Implementacja jest inspirowana `org.springframework.jdbc.core.namedparam.NamedParameterUtils`,
 * ale została dostosowana i rozszerzona, aby poprawnie obsługiwać specyficzne konstrukcje PostgreSQL,
 * takie jak "dollar-quoted string constants" (np. `$$...$$` lub `$tag$...$tag$`).
 *
 * Parser poprawnie ignoruje placeholdery znajdujące się wewnątrz:
 * - Komentarzy jednoliniowych (`-- ...`)
 * - Komentarzy wieloliniowych (`/* ... */`)
 * - Literałów tekstowych (`'...'`)
 * - Identyfikatorów w cudzysłowach (`"..."`)
 * - Dollar-quoted strings (`$$...$$`, `$tag$...$tag$`)
 */
internal object PostgresqlNamedParameterParser {

    private val PARAMETER_SEPARATORS = "\"':&,;()|=+-*%/\\<>^"
    private val separatorIndex = BooleanArray(128).apply {
        PARAMETER_SEPARATORS.forEach { this[it.code] = true }
    }

    private fun isParameterSeparator(c: Char): Boolean {
        return (c.code < 128 && separatorIndex[c.code]) || c.isWhitespace()
    }

    /**
     * Analizuje podany ciąg SQL i zwraca listę znalezionych parametrów w kolejności ich występowania.
     */
    fun parse(sql: String): List<ParsedParameter> {
        val foundParameters = mutableListOf<ParsedParameter>()
        val statement = sql.toCharArray()
        var i = 0
        while (i < statement.size) {
            if (statement[i] == '$') {
                val endPos = findDollarQuoteEnd(statement, i)
                if (endPos != -1) {
                    i = endPos + 1
                    continue
                }
            }

            when (statement[i]) {
                '\'' -> i = skipUntil(statement, i, '\'')
                '"' -> i = skipUntil(statement, i, '"')
                '-' -> if (i + 1 < statement.size && statement[i + 1] == '-') { i = skipUntil(statement, i, '\n') }
                '/' -> if (i + 1 < statement.size && statement[i + 1] == '*') { i = skipUntil(statement, i, "*/") }
            }

            if (i >= statement.size) break

            if (statement[i] == ':') {
                var j = i + 1
                if (j < statement.size && statement[j] == ':') {
                    i += 2
                    continue
                }
                while (j < statement.size && !isParameterSeparator(statement[j])) {
                    j++
                }
                if (j - i > 1) {
                    val paramName = sql.substring(i + 1, j)
                    foundParameters.add(ParsedParameter(paramName, i, j))
                    i = j - 1 // Ustawiamy i na ostatni znak parametru
                }
            }
            i++
        }
        return foundParameters
    }

    /** Przeskakuje do końca bloku dollar-quoted. */
    private fun findDollarQuoteEnd(statement: CharArray, start: Int): Int {
        if (start + 1 >= statement.size) return -1

        var tagEnd = start
        while (tagEnd + 1 < statement.size && statement[tagEnd + 1] != '$') {
            val char = statement[tagEnd + 1]
            if (!(char.isLetterOrDigit() || char == '_')) return -1
            tagEnd++
        }

        if (tagEnd + 1 >= statement.size || statement[tagEnd + 1] != '$') return -1

        val tag = statement.slice(start..tagEnd + 1).joinToString("")
        val endTagIndex = statement.joinToString("").indexOf(tag, startIndex = tagEnd + 2)

        return if (endTagIndex != -1) endTagIndex + tag.length - 1 else -1
    }

    /** Przeskakuje do następnego wystąpienia określonego znaku. */
    private fun skipUntil(statement: CharArray, start: Int, endChar: Char): Int {
        var i = start + 1
        while (i < statement.size) {
            if (statement[i] == endChar) {
                if (endChar == '\'' && i + 1 < statement.size && statement[i + 1] == '\'') {
                    i++
                } else {
                    return i
                }
            }
            i++
        }
        return i
    }

    /** Przeskakuje do następnego wystąpienia określonego ciągu znaków. */
    private fun skipUntil(statement: CharArray, start: Int, endSequence: String): Int {
        val endIdx = statement.joinToString("").indexOf(endSequence, startIndex = start + endSequence.length)
        return if (endIdx == -1) statement.size else endIdx + endSequence.length - 1
    }
}