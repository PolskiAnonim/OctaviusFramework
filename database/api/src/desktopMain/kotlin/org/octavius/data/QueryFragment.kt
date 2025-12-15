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
 * Łączy listę fragmentów w jeden, używając podanego separatora.
 *
 * 1.  **Bezpiecznie łączy mapy parametrów**, rzucając błąd w przypadku konfliktu kluczy.
 *     To zapobiega cichemu nadpisywaniu parametrów.
 * 2.  Ignoruje puste fragmenty, upraszczając logikę w miejscu wywołania.
 * 3.  Dla pewności otacza każdy fragment nawiasami, co gwarantuje poprawną kolejność operacji.
 *
 * @param separator Ciąg znaków używany do oddzielenia części SQL, np. " AND ".
 * @return Nowy, połączony [QueryFragment].
 */
fun List<QueryFragment>.join(separator: String): QueryFragment {
    val significantFragments = this.filter { it.sql.isNotBlank() }
    if (significantFragments.isEmpty()) {
        return QueryFragment("")
    }

    val finalSql = significantFragments.joinToString(separator) { "(${it.sql})" }

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
