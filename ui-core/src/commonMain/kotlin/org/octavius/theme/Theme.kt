package org.octavius.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Główny system motywów aplikacji.
 * 
 * Zapewnia centralizowany dostęp do wszystkich elementów motywu:
 * kolory, typografia, odstępy oraz konfiguracja Material 3.
 */

/**
 * Centralny obiekt zapewniający dostęp do elementów motywu.
 * 
 * Użycie w komponentach Composable:
 * ```kotlin
 * Text(
 *     color = AppTheme.colors.primary,
 *     style = AppTheme.typography.h1,
 *     modifier = Modifier.padding(AppTheme.spacing.large)
 * )
 * ```
 */
object AppTheme {
    /** Aktualny schemat kolorów */
    val colors: Colors
        @ReadOnlyComposable @Composable
        get() = LocalColors.current

    /** Aktualny system typografii */
    val typography: Typography
        @ReadOnlyComposable @Composable
        get() = LocalTypography.current

    /** Aktualny system odstępów */
    val spacing: Spacing
        @ReadOnlyComposable @Composable
        get() = LocalSpacing.current
}

/**
 * Główny Composable konfigurujący motyw aplikacji.
 * 
 * Ustawia wszystkie CompositionLocal providers dla:
 * - Schemat kolorów (jasny/ciemny)
 * - System typografii z dynamicznymi czcionkami
 * - System odstępów
 * - Material 3 ripple effects
 * - Kolory zaznaczania tekstu
 * 
 * @param isDarkTheme Czy użyć ciemnego motywu (domyślnie wykrywa systemowe ustawienie)
 * @param content Treść do wyświetlenia z zastosowanym motywem
 */
@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val rippleIndication = ripple()
    val selectionColors = rememberTextSelectionColors(LightColors)
    val typography = provideTypography()
    val colors = if (isDarkTheme) DarkColors else LightColors

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalTypography provides typography,
        LocalSpacing provides Spacing(),
        LocalIndication provides rippleIndication,
        LocalTextSelectionColors provides selectionColors,
        LocalContentColor provides colors.contentColorFor(colors.background),
        LocalTextStyle provides typography.body1,
        content = content,
    )
}

/**
 * Zwraca odpowiedni kolor tekstu dla podanego koloru tła.
 * 
 * @param color Kolor tła
 * @return Odpowiedni kolor tekstu
 */
@Composable
fun contentColorFor(color: Color): Color {
    return AppTheme.colors.contentColorFor(color)
}

/**
 * Tworzy kolory zaznaczania tekstu na podstawie schematu kolorów.
 * 
 * @param colorScheme Schemat kolorów do użycia
 * @return TextSelectionColors z odpowiednimi kolorami
 */
@Composable
internal fun rememberTextSelectionColors(colorScheme: Colors): TextSelectionColors {
    val primaryColor = colorScheme.primary
    return remember(primaryColor) {
        TextSelectionColors(
            handleColor = primaryColor,
            backgroundColor = primaryColor.copy(alpha = TextSelectionBackgroundOpacity),
        )
    }
}

/** Przezroczystość tła zaznaczanego tekstu */
internal const val TextSelectionBackgroundOpacity = 0.4f
