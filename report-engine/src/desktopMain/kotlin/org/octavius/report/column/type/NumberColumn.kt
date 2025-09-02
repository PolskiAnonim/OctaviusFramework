package org.octavius.report.column.type

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.octavius.report.CellRendererUtils
import org.octavius.report.ColumnWidth
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.Filter
import org.octavius.report.filter.type.NumberFilter
import java.math.BigDecimal
import kotlin.reflect.KClass

/**
 * Uniwersalna kolumna do wyświetlania danych numerycznych w raporcie z formatowaniem i filtrowaniem.
 * 
 * Automatycznie wyrównuje zawartość do prawej strony (zgodnie z konwencjami numerycznymi) 
 * i obsługuje wszystkie typy numeryczne poprzez generyczne parametry.
 *
 * @param T Typ numeryczny rozszerzający Number (Int, Double, BigDecimal, Float, Long, etc.)
 * @param header Nagłówek kolumny wyświetlany w tabeli
 * @param numberClass Klasa typu numerycznego używana do deserializacji filtrów
 * @param valueParser Funkcja konwertująca tekst wprowadzony przez użytkownika na typ T dla potrzeb filtrowania.
 *                    Powinna zwracać null dla nieprawidłowych wartości
 * @param width Względna szerokość kolumny (domyślnie 1.0 = równa proporcja z innymi kolumnami)
 * @param sortable Określa czy kolumna może być sortowana przez użytkownika (domyślnie true)
 * @param filterable Określa czy kolumna obsługuje filtrowanie numeryczne z operatorami porównania (domyślnie true)
 * @param formatter Funkcja formatująca wartość numeryczną do wyświetlenia. Otrzymuje nullable T i zwraca string.
 *                  Powinna obsługiwać przypadek gdy wartość jest null
 */
class NumberColumn<T : Number>(
    header: String,
    private val numberClass: KClass<T>,
    private val valueParser: (String) -> T?,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
    private val formatter: (T?) -> String
) : ReportColumn(header, ColumnWidth.Flexible(width), filterable, sortable) {

    override fun createFilter(): Filter<*> {
        return NumberFilter(numberClass, valueParser)
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        @Suppress("UNCHECKED_CAST")
        val value = item as? T

        CellRendererUtils.StandardCellWrapper(
            modifier = modifier,
            alignment = Alignment.CenterEnd
        ) {
            Text(
                text = formatter(value),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right
            )
        }
    }
}

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

/**
 * Funkcja fabryczna tworząca kolumnę do wyświetlania liczb całkowitych typu Long.
 * Używa generycznej NumberColumn pod spodem.
 */
@Suppress("FunctionName")
fun LongColumn(
    header: String,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
    formatter: (Long?) -> String = { it?.toString() ?: "" }
) = NumberColumn(
    header = header,
    numberClass = Long::class,
    valueParser = { it.toLongOrNull() },
    width = width,
    sortable = sortable,
    filterable = filterable,
    formatter = formatter
)