package org.octavius.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/**
 * Interfejsy i klasy pomocnicze dla globalnego systemu nawigacji opartego na AppRouter.
 *
 * ## Architektura nawigacji
 * 
 * Aplikacja używa centralnego systemu nawigacji z singletonem `AppRouter`, który zarządza:
 * - **Zakładkami aplikacji** - główne sekcje (Asian Media, Games, Settings)  
 * - **Stosami nawigacji** - dla każdej zakładki osobny stos ekranów
 * - **Globalnym stanem** - Single Source of Truth dla całej nawigacji
 *
 * ## Główne komponenty
 * 
 * - **Screen** - pojedynczy ekran w stosie nawigacji zakładki
 * - **Tab** - zakładka aplikacji z własnym stosem ekranów
 * - **TabOptions** - opcje konfiguracyjne zakładek (tytuł, ikona)
 * - **AppRouter** - centralny singleton zarządzający stanem nawigacji
 * - **AppNavigationState** - stan całej nawigacji aplikacji
 * 
 * ## Przykład użycia
 * 
 * ```kotlin
 * // Przełączanie między zakładkami
 * AppRouter.switchToTab(1u)
 * 
 * // Nawigacja do nowego ekranu na aktywnej zakładce
 * AppRouter.navigateTo(MyScreen())
 * 
 * // Powrót do poprzedniego ekranu
 * AppRouter.goBack()
 * ```
 */

/**
 * Interfejs reprezentujący pojedynczy ekran w stosie nawigacji zakładki.
 *
 * Każdy ekran w systemie nawigacji `AppRouter` musi implementować ten interfejs.
 * Ekrany są organizowane w stosy - każda zakładka ma własny stos ekranów.
 *
 * ## Właściwości
 * 
 * - **title** - wyświetlany w górnym pasku nawigacji
 * - **Content()** - metoda renderująca UI ekranu
 *
 * ## Przykład implementacji
 * 
 * ```kotlin
 * class MyFormScreen : Screen {
 *     override val title = "Edycja danych"
 *
 *     @Composable
 *     override fun Content() {
 *         MyFormComponent()
 *     }
 * }
 * 
 * // Nawigacja do ekranu
 * AppRouter.navigateTo(MyFormScreen())
 * ```
 */
interface Screen {
    /** Tytuł ekranu wyświetlany w górnym pasku nawigacji */
    val title: String

    /**
     * Metoda renderująca interfejs ekranu.
     *
     * Wywoływana przez `ScreenContent` gdy ekran jest aktywny na szczycie stosu.
     */
    @Composable
    fun Content()
}

/**
 * Opcje konfiguracyjne dla zakładki w systemie nawigacji AppRouter.
 *
 * @param title Tytuł zakładki wyświetlany w `AppTabBar`
 * @param icon Opcjonalna ikona zakładki wyświetlana nad tytułem
 */
data class TabOptions(
    val title: String, val icon: Painter? = null
)

/**
 * Interfejs reprezentujący zakładkę w globalnym systemie nawigacji.
 *
 * Każda zakładka aplikacji (Asian Media, Games, Settings) implementuje ten interfejs.
 * Zakładki są zarządzane przez `AppRouter` i każda ma własny stos ekranów.
 *
 * ## Wymagane właściwości
 * 
 * - **options** - tytuł i opcjonalna ikona (mogą być dynamiczne)
 * - **index** - unikalny identyfikator zakładki (0, 1, 2...)
 * - **getInitialScreen()** - pierwszy ekran wyświetlany po otwarciu zakładki
 *
 * ## Przykład implementacji
 * 
 * ```kotlin
 * class MyTab : Tab {
 *     override val options @Composable get() = TabOptions(
 *         title = "Moja sekcja",
 *         icon = painterResource("icons/my_icon.xml")
 *     )
 *     override val index: UShort = 2u
 *
 *     override fun getInitialScreen() = MyReportScreen()
 * }
 * ```
 */
interface Tab {
    /** 
     * Opcje konfiguracyjne zakładki.
     * 
     * Może być dynamiczne i używać Composable state do reaktywności.
     */
    val options: TabOptions
        @Composable get

    /** 
     * Unikalny indeks zakładki używany przez AppRouter do identyfikacji.
     * 
     * Powinien być unikatowy w całej aplikacji (0, 1, 2...).
     */
    val index: UShort

    /** 
     * Zwraca pierwszy ekran wyświetlany po przełączeniu na tę zakładkę.
     * 
     * Zazwyczaj jest to ekran raportu/listy głównej dla danej sekcji.
     */
    fun getInitialScreen(): Screen
}