package org.octavius.data.annotation

import org.octavius.data.util.CaseConvention

/**
 * Oznacza klasę `enum` jako typ danych, który może być mapowany na typ `ENUM`
 * w bazie danych PostgreSQL.
 *
 * Ta adnotacja jest kluczowa dla `TypeRegistry`, które skanuje classpath w poszukiwaniu
 * oznaczonych klas, aby automatycznie zbudować mapowanie między typami Kotlina
 * a typami `ENUM` w PostgreSQL.
 *
 * **Konwencja nazewnicza:**
 * Domyślnie, nazwa typu w PostgreSQL jest wyliczana na podstawie prostej nazwy klasy
 * poprzez konwersję z `CamelCase` na `snake_case` (np. klasa `OrderStatus` zostanie
 * zmapowana na typ `order_status`). Wartości enuma są domyślnie mapowane na
 * `SNAKE_CASE_UPPER` (np. `PENDING` -> `PENDING`).
 *
 * **Jawne określenie nazwy:**
 * Można nadpisać domyślną nazwę typu, podając ją w parametrze [name].
 *
 * @param name Opcjonalna, jawna nazwa odpowiadającego typu w bazie danych PostgreSQL.
 *             Jeśli pozostawiona pusta, nazwa zostanie wygenerowana automatycznie
 *             zgodnie z konwencją `CamelCase` -> `snake_case`.
 * @param enumConvention Konwencja nazewnicza dla wartości enuma podczas mapowania
 *                       na odpowiedniki w PostgreSQL. Domyślnie `SNAKE_CASE_UPPER`.
 *
 * ### Przykłady
 * ```kotlin
 * // Przykład 1: Użycie domyślnej konwencji nazewniczej
 * // Klasa `OrderStatus` zostanie zmapowana na typ `order_status` w PostgreSQL.
 * // Wartości (PENDING, COMPLETED) zostaną zmapowane jako 'PENDING', 'COMPLETED'.
 * @PgEnum
 * enum class OrderStatus { PENDING, COMPLETED }
 *
 * // Przykład 2: Jawne określenie nazwy typu i inna konwencja wartości
 * // Klasa `PaymentMethod` zostanie zmapowana na typ `payment_method_enum`.
 * // Wartości (CreditCard) zostaną zmapowane jako 'credit_card'.
 * @PgEnum(name = "payment_method_enum", enumConvention = EnumCaseConvention.SNAKE_CASE_LOWER)
 * enum class PaymentMethod { CreditCard, BankTransfer }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PgEnum(
    val name: String = "",
    // Jak zapisane są wartości w bazie PostgreSQL (np. 'PENDING', 'in_progress')
    val pgConvention: CaseConvention = CaseConvention.SNAKE_CASE_UPPER,
    // Jak zapisane są wartości w klasie Enum w Kotlinie (np. Pending, InProgress)
    val kotlinConvention: CaseConvention = CaseConvention.PASCAL_CASE
)