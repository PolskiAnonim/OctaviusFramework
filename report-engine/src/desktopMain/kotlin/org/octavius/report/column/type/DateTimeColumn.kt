package org.octavius.report.column.type

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.octavius.report.CellRendererUtils
import org.octavius.report.ColumnWidth
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.Filter
import org.octavius.report.filter.type.DateTimeFilter
import org.octavius.util.*
import org.octavius.data.OffsetTime
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Generyczna kolumna do wyświetlania danych daty i/lub czasu w raporcie.
 * Wykorzystuje adapter `DateTimeAdapter` do formatowania i obsługi różnych typów daty/czasu.
 *
 * @param T Typ daty/czasu (np. LocalDate, LocalDateTime, Instant, LocalTime, OffsetTime).
 * @param header Nagłówek kolumny
 * @param kClass Klasa typu T, używana do deserializacji filtra.
 * @param adapter Adapter specyficzny dla typu T, służący do formatowania i konwersji.
 * @param width Względna szerokość kolumny (domyślnie 1.0)
 * @param sortable Czy kolumna obsługuje sortowanie (domyślnie true)
 * @param filterable Czy kolumna obsługuje filtrowanie (domyślnie true)
 */
class DateTimeColumn<T : Any>(
    header: String,
    private val kClass: KClass<T>,
    private val adapter: DateTimeAdapter<T>,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
) : ReportColumn(header, ColumnWidth.Flexible(width), filterable, sortable) {

    override fun createFilter(): Filter<*> {
        return DateTimeFilter(kClass, adapter)
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
                text = adapter.format(value),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right
            )
        }
    }
}

// --- Funkcje fabryczne dla konkretnych typów daty/czasu ---

/**
 * Funkcja fabryczna tworząca kolumnę do wyświetlania dat (LocalDate).
 */
@Suppress("FunctionName")
fun DateColumn(
    header: String,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
) = DateTimeColumn(
    header = header,
    kClass = LocalDate::class,
    adapter = DateAdapter,
    width = width,
    sortable = sortable,
    filterable = filterable,
)

/**
 * Funkcja fabryczna tworząca kolumnę do wyświetlania daty i czasu (LocalDateTime).
 */
@Suppress("FunctionName")
fun LocalDateTimeColumn(
    header: String,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
) = DateTimeColumn(
    header = header,
    kClass = LocalDateTime::class,
    adapter = LocalDateTimeAdapter,
    width = width,
    sortable = sortable,
    filterable = filterable,
)

/**
 * Funkcja fabryczna tworząca kolumnę do wyświetlania chwil czasowych (Instant).
 */
@OptIn(ExperimentalTime::class)
@Suppress("FunctionName")
fun InstantColumn(
    header: String,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
    timeZone: TimeZone = TimeZone.currentSystemDefault(), // Możliwość zmiany strefy czasowej dla wyświetlania
) = DateTimeColumn(
    header = header,
    kClass = Instant::class,
    adapter = InstantAdapter(timeZone),
    width = width,
    sortable = sortable,
    filterable = filterable,
)

/**
 * Funkcja fabryczna tworząca kolumnę do wyświetlania czasu (LocalTime).
 */
@Suppress("FunctionName")
fun LocalTimeColumn(
    header: String,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
) = DateTimeColumn(
    header = header,
    kClass = LocalTime::class,
    adapter = LocalTimeAdapter,
    width = width,
    sortable = sortable,
    filterable = filterable,
)

/**
 * Funkcja fabryczna tworząca kolumnę do wyświetlania czasu z offsetem (OffsetTime).
 */
@Suppress("FunctionName")
fun OffsetTimeColumn(
    header: String,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
) = DateTimeColumn(
    header = header,
    kClass = OffsetTime::class,
    adapter = OffsetTimeAdapter,
    width = width,
    sortable = sortable,
    filterable = filterable,
)