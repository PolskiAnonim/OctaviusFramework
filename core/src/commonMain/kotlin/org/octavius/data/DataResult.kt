package org.octavius.data

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
 *
 * Pozwala na czyste i bezpieczne operowanie na wyniku bez rzucania wyjątków.
 * Przykład: `result.map { it.toString() }` konwertuje Success<Int> na Success<String>.
 *
 * @param T Typ oryginalnej wartości.
 * @param R Typ wartości po transformacji.
 * @param transform Funkcja transformująca wartość typu T na typ R.
 * @return Nowy DataResult z przekształconą wartością lub oryginalny Failure.
 */
inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> {
    return when (this) {
        is DataResult.Success -> DataResult.Success(transform(value))
        is DataResult.Failure -> this
    }
}

/**
 * Wykonuje akcję jeśli wynik jest Success, nie modyfikując oryginalnej wartości.
 *
 * @param action Akcja do wykonania na wartości Success.
 * @return Ten sam DataResult dla łańcuchowania.
 */
fun <T> DataResult<T>.onSuccess(action: (T) -> Unit): DataResult<T> {
    if (this is DataResult.Success) action(value)
    return this
}

/**
 * Wykonuje akcję jeśli wynik jest Failure, nie modyfikując oryginalnego błędu.
 *
 * @param action Akcja do wykonania na błędzie Failure.
 * @return Ten sam DataResult dla łańcuchowania.
 */
fun <T> DataResult<T>.onFailure(action: (DatabaseException) -> Unit): DataResult<T> {
    if (this is DataResult.Failure) action(error)
    return this
}