package org.octavius.database.config

/**
 * Strategia serializacji klas oznaczonych @DynamicallyMappable
 *
 * Uwaga
 *
 * Jawne użycie klasy z PgTyped wymusza traktowanie klasy jako `@PgComposite/@PgEnum`
 * Natomiast jawne użycie DynamicDto wymusza traktowanie klasy jako `@DynamicallyMappable`
 *
 * Pod warunkiem że posiadają takie adnotacje
 */
enum class DynamicDtoSerializationStrategy {
    /**
     * NIGDY nie konwertuje automatycznie do dynamic_dto.
     * Użycie wymaga jawnego opakowania w `DynamicDto.from()`.
     * Maksymalne bezpieczeństwo i jawność.
     */
    EXPLICIT_ONLY,

    /**
     * Automatycznie konwertuje do dynamic_dto, ale TYLKO wtedy,
     * gdy nie ma dwuznaczności (tj. klasa NIE ma adnotacji @PgComposite lub @PgEnum).
     * W przypadku konfliktu, @PgComposite zawsze wygrywa.
     */
    AUTOMATIC_WHEN_UNAMBIGUOUS,

    /**
     * Agresywnie konwertuje do dynamic_dto, jeśli tylko klasa ma
     * adnotację @DynamicallyMappable. Ta strategia ma wyższy priorytet
     * niż @PgComposite i @PgEnum w przypadku konfliktu.
     */
    PREFER_DYNAMIC_DTO
}
