package org.octavius.report.column.type.number

/**
 * Funkcja fabryczna tworząca kolumnę do wyświetlania liczb całkowitych.
 * Używa generycznej NumberColumn pod spodem.
 */
@Suppress("FunctionName")
fun IntegerColumn(
    header: String,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
    formatter: (Int?) -> String = { it?.toString() ?: "" }
) = NumberColumn(
    header = header,
    numberClass = Int::class,
    valueParser = { it.toIntOrNull() },
    width = width,
    sortable = sortable,
    filterable = filterable,
    formatter = formatter
)