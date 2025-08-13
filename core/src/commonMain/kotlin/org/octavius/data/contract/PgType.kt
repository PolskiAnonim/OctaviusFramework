package org.octavius.data.contract

/**
 * Oznacza klasę `data class` lub `enum` jako typ danych, który może być mapowany
 * na niestandardowy typ w bazie danych PostgreSQL (np. typ kompozytowy lub enum).
 *
 * Ta adnotacja jest kluczowa dla `TypeRegistry`, które skanuje classpath w poszukiwaniu
 * oznaczonych klas, aby automatycznie zbudować mapowanie między typami Kotlina
 * a typami PostgreSQL.
 *
 * Główne zastosowania:
 * - Mapowanie `data class` na typy kompozytowe PostgreSQL.
 * - Mapowanie klas `enum` Kotlina na typy `ENUM` w PostgreSQL.
 *
 * **Konwencja nazewnicza:**
 * Domyślnie, nazwa typu w PostgreSQL jest wyliczana na podstawie prostej nazwy klasy
 * poprzez konwersję z `CamelCase` na `snake_case` (np. klasa `TestPerson` zostanie
 * zmapowana na typ `test_person`).
 *
 * **Jawne określenie nazwy:**
 * Można nadpisać domyślną nazwę, podając ją w parametrze [name]. Jest to przydatne,
 * gdy nazwa typu w bazie danych nie pasuje do konwencji.
 *
 * @param name Opcjonalna, jawna nazwa odpowiadającego typu w bazie danych PostgreSQL.
 *             Jeśli pozostawiona pusta, nazwa zostanie wygenerowana automatycznie
 *             zgodnie z konwencją `CamelCase` -> `snake_case`.
 *
 *
 * @sample
 * // Przykład 1: Użycie domyślnej konwencji nazewniczej
 * // Klasa `UserInfo` zostanie zmapowana na typ `user_info` w PostgreSQL.
 * @PgType
 * data class UserInfo(val id: Int, val username: String)
 *
 * // Przykład 2: Jawne określenie nazwy typu
 * // Klasa `OrderStatus` zostanie zmapowana na typ `order_status_enum` w PostgreSQL.
 * @PgType(name = "order_status_enum")
 * enum class OrderStatus { PENDING, COMPLETED, CANCELED }
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PgType(
    val name: String = ""
)