package org.octavius.report.column.type.number

/**
 * Funkcja fabryczna tworząca kolumnę do wyświetlania liczb zmiennoprzecinkowych.
 * Używa generycznej NumberColumn pod spodem.
 */
@Suppress("FunctionName")
fun DoubleColumn(
    header: String,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
    formatter: (Double?) -> String = { it?.toString() ?: "" }
) = NumberColumn(
    header = header,
    numberClass = Double::class,
    valueParser = { it.replace(",", ".").toDoubleOrNull() },
    width = width,
    sortable = sortable,
    filterable = filterable,
    formatter = formatter
)