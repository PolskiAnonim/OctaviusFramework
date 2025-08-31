package org.octavius.form.control.base

import org.octavius.form.component.ErrorManager
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormState

/**
 * Abstrakcyjna klasa bazowa dla wszystkich walidatorów kontrolek formularza.
 *
 * Walidator odpowiada za:
 * - Sprawdzanie widoczności kontrolek na podstawie zależności
 * - Określanie wymagalności pól w zależności od warunków
 * - Walidację podstawową (wymagalność, pusty stan)
 * - Walidację specyficzną dla typu kontrolki
 *
 * Każdy typ kontrolki powinien mieć własny walidator dziedziczący z tej klasy
 * i implementujący metodę validateSpecific() dla specyficznych reguł walidacji.
 *
 * @param T typ danych przechowywanych przez kontrolkę
 */
abstract class ControlValidator<T : Any> {
    protected lateinit var formState: FormState
    protected lateinit var formSchema: FormSchema
    protected lateinit var errorManager: ErrorManager

    /**
     * Ustawia referencje do komponentów formularza dla walidatora.
     */
    internal open fun setupFormReferences(
        formState: FormState,
        formSchema: FormSchema,
        errorManager: ErrorManager
    ) {
        this.formState = formState
        this.formSchema = formSchema
        this.errorManager = errorManager
    }

    private fun createContextFromName(fullPath: String): RenderContext {

        val lastDotIndex = fullPath.lastIndexOf('.')
        if (lastDotIndex == -1) {
            // Nie ma kropek, to jest kontrolka na najwyższym poziomie
            return RenderContext(localName = fullPath, basePath = "")
        }

        val basePath = fullPath.substring(0, lastDotIndex)
        val localName = fullPath.substring(lastDotIndex + 1)
        return RenderContext(localName = localName, basePath = basePath)
    }

    /**
     * Sprawdza czy kontrolka jest widoczna na podstawie zależności i hierarchii.
     *
     * Kontrolka jest widoczna gdy:
     * - Jej kontrolka nadrzędna (jeśli istnieje) jest widoczna
     * - Wszystkie zależności typu Visible są spełnione
     *
     * @param control kontrolka do sprawdzenia
     * @return true jeśli kontrolka powinna być widoczna
     */
    internal fun isControlVisible(
        control: Control<*>,
        renderContext: RenderContext
    ): Boolean {
        val controls = formSchema.getAllControls()
        val states = formState.getAllStates()
        // Jeśli kontrolka ma rodzica, najpierw sprawdź czy rodzic jest widoczny
        control.parentControl?.let { parentName ->
            val parentControl = controls[parentName]!! // Rodzic musi istnieć

            // Stwórz kontekst dla rodzica. Jego nazwa jest jego pełną ścieżką.
            val parentContext = createContextFromName(parentName)

            // Wywołaj rekurencyjnie isControlVisible dla rodzica, z jego WŁASNYM kontekstem.
            if (!isControlVisible(parentControl, parentContext)) {
                return false
            }
        }

        // Sprawdź zależności
        control.dependencies?.forEach { (_, dependency) ->
            if (dependency.dependencyType != DependencyType.Visible) return@forEach

            val resolvedControlName = resolveDependencyControlName(dependency, renderContext)
            val dependentState = states[resolvedControlName] ?: return@forEach
            val dependentValue = dependentState.value.value

            when (dependency.comparisonType) {
                ComparisonType.OneOf -> {
                    @Suppress("UNCHECKED_CAST")
                    val acceptedValues = dependency.value as? List<*> ?: listOf(dependency.value)
                    if (dependentValue !in acceptedValues) return false
                }

                ComparisonType.NotEquals -> {
                    if (dependentValue == dependency.value) return false
                }

                ComparisonType.Equals -> {
                    if (dependentValue != dependency.value) return false
                }
            }
        }

        return true
    }

    /**
     * Sprawdza czy kontrolka jest wymagana na podstawie jej konfiguracji i zależności.
     *
     * Kontrolka jest wymagana gdy:
     * - Ma ustawioną flagę required = true, lub
     * - Spełnione są zależności typu Required
     *
     * @param control kontrolka do sprawdzenia
     * @return true jeśli kontrolka jest wymagana
     */
    internal fun isControlRequired(
        control: Control<*>,
        renderContext: RenderContext
    ): Boolean {
        formSchema.getAllControls()
        val states = formState.getAllStates()
        var isRequired = control.required == true

        // Sprawdzamy zależności typu Required
        control.dependencies?.forEach { (_, dependency) ->
            if (dependency.dependencyType == DependencyType.Required) {
                val resolvedControlName = resolveDependencyControlName(dependency, renderContext)
                val dependentState = states[resolvedControlName] ?: return@forEach
                val dependentValue = dependentState.value.value

                when (dependency.comparisonType) {
                    ComparisonType.OneOf -> {
                        @Suppress("UNCHECKED_CAST")
                        val acceptedValues = dependency.value as? List<*> ?: listOf(dependency.value)
                        if (dependentValue in acceptedValues) {
                            isRequired = true
                        }
                    }

                    ComparisonType.Equals -> {
                        if (dependentValue == dependency.value) {
                            isRequired = true
                        }
                    }

                    ComparisonType.NotEquals -> {
                        if (dependentValue != dependency.value) {
                            isRequired = true
                        }
                    }
                }
            }
        }

        return isRequired
    }

    /**
     * Sprawdza czy wartość jest pusta lub nie została wypełniona.
     *
     * Wartość jest uważana za pustą gdy:
     * - Jest null
     * - Jest pustym ciągiem znaków lub zawiera tylko białe znaki
     * - Jest pustą listą
     *
     * @param value wartość do sprawdzenia
     * @return true jeśli wartość jest pusta
     */
    internal fun isValueEmpty(value: Any?): Boolean {
        return when (value) {
            null -> true
            is String -> value.isBlank()
            is List<*> -> value.isEmpty()
            else -> false
        }
    }

    /**
     * Rozwiązuje nazwę kontrolki dla zależności, uwzględniając scope (Local/Global)
     */
    private fun resolveDependencyControlName(
        dependency: ControlDependency<*>,
        renderContext: RenderContext
    ): String {
        return if (dependency.scope == DependencyScope.Local) {
            // Jeśli to lokalna zależność, składamy pełną ścieżkę
            // do "sąsiada" w tym samym basePath.
            // basePath to np. "publications[123]"
            // dependency.controlName to np. "trackProgress"
            "${renderContext.basePath}.${dependency.controlName}"
        } else {
            // Globalna zależność - użyj oryginalnej nazwy z definicji zależności
            dependency.controlName
        }
    }

    /**
     * Główna metoda walidacji kontrolki.
     *
     * Proces walidacji:
     * 1. Sprawdza czy kontrolka jest widoczna (jeśli nie, pomija walidację)
     * 2. Określa czy kontrolka jest wymagana
     * 3. Sprawdza czy wymagane pole nie jest puste
     * 4. Uruchamia walidację specyficzną dla typu kontrolki
     *
     * @param state stan kontrolki
     * @param control definicja kontrolki
     */
    open fun validate(
        renderContext: RenderContext,
        state: ControlState<*>,
        control: Control<*>
    ) {
        // Jeśli kontrolka nie jest widoczna, pomijamy walidację
        if (!isControlVisible(control, renderContext)) {
            return
        }

        // Sprawdzamy, czy pole jest wymagane
        val isRequired = isControlRequired(control, renderContext)

        // Jeśli pole jest wymagane i wartość jest pusta, ustawiamy błąd
        if (isRequired && isValueEmpty(state.value.value)) {
            errorManager.setFieldErrors(renderContext.fullPath, listOf("To pole jest wymagane"))
            return
        }

        // Wywołujemy dodatkową walidację specyficzną dla kontrolki
        validateSpecific(renderContext, state)
    }

    /**
     * Metoda walidacji specyficznej dla typu kontrolki.
     *
     * Powinna być przesłonięta przez klasy pochodne dla implementacji
     * walidacji specyficznej dla danego typu kontrolki (np. sprawdzanie
     * formatu email, zakresu liczb, unikalności wartości, itp.).
     *
     * Domyślna implementacja nie wykonuje żadnej dodatkowej walidacji.
     *
     * @param controlName nazwa kontrolki
     * @param state stan kontrolki do walidacji
     */
    open fun validateSpecific(renderContext: RenderContext, state: ControlState<*>) {
        // Domyślnie czyścimy błędy jeśli nie ma problemów
        errorManager.clearFieldErrors(renderContext.fullPath)
    }
}