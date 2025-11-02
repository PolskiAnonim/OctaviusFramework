package org.octavius.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Stan całej nawigacji w aplikacji.
 *
 * Przechowuje informacje o aktywnej zakładce oraz stosach ekranów dla każdej zakładki.
 * Używany przez `AppRouter` jako model danych dla stanu nawigacji.
 *
 * @param activeTab Aktualnie wyświetlana zakładka
 * @param tabStacks Mapa stosów ekranów - klucz to indeks zakładki, wartość to lista ekranów od najstarszego do najnowszego
 */
data class AppNavigationState(
    val activeTab: Tab,
    val tabStacks: Map<UShort, List<Screen>>
)

/**
 * Centralny singleton zarządzający stanem nawigacji w całej aplikacji.
 *
 * `AppRouter` jest pojedynczym źródłem prawdy (Single Source of Truth) dla nawigacji
 * i zapewnia globalny dostęp do funkcji nawigacyjnych z dowolnego miejsca w aplikacji.
 *
 * ## Architektura
 *
 * - **Zakładki**: Główne sekcje aplikacji
 * - **Stosy nawigacji**: Każda zakładka ma własny stos ekranów z możliwością nawigacji w przód/tył
 * - **Reaktywny stan**: UI obserwuje zmiany stanu przez `StateFlow`
 * - **Bezpieczeństwo**: Zabezpieczenia przed nieprawidłowym stanem (np. cofanie z ekranu głównego)
 *
 * ## Przykłady użycia
 *
 * ### Przełączanie zakładek
 * ```kotlin
 * AppRouter.switchToTab(1u) // Przejdź do zakładki o indexie  1
 * ```
 *
 * ### Nawigacja do nowego ekranu
 * ```kotlin
 * val formScreen = GameFormScreen(gameId = 123)
 * AppRouter.navigateTo(formScreen)
 * ```
 *
 * ### Powrót do poprzedniego ekranu
 * ```kotlin
 * AppRouter.goBack() // Bezpieczne - nie cofnie z ekranu głównego zakładki
 * ```
 *
 * ### Obserwowanie stanu w UI
 * ```kotlin
 * @Composable
 * fun MyComponent() {
 *     val navState by AppRouter.state.collectAsState()
 *     navState?.let { state ->
 *         Text("Aktywna zakładka: ${state.activeTab.options.title}")
 *     }
 * }
 * ```
 *
 * ## Inicjalizacja
 *
 * Router musi być zainicjalizowany przed użyciem, zazwyczaj w `MainScreen`:
 * ```kotlin
 * val tabs = listOf(AsianMediaTab(), GameTab(), SettingsTab())
 * AppRouter.initialize(tabs)
 * ```
 *
 * ## Bezpieczeństwo typów
 *
 * - Duplikaty ekranów tego samego typu są ignorowane w `navigateTo()`
 * - `goBack()` nie pozwala opuścić ekranu głównego zakładki
 * - Wszystkie operacje są null-safe względem stanu inicjalizacji
 *
 * @see Screen Interfejs implementowany przez wszystkie ekrany
 * @see Tab Interfejs implementowany przez wszystkie zakładki
 * @see AppNavigationState Model danych stanu nawigacji
 */
object AppRouter {

    private lateinit var initialTabs: List<Tab>

    /**
     * Inicjalizuje router z podaną listą zakładek.
     *
     * Musi być wywołane przed jakimkolwiek użyciem routera, zazwyczaj w `MainScreen.init`.
     * Tworzy początkowy stan nawigacji z pierwszą zakładką jako aktywną i ekranami głównymi
     * dla każdej zakładki w ich stosach.
     *
     * @param tabs Lista wszystkich zakładek aplikacji w kolejności wyświetlania
     * @throws IllegalStateException jeśli lista zakładek jest pusta
     */
    fun initialize(tabs: List<Tab>) {
        require(tabs.isNotEmpty()) { "Lista zakładek nie może być pusta" }
        
        initialTabs = tabs
        val initialStacks = tabs.associate { tab ->
            tab.index to listOf(tab.getInitialScreen())
        }

        _state.value = AppNavigationState(
            activeTab = tabs.first(),
            tabStacks = initialStacks
        )
    }

    private val _state = MutableStateFlow<AppNavigationState?>(null)
    
    /**
     * Reaktywny stan nawigacji obserwowany przez UI.
     *
     * Emituje `null` przed inicjalizacją, następnie `AppNavigationState` po każdej zmianie.
     * UI powinno obserwować ten stan przez `collectAsState()` w Composable.
     */
    val state = _state.asStateFlow()

    /**
     * Przełącza na zakładkę o podanym indeksie.
     *
     * Zachowuje stos ekranów dla każdej zakładki - po powrocie do zakładki
     * użytkownik znajdzie się na tym samym ekranie, na którym ją opuścił.
     *
     * @param tabIndex Indeks zakładki do aktywacji (0, 1, 2...)
     * @throws NoSuchElementException jeśli zakładka o podanym indeksie nie istnieje
     */
    fun switchToTab(tabIndex: UShort) {
        _state.update { currentState ->
            currentState?.copy(
                activeTab = initialTabs.first { it.index == tabIndex }
            )
        }
    }

    /**
     * Nawiguje do nowego ekranu na aktywnej zakładce.
     *
     * Dodaje ekran na szczyt stosu nawigacji aktywnej zakładki. Jeśli ekran tego samego
     * typu już jest na szczycie stosu, operacja jest ignorowana (zabezpieczenie przed duplikatami).
     *
     * @param screen Nowy ekran do wyświetlenia
     */
    fun navigateTo(screen: Screen) {
        _state.update { currentState ->
            val activeTabIndex = currentState!!.activeTab.index
            val currentStack = currentState.tabStacks[activeTabIndex]!!

            // Zabezpieczenie przed duplikatami tego samego typu ekranu
            if (currentStack.last()::class == screen::class) {
                return@update currentState
            }

            val newStack = currentStack + screen
            currentState.copy(
                tabStacks = currentState.tabStacks + (activeTabIndex to newStack)
            )
        }
    }

    /**
     * Atomowo przełącza zakładkę i nawiguje do nowego ekranu.
     *
     * Ta metoda najpierw zmienia aktywną zakładkę na tę o podanym `tabIndex`,
     * a następnie dodaje ekran na szczyt stosu tej zakładki.
     * Cała operacja jest wykonana w jednym kroku, aby zapobiec niechcianym
     * stanom pośrednim w UI.
     *
     * @param screen Nowy ekran do wyświetlenia.
     * @param tabIndex Indeks zakładki, na której ma się odbyć nawigacja.
     */
    fun navigateTo(screen: Screen, tabIndex: UShort) {
        _state.update { currentState ->
            // Krok 1: Przygotuj stan z nową aktywną zakładką
            val stateWithSwitchedTab = currentState!!.copy(
                activeTab = initialTabs.first { it.index == tabIndex }
            )

            // Krok 2: Dodaj nowy ekran do stosu tej (teraz aktywnej) zakładki
            val targetStack = stateWithSwitchedTab.tabStacks[tabIndex]!!

            if (targetStack.last()::class == screen::class) {
                return@update stateWithSwitchedTab // Tylko przełącz zakładkę, nie duplikuj ekranu
            }

            val newStack = targetStack + screen
            stateWithSwitchedTab.copy(
                tabStacks = stateWithSwitchedTab.tabStacks + (tabIndex to newStack)
            )
        }
    }

    /**
     * Cofa do poprzedniego ekranu na aktywnej zakładce.
     *
     * Usuwa ostatni ekran ze stosu nawigacji aktywnej zakładki. Jeśli stos zawiera
     * tylko jeden ekran (ekran główny zakładki), operacja jest ignorowana.
     */
    fun goBack() {
        _state.update { currentState ->
            val activeTabIndex = currentState!!.activeTab.index
            val currentStack = currentState.tabStacks[activeTabIndex]!!

            // Zabezpieczenie - nie pozwalaj opuścić ekranu głównego zakładki
            if (currentStack.size <= 1) return@update currentState

            val newStack = currentStack.dropLast(1)
            currentState.copy(
                tabStacks = currentState.tabStacks + (activeTabIndex to newStack)
            )
        }
    }
}