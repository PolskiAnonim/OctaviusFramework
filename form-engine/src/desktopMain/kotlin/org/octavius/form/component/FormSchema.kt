package org.octavius.form.component

import org.octavius.form.control.base.Control

/**
 * Schemat formularza - definicja struktury i układu kontrolek.
 *
 * FormSchema przechowuje kompletna definicję formularza włączając:
 * - Wszystkie kontrolki z ich konfiguracją
 * - Kolejność wyświetlania w głównym obszarze
 * - Kolejność wyświetlania w pasku akcji
 *
 * Schemat jest tworzona przez FormSchemaBuilder i jest niezmienny po utworzeniu.
 *
 * @param controls Mapa wszystkich kontrolek formularza (klucz = unikalna nazwa).
 * @param contentOrder Lista nazw kontrolek do wyświetlenia w głównym obszarze formularza.
 * @param actionBarOrder Lista nazw kontrolek do wyświetlenia w dolnym pasku akcji.
 *
 * @throws IllegalArgumentException jeśli nazwy w listach porządkujących nie istnieją w mapie controls.
 */
class FormSchema(
    private val controls: Map<String, Control<*>>,
    val contentOrder: List<String>,
    val actionBarOrder: List<String>
) {
    /**
     * Inicjalizuje relacje hierarchiczne między kontrolkami.
     *
     * Ustawia relacje rodzic-dziecko, szczególnie ważne dla kontrolek-kontenerów
     * takich jak sekcje, które zawierają inne kontrolki.
     */
    init {
        setupParentChildRelationships()
    }

    /**
     * Funkcja ustawia relacje nadrzędnych kontrolek
     */
    private fun setupParentChildRelationships() {
        controls.forEach { control ->
            control.value.setupParentRelationships(control.key, controls)
        }
    }

    /**
     * Zwraca kontrolkę o danej nazwie
     */
    fun getControl(name: String): Control<*>? = controls[name]

    /**
     * Zwraca wszystkie kontrolki zdefiniowane w tej klasie
     */
    fun getAllControls(): Map<String, Control<*>> = controls
}

/**
 * Abstrakcyjna fabryka do tworzenia schematów formularzy.
 *
 * Każdy formularz powinien mieć własną implementację buildera,
 * która definiuje specyficzną strukturę kontrolek, ich kolejność
 * i konfiguracje dla danej domeny biznesowej.
 *
 * Przykładowa implementacja:
 * ```kotlin
 * class UserFormSchemaBuilder : FormSchemaBuilder() {
 *     override fun build(): FormSchema {
 *         return FormSchema(
 *             controls = mapOf(
 *                 "name" to StringControl(label = "Imię", required = true),
 *                 "email" to StringControl(label = "Email")
 *             ),
 *             contentOrder = listOf("name", "email"),
 *             actionBarOrder = listOf("save", "cancel")
 *         )
 *     }
 * }
 * ```
 */
abstract class FormSchemaBuilder {
    abstract fun build(): FormSchema
}