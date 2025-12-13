package org.octavius.data.annotation



/**
 * Oznacza klasę `data class` jako typ danych, który może być mapowany
 * na typ kompozytowy w bazie danych PostgreSQL.
 *
 * Ta adnotacja jest kluczowa dla `TypeRegistry`, które skanuje classpath w poszukiwaniu
 * oznaczonych klas, aby automatycznie zbudować mapowanie między klasami Kotlina
 * a typami kompozytowymi PostgreSQL.
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
 * ### Przykłady
 * ```kotlin
 * // Przykład 1: Użycie domyślnej konwencji nazewniczej
 * // Klasa `UserInfo` zostanie zmapowana na typ `user_info` w PostgreSQL.
 * @PgComposite
 * data class UserInfo(val id: Int, val username: String)
 *
 * // Przykład 2: Jawne określenie nazwy typu
 * // Klasa `AddressDetails` zostanie zmapowana na typ `address_type` w PostgreSQL.
 * @PgComposite(name = "address_type")
 * data class AddressDetails(val street: String, val city: String)
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PgComposite(
    val name: String = ""
)