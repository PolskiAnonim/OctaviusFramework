package org.octavius.data

/**
 * Reprezentuje standardowe, wbudowane typy danych PostgreSQL.
 * Używany do bezpiecznego typowo określania typu w metodzie `withPgType`.
 *
 */
enum class PgStandardType(val typeName: String, val isArray: Boolean = false) {
    // --- Typy proste ---
    // Typy stałoprzecinkowe
    INT2("int2"),
    SMALLSERIAL("smallserial"),
    INT4("int4"),
    SERIAL("serial"),
    INT8("int8"),
    BIGSERIAL("bigserial"),
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
    INTERVAL("interval"),
    // Json
    JSON("json"),
    JSONB("jsonb"),
    // Inne
    BOOL("bool"),
    UUID("uuid"),
    BYTEA("bytea"),

    // --- Typy tablicowe (generowane automatycznie) ---
    INT2_ARRAY("_int2", true),
    SMALLSERIAL_ARRAY("_smallserial", true),
    INT4_ARRAY("_int4", true),
    SERIAL_ARRAY("_serial", true),
    INT8_ARRAY("_int8", true),
    BIGSERIAL_ARRAY("_bigserial", true),
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
    INTERVAL_ARRAY("_interval", true),
    JSON_ARRAY("_json", true),
    JSONB_ARRAY("_jsonb", true),
    BOOL_ARRAY("_bool", true),
    UUID_ARRAY("_uuid", true),
    BYTEA_ARRAY("_bytea", true)
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
