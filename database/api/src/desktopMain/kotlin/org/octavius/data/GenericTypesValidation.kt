package org.octavius.data

import org.octavius.data.exception.ConversionException
import org.octavius.data.exception.ConversionExceptionMessage
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Waliduje, czy wartość pasuje do docelowego typu KType.
 * Dla list i map, weryfikuje typ pierwszego elementu, który nie jest nullem.
 */
fun validateValue(value: Any?, targetType: KType): Any? {
    if (value == null) {
        return null
    }

    val targetClass = targetType.classifier as KClass<*>

    // --- Walidacja 1: Sprawdzenie głównego typu ---
    if (!targetClass.isInstance(value)) {
        throw ConversionException(
            messageEnum = ConversionExceptionMessage.INCOMPATIBLE_TYPE,
            value = value,
            targetType = targetType.toString()
        )
    }

    // --- Walidacja 2: Sprawdzenie typu elementu w kolekcji ---
    when (value) {
        is List<*> -> validateList(value, targetType)
        // Jedyna mapa z bazy która może przyjść to JsonObject
        is Map<*, *> -> validateMap(value, targetType)
    }

    return value
}

private fun validateList(value: List<*>, targetType: KType) {
    val firstNonNullElement = value.firstOrNull { it != null }

    if (firstNonNullElement != null) {
        val listElementType = targetType.arguments.firstOrNull()?.type
            ?: return // Dla List<*> lub gdy typ jest nieznany, nie walidujemy dalej

        val listElementClass = listElementType.classifier as? KClass<*>
            ?: return // Nie udało się określić klasy elementu, odpuszczamy

        if (!listElementClass.isInstance(firstNonNullElement)) {
            throw ConversionException(
                messageEnum = ConversionExceptionMessage.INCOMPATIBLE_COLLECTION_ELEMENT_TYPE,
                value = firstNonNullElement,
                targetType = listElementType.toString() // Np. "kotlin.String"
            )
        }
    }
}

private fun validateMap(value: Map<*,*>, targetType: KType) {
    val firstNonNullEntry = value.entries.firstOrNull { it.key != null && it.value != null }

    if (firstNonNullEntry != null && targetType.arguments.size == 2) {
        val keyType = targetType.arguments[0].type ?: return
        val valueType = targetType.arguments[1].type ?: return

        val keyClass = keyType.classifier as KClass<*>
        val valueClass = valueType.classifier as KClass<*>

        // Sprawdź typ klucza i wartości dla pierwszej pary
        if (!keyClass.isInstance(firstNonNullEntry.key) || !valueClass.isInstance(firstNonNullEntry.value)) {
            throw ConversionException(
                messageEnum = ConversionExceptionMessage.INCOMPATIBLE_COLLECTION_ELEMENT_TYPE,
                value = firstNonNullEntry,
                targetType = targetType.toString()
            )
        }
    }
}