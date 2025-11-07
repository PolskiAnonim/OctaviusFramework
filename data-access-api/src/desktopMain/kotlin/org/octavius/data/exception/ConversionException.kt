package org.octavius.data.exception

enum class ConversionExceptionMessage {
    VALUE_CONVERSION_FAILED,    // Ogólny błąd konwersji standardowego typu
    ENUM_CONVERSION_FAILED,     // Wartość z bazy nie pasuje do żadnego enuma
    UNSUPPORTED_COMPONENT_TYPE_IN_ARRAY,
    INVALID_DYNAMIC_DTO_FORMAT, // Błąd parsowania dynamic_dto

    // Błędy mapowania
    OBJECT_MAPPING_FAILED,      // Ogólny błąd podczas tworzenia instancji data class
    MISSING_REQUIRED_PROPERTY,  // Brak w danych klucza dla wymaganego pola w data class
    JSON_DESERIALIZATION_FAILED, // Błąd deserializacji JSONa w dynamic_dto
    JSON_SERIALIZATION_FAILED   // Błąd serializacji obiektu do JSON na potrzeby dynamic_dto
}

private fun generateDeveloperMessage(
    messageEnum: ConversionExceptionMessage,
    value: Any?,
    targetType: String?,
    propertyName: String?
): String {
    return when (messageEnum) {
        ConversionExceptionMessage.VALUE_CONVERSION_FAILED -> "Nie można przekonwertować wartości '$value' na typ '$targetType'."
        ConversionExceptionMessage.ENUM_CONVERSION_FAILED -> "Nie można przekonwertować wartości enum '$value' na typ '$targetType'."
        ConversionExceptionMessage.INVALID_DYNAMIC_DTO_FORMAT -> "Nieprawidłowy format dynamic_dto: '$value'."
        ConversionExceptionMessage.OBJECT_MAPPING_FAILED -> "Nie udało się zmapować danych na obiekt klasy '$targetType'."
        ConversionExceptionMessage.MISSING_REQUIRED_PROPERTY -> "Brak wymaganego pola '$propertyName' (klucz: '$value') podczas mapowania na klasę '$targetType'."
        ConversionExceptionMessage.JSON_DESERIALIZATION_FAILED -> "Nie udało się zdeserializować JSON dla dynamicznego typu '$targetType'."
        ConversionExceptionMessage.UNSUPPORTED_COMPONENT_TYPE_IN_ARRAY ->
            "Natywne tablice JDBC (Array<*>) nie obsługują typów złożonych (np. data class, List, Map). " +
                    "Wykryto typ: '${targetType}'. Użyj List<DataClass>, aby biblioteka mogła wygenerować składnię ARRAY[ROW(...)]."
        ConversionExceptionMessage.JSON_SERIALIZATION_FAILED -> "Nie udało się zserializować obiektu klasy '$targetType' do formatu JSON. " +
                "Upewnij się, że klasa i wszystkie jej zagnieżdżone typy mają adnotację @Serializable."
    }
}

/**
 * Błędy związane z konwersją, parsowaniem lub mapowaniem danych między bazą a Kotlinem.
 */
class ConversionException(
    val messageEnum: ConversionExceptionMessage,
    // Pola kontekstowe - mogą być null w zależności od typu błędu
    val value: Any? = null,
    val targetType: String? = null,
    val rowData: Map<String, Any?>? = null,
    val propertyName: String? = null,
    cause: Throwable? = null
) : DatabaseException(
    messageEnum.name,
    cause
) {
    override fun toString(): String {
        return """
        -------------------------------
        |     CONVERSION FAILED     
        | message: ${generateDeveloperMessage(this.messageEnum, value, targetType, propertyName) }
        | value: $value
        | targetType: $targetType
        | rowData: $rowData
        | propertyName: $propertyName
        ---------------------------------
        """.trimIndent()
    }
}