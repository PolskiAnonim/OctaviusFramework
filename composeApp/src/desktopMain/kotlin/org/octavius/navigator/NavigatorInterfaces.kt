package org.octavius.navigator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import org.octavius.localization.Translations

/**
 * Interfejsy i klasy pomocnicze dla systemów nawigacji.
 *
 * Zawiera definicje dla:
 * - Screen - pojedynczy ekran w nawigatorze stosowym
 * - Tab - zakładka w nawigatorze tabularnym
 * - TabOptions - opcje konfiguracyjne dla zakładek
 * - CompositionLocal dla dostępu do nawigatora
 */

/**
 * CompositionLocal zapewniający dostęp do aktualnego nawigatora.
 *
 * Użycie w komponentach zagnieżdżonych:
 * ```kotlin
 * val navigator = LocalNavigator.current
 * navigator.addScreen(NewScreen())
 * ```
 *
 * @throws IllegalStateException gdy navigator nie został dostarczony
 */
val LocalNavigator = compositionLocalOf<Navigator> { error(Translations.get("navigation.noNavigator")) }

/**
 * Interfejs reprezentujący pojedynczy ekran w nawigatorze stosowym.
 *
 * Każdy ekran musi mieć tytuł wyświetlany w pasku nawigacji
 * oraz metodę Content() renderującą interfejs ekranu.
 *
 * Przykład implementacji:
 * ```kotlin
 * class MyScreen : Screen {
 *     override val title = "Mój Ekran"
 *
 *     @Composable
 *     override fun Content() {
 *         Text("Zawartość ekranu")
 *     }
 * }
 * ```
 */
interface Screen {
    /** Tytuł ekranu wyświetlany w pasku nawigacji */
    val title: String

    /**
     * Metoda renderująca interfejs ekranu.
     *
     * Wywoływana przez Navigator gdy ekran jest aktywny.
     */
    @Composable
    fun Content()
}

/**
 * Opcje konfiguracyjne dla zakładki w TabNavigator.
 *
 * @param title Tytuł zakładki wyświetlany w pasku zakładek
 * @param icon Opcjonalna ikona zakładki
 */
data class TabOptions(
    val title: String, val icon: Painter? = null
)

/**
 * Interfejs reprezentujący zakładkę w nawigatorze tabularnym.
 *
 * Każda zakładka musi mieć:
 * - Opcje konfiguracyjne (tytuł, ikona)
 * - Unikalny indeks do identyfikacji
 * - Metodę Content() renderującą zawartość zakładki
 *
 * Przykład implementacji:
 * ```kotlin
 * class MyTab : Tab {
 *     override val options @Composable get() = TabOptions("Moja Zakładka")
 *     override val index: UShort = 0u
 *
 *     @Composable
 *     override fun Content() {
 *         Text("Zawartość zakładki")
 *     }
 * }
 * ```
 */
interface Tab {
    /** Opcje konfiguracyjne zakładki (dynamiczne, może używać Composable state) */
    val options: TabOptions
        @Composable get

    /** Unikalny indeks zakładki używany do identyfikacji */
    val index: UShort

    /**
     * Metoda renderująca zawartość zakładki.
     *
     * Wywoływana przez TabNavigator gdy zakładka jest aktywna.
     */
    @Composable
    fun Content()
}