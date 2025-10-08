package org.octavius.data

/**
 * Reprezentuje standardowe, wbudowane typy danych PostgreSQL.
 * Używany do bezpiecznego typowo określania typu w metodzie `withPgType`.
 *
 */
enum class PgStandardType(val typeName: String, val isArray: Boolean = false) {
    // --- Typy proste ---
    // Typy stałoprzecinkowe
    SERIAL("serial"),
    BIGSERIAL("bigserial"),
    SMALLSERIAL("smallserial"),
    INT4("int4"),
    INT8("int8"),
    INT2("int2"),
    // Typy zmiennoprzecinkowe
    FLOAT4("float4"),
    FLOAT8("float8"),
    NUMERIC("numeric"),
    // Typy tekstowe
    TEXT("text"),
    VARCHAR("varchar"),
    CHAR("char"),
    // Data i czas
    DATE("date"),
    TIMESTAMP("timestamp"),
    TIMESTAMPTZ("timestamptz"),
    TIME("time"),
    TIMETZ("timetz"),
    // Json
    JSON("json"),
    JSONB("jsonb"),
    // Inne
    BOOL("bool"),
    UUID("uuid"),
    INTERVAL("interval"),

    // --- Typy tablicowe (generowane automatycznie) ---
    SERIAL_ARRAY("_serial", true),
    BIGSERIAL_ARRAY("_bigserial", true),
    SMALLSERIAL_ARRAY("_smallserial", true),
    INT4_ARRAY("_int4", true),
    INT8_ARRAY("_int8", true),
    INT2_ARRAY("_int2", true),
    FLOAT4_ARRAY("_float4", true),
    FLOAT8_ARRAY("_float8", true),
    NUMERIC_ARRAY("_numeric", true),
    TEXT_ARRAY("_text", true),
    VARCHAR_ARRAY("_varchar", true),
    CHAR_ARRAY("_char", true),
    DATE_ARRAY("_date", true),
    TIMESTAMP_ARRAY("_timestamp", true),
    TIMESTAMPTZ_ARRAY("_timestamptz", true),
    TIME_ARRAY("_time", true),
    TIMETZ_ARRAY("_timetz", true),
    JSON_ARRAY("_json", true),
    JSONB_ARRAY("_jsonb", true),
    BOOL_ARRAY("_bool", true),
    UUID_ARRAY("_uuid", true),
    INTERVAL_ARRAY("_interval", true);
}

/**
 * Opakowuje wartość, aby jawnie określić docelowy typ PostgreSQL.
 *
 * Powoduje dodanie rzutowania typu (`::pgType`) do wygenerowanego fragmentu SQL.
 * Przydatne do obsługi niejednoznaczności typów, np. przy tablicach.
 *
 * @param value Wartość do osadzenia w zapytaniu (należy unikać data class gdzie jest to dodawane automatycznie!).
 * @param pgType Nazwa typu PostgreSQL, na który wartość ma być rzutowana (np. "text[]", "jsonb").
 */
data class PgTyped(val value: Any?, val pgType: String)


/**
 * Opakowuje wartość w PgTyped, aby jawnie określić docelowy typ PostgreSQL
 * w sposób bezpieczny typowo.
 */
fun Any?.withPgType(pgType: PgStandardType): PgTyped = PgTyped(this, pgType.typeName)

/**
 * Opakowuje wartość w PgTyped w bardziej płynny sposób.
 * Używaj tej metody tylko dla niestandardowych lub rzadkich typów, które
 * nie są zdefiniowane w `PgStandardType`.
 *
 * @see PgStandardType
 */
fun Any?.withPgType(pgType: String): PgTyped = PgTyped(this, pgType)

enum class EnumCaseConvention {
    SNAKE_CASE_UPPER,  // MOJA_WARTOSC
    SNAKE_CASE_LOWER,  // moja_wartosc
    PASCAL_CASE,       // MojaWartosc
    CAMEL_CASE,        // mojaWartosc
    AS_IS              // MojaWartosc -> MojaWartosc
}

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
    val name: String = "",
    val enumConvention: EnumCaseConvention = EnumCaseConvention.SNAKE_CASE_UPPER
)