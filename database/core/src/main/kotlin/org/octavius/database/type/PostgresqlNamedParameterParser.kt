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

    private const val PARAMETER_SEPARATORS = "\"':&,;()|=+-*%/\\<>^"
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
            when (statement[i]) {
                '\'' -> {
                    // Sprawdzamy, czy to literał typu E'...'
                    i = if (i > 0 && (statement[i - 1] == 'E' || statement[i - 1] == 'e')) {
                        skipBackslashEscapedLiteral(statement, i)
                    } else {
                        // Zwykły literał ('...' lub U&'...'),
                        skipUntil(statement, i, '\'')
                    }
                }
                ':' -> {
                    // Potencjalny parametr lub operator rzutowania `::`
                    if (i + 1 < statement.size && statement[i + 1] == ':') {
                        // To jest operator rzutowania '::', ignorujemy
                        i++ // Przeskakujemy drugie ':'
                    } else {
                        var j = i + 1
                        while (j < statement.size && !isParameterSeparator(statement[j])) {
                            j++
                        }
                        if (j - i > 1) { // Znaleziono nazwę parametru (dłuższą niż 0 znaków)
                            val paramName = sql.substring(i + 1, j)
                            foundParameters.add(ParsedParameter(paramName, i, j))
                            i = j - 1 // Ustawiamy i na ostatni znak parametru
                        }
                    }
                }
                '"' -> i = skipUntil(statement, i, '"')
                '-' -> if (i + 1 < statement.size && statement[i + 1] == '-') {
                    i = skipUntil(statement, i, '\n')
                }
                '/' -> if (i + 1 < statement.size && statement[i + 1] == '*') {
                    i = skipUntil(statement, i, "*/")
                }
                // Sprawdzanie dollar-quote na końcu, bo jest rzadsze
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

    /** Przeskakuje do końca bloku dollar-quoted. Zwraca indeks ostatniego znaku. */
    private fun findDollarQuoteEnd(statement: CharArray, start: Int): Int {
        if (start + 1 >= statement.size) return -1

        // 1. Znajdź tag otwierający (np. $tag$)
        var tagEnd = start
        while (tagEnd + 1 < statement.size && statement[tagEnd + 1] != '$') {
            tagEnd++
        }

        if (tagEnd + 1 >= statement.size || statement[tagEnd + 1] != '$') {
            return -1 // Nie znaleziono pełnego tagu otwierającego
        }

        val tagLength = (tagEnd + 1) - start + 1

        // 2. Szukaj tagu zamykającego
        var searchPos = tagEnd + 2
        while (searchPos + tagLength <= statement.size) {
            if (regionMatches(statement, searchPos, statement, start, tagLength)) {
                return searchPos + tagLength - 1
            }
            searchPos++
        }

        return -1 // Nie znaleziono tagu zamykającego
    }

    private fun skipBackslashEscapedLiteral(statement: CharArray, start: Int): Int {
        var i = start + 1
        while (i < statement.size) {
            if (statement[i] == '\\') {
                // To jest znak ucieczki, zignoruj następny znak
                i++
            } else if (statement[i] == '\'') {
                // To jest koniec literału
                return i
            }
            i++
        }
        return i
    }

    /** Przeskakuje do następnego wystąpienia określonego znaku. Zwraca jego indeks. */
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

    /** Przeskakuje do następnego wystąpienia określonego ciągu znaków. Zwraca indeks ostatniego znaku sekwencji. */
    private fun skipUntil(statement: CharArray, start: Int, endSequence: String): Int {
        val endChars = endSequence.toCharArray()
        var i = start + endChars.size
        while (i < statement.size) {
            if (regionMatches(statement, i - endChars.size + 1, endChars, 0, endChars.size)) {
                return i
            }
            i++
        }
        return statement.size
    }

    /**
     * NOWA FUNKCJA POMOCNICZA: Sprawdza, czy region w tablicy `source` pasuje do regionu w tablicy `target`.
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