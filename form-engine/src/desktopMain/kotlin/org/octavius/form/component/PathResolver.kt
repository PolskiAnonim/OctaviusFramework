package org.octavius.form.component

import org.octavius.form.control.base.ControlContext

/**
 * Narzędzie do rozwiązywania ścieżek w hierarchii formularza.
 * Obsługuje ścieżki bezwzględne, względne (.) oraz wyjście w górę (..), 
 * wykorzystując strukturę drzewa [ControlContext].
 */
object PathResolver {
    //TODO lepsza logika oraz podróż w dół (zmiana pól we wszystkich wierszach repeatable)
    const val SEPARATOR = "/"
    const val PARENT = ".."
    const val CURRENT = "."

    /**
     * Rozwiązuje ścieżkę [path] względem podanego kontekstu [currentContext].
     *
     * Reguły:
     * - Ścieżka zaczynająca się od "/" (np. "/name") -> bezwzględna od korzenia.
     * - Ścieżka bez "/" i bez ".", ".." (np. "name") -> traktowana jako bezwzględna (kompatybilność wsteczna).
     * - Ścieżka zaczynająca się od "." (np. "./name") -> rodzeństwo w tym samym kontenerze.
     * - Ścieżka zaczynająca się od ".." (np. "../name") -> wyjście poziom wyżej w hierarchii kontenerów.
     */
    fun resolvePath(path: String, currentContext: ControlContext): String {
        // Ścieżka bezwzględna (zaczyna się od /)
        if (path.startsWith(SEPARATOR)) {
            return path.substring(1)
        }

        // Kompatybilność wsteczna: prosta nazwa bez separatorów i modyfikatorów to path bezwzględny
        if (!path.contains(SEPARATOR) && 
            !path.startsWith(CURRENT) && 
            !path.startsWith(PARENT)
        ) {
            return path
        }

        val targetSegments = path.split(SEPARATOR)
        var contextNode: ControlContext? = currentContext

        var i = 0
        while (i < targetSegments.size) {
            when (targetSegments[i]) {
                CURRENT -> {
                    // . oznacza pozostanie w tym samym kontenerze. 
                    // Ponieważ currentContext.statePath odnosi się już do kontenera,
                    // nie musimy wchodzić wyżej w drzewo ControlContext.
                }
                PARENT -> {
                    // .. oznacza wyjście do kontenera nadrzędnego
                    contextNode = contextNode?.parent
                }
                else -> {
                    // Dotarliśmy do właściwych nazw ścieżki
                    break
                }
            }
            i++
        }

        val remainingPath = targetSegments.drop(i).joinToString(SEPARATOR)

        // Konstruujemy bazową ścieżkę do kontenera, w którym ostatecznie wylądowaliśmy
        val basePath = contextNode?.statePath ?: ""

        return if (basePath.isEmpty()) {
            remainingPath
        } else if (remainingPath.isEmpty()) {
            basePath
        } else {
            "$basePath/$remainingPath"
        }
    }
}
