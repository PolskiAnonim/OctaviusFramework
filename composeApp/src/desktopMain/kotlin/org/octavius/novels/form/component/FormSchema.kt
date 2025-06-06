package org.octavius.novels.form.component

import org.octavius.novels.form.control.Control

/**
 * FormSchema
 * Klasa przy utworzeniu powinna dostać kontrolki formularza
 * oraz kolejność ich wyświetlania
 * @property controls nazwy kontrolek wraz z ich definicjami jako mapa - nazwy nie mogą się powtarzać
 * @property order kolejność i nazwy kontrolek które mają być wyświetlone na głównym poziomie
 * @exception NullPointerException nazwy kontrolek w order muszą istnieć w mapie controls
 */
class FormSchema(
    private val controls: Map<String, Control<*>>,
    val order: List<String>
) {
    /**
     * Inicjalizacja ustawia rodziców (kontrolki nadrzędne - przede wszystkim sekcje)
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
     * Funkcja ustawia referencje do komponentów formularza dla kontrolek które tego wymagają
     */
    fun setupFormReferences(formState: FormState, errorManager: ErrorManager) {
        controls.forEach { (controlName, control) ->
            control.setupFormReferences(formState, this, errorManager, controlName)
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
 * Fabryka do tworzenia schematów
 * Zawiera jedną funkcję - build
 * Służy do tworzenia klasy FormSchema
 */
abstract class FormSchemaBuilder {
    abstract fun build(): FormSchema
}