package org.octavius.report.column.type.number

import java.math.BigDecimal


/**
 * Funkcja fabryczna tworząca kolumnę do wyświetlania liczb o wysokiej precyzji (BigDecimal).
 * Używa generycznej NumberColumn pod spodem.
 */
@Suppress("FunctionName")
fun BigDecimalColumn(
    header: String,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
    formatter: (BigDecimal?) -> String = { it?.toPlainString() ?: "" }
) = NumberColumn(
    header = header,
    numberClass = BigDecimal::class,
    valueParser = { it.replace(",", ".").toBigDecimalOrNull() },
    width = width,
    sortable = sortable,
    filterable = filterable,
    formatter = formatter
)