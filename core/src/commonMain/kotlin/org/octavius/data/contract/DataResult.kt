package org.octavius.data.contract

import org.octavius.exception.DatabaseException

/**
 * Kontener na wynik operacji bazodanowej, który może zakończyć się sukcesem lub porażką.
 * Zastępuje mechanizm wyjątków, zmuszając do jawnego obsłużenia obu przypadków.
 *
 * @param T Typ danych w przypadku sukcesu.
 */
sealed class DataResult<out T> {
    /** Reprezentuje pomyślne wykonanie operacji. */
    data class Success<out T>(val value: T) : DataResult<T>()

    /** Reprezentuje błąd podczas operacji. */
    data class Failure(val error: DatabaseException) : DataResult<Nothing>()
}

/**
 * Transformuje wartość wewnątrz [DataResult.Success], zachowując [DataResult.Failure] bez zmian.
 * Pozwala na czyste i bezpieczne operowanie na wyniku.
 */
inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> {
    return when (this) {
        is DataResult.Success -> DataResult.Success(transform(value))
        is DataResult.Failure -> this
    }
}

fun <T> DataResult<T>.onSuccess(action: (T) -> Unit): DataResult<T> {
    if (this is DataResult.Success) action(value)
    return this
}

fun <T> DataResult<T>.onFailure(action: (DatabaseException) -> Unit): DataResult<T> {
    if (this is DataResult.Failure) action(error)
    return this
}