package org.octavius.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * System odstępów i wymiarów używanych w aplikacji.
 * 
 * Zawiera standardowe odstępy ogólne oraz wyspecjalizowane wymiary dla formularzy.
 */

/**
 * Podstawowe odstępy używane w całej aplikacji.
 * 
 * Dostępne przez AppTheme.spacing w komponentach Composable.
 */
data class Spacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp,
)

/**
 * Wyspecjalizowane odstępy i wymiary dla komponentów formularzy.
 * 
 * Zawiera ponad 40 predefiniowanych stałych dla:
 * - Padding pól formularza
 * - Wymiary nagłówków i sekcji
 * - Odstępy kontrolek (dropdown, boolean, repeatable)
 * - Wymiary kart i kontenerów
 */
object FormSpacing {
    val fieldPaddingHorizontal = 4.dp
    val fieldPaddingVertical = 8.dp
    val labelPaddingStart = 4.dp
    val labelPaddingBottom = 4.dp
    val errorPaddingStart = 24.dp
    val errorPaddingBottom = 8.dp
    val sectionPadding = 16.dp
    val sectionHeaderPaddingBottom = 12.dp
    val sectionContentSpacing = 8.dp
    val containerPaddingVertical = 8.dp
    val itemSpacing = 8.dp
    val controlSpacing = 12.dp
    val cardPadding = 16.dp
    val headerHeight = 48.dp
    val smallHeaderHeight = 40.dp
    val booleanControlPadding = 4.dp
    val booleanRowPaddingHorizontal = 12.dp
    val booleanRowPaddingVertical = 8.dp
    val dropdownPaddingHorizontal = 16.dp
    val dropdownPaddingVertical = 8.dp
    val repeatableRowPadding = 16.dp
    val repeatableHeaderPadding = 12.dp
}

/** CompositionLocal zapewniający dostęp do systemu odstępów */
val LocalSpacing = staticCompositionLocalOf { Spacing() }