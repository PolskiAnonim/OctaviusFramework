package org.octavius.data

/**
 * Prosty kontener na fragment zapytania SQL i jego parametry.
 *
 * Służy jako nośnik danych, który łączy SQL z wartościami w bezpieczny sposób,
 * zapobiegając SQL Injection. Nie narzuca żadnego stylu budowania zapytań.
 * Jest to de facto para (String, Map).
 *
 * @property sql Fragment kodu SQL, np. `status = :status OR user_id = ANY(:userIds)`.
 * @property params Mapa parametrów używanych w [sql].
 */
data class QueryFragment(
    val sql: String,
    val params: Map<String, Any?> = emptyMap()
)

/**
 * Łączy listę fragmentów w jeden, działając analogicznie do standardowego [joinToString],
 * ale z uwzględnieniem scalania parametrów.
 *
 * Postfix i prefix mają zastosowanie głównie dla [org.octavius.data.builder.RawQueryBuilder]. Preferowanym sposobem dodawania samych klauzul
 * jest użycie odpowiednich builderów
 *
 * 1.  **Bezpiecznie łączy mapy parametrów**, rzucając błąd w przypadku konfliktu kluczy.
 * 2.  Ignoruje puste fragmenty. Jeśli po filtrowaniu lista jest pusta, zwraca pusty QueryFragment
 *     (ignorując prefix i postfix, aby nie generować np. pustego "WHERE ()").
 * 3.  Otacza każdy *pojedynczy* fragment nawiasami dla zachowania priorytetów operatorów.
 *
 * @param separator Ciąg znaków oddzielający fragmenty, np. " AND ".
 * @param prefix Ciąg wstawiany na początku całego wynikowego łańcucha (np. "WHERE ").
 * @param postfix Ciąg wstawiany na końcu całego wynikowego łańcucha (np. ";").
 * @param addParenthesis Czy dodawać nawiasy wokół fragmentów - domyślnie true
 *
 * @return Nowy, połączony [QueryFragment].
 */
fun List<QueryFragment>.join(separator: String, prefix: String = "", postfix: String = "", addParenthesis: Boolean = true): QueryFragment {
    val significantFragments = this.filter { it.sql.isNotBlank() }
    // Jeśli nie ma znaczących fragmentów, zwracamy pusty obiekt.
    if (significantFragments.isEmpty()) {
        return QueryFragment("")
    }
    val finalSql = significantFragments.joinToString(separator, prefix, postfix) { if (addParenthesis) "(${it.sql})" else it.sql }

    val finalParams = mutableMapOf<String, Any?>()
    significantFragments.forEach { fragment ->
        fragment.params.forEach { (key, value) ->
            require(!finalParams.containsKey(key) || finalParams[key] == value) {
                "Duplicate parameter key '$key' with different values found while joining QueryFragments. " +
                        "Parameter names must be unique across all joined fragments."
            }
            finalParams[key] = value
        }
    }

    return QueryFragment(finalSql, finalParams)
}
