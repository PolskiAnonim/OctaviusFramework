package org.octavius.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * System typografii aplikacji.
 * 
 * Zawiera kompletny zestaw stylów tekstu i funkcje do zarządzania czcionkami.
 */

/**
 * Zwraca domyślną rodzinę czcionek używaną w aplikacji.
 * 
 * @return FontFamily.Default
 */
@Composable
fun fontFamily() = FontFamily.Default

/**
 * Kompletny zestaw stylów tekstu używanych w aplikacji.
 * 
 * Zawiera style podzielone na kategorie:
 * - Nagłówki: h1 (24sp) do h4 (16sp)
 * - Treść: body1 (16sp) do body3 (12sp)
 * - Etykiety: label1 (14sp) do label3 (10sp)
 * - Specjalne: button, input z odpowiednimi wagami fontów
 */
data class Typography(
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val h4: TextStyle,
    val body1: TextStyle,
    val body2: TextStyle,
    val body3: TextStyle,
    val label1: TextStyle,
    val label2: TextStyle,
    val label3: TextStyle,
    val button: TextStyle,
    val input: TextStyle,
)

/** Domyślny zestaw stylów typograficznych z predefiniowanymi wartościami */
private val defaultTypography =
    Typography(
        h1 =
            TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        h2 =
            TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        h3 =
            TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
            ),
        h4 =
            TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
            ),
        body1 =
            TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
            ),
        body2 =
            TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.15.sp,
            ),
        body3 =
            TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.15.sp,
            ),
        label1 =
            TextStyle(
                fontWeight = FontWeight.W500,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        label2 =
            TextStyle(
                fontWeight = FontWeight.W500,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        label3 =
            TextStyle(
                fontWeight = FontWeight.W500,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.5.sp,
            ),
        button =
            TextStyle(
                fontWeight = FontWeight.W500,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 1.sp,
            ),
        input =
            TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
            ),
    )

/**
 * Tworzy konfigurowalne style typograficzne z dynamiczną czcionką.
 * 
 * Bierze domyślną typografię i aplikuje aktualną rodzinę czcionek
 * do wszystkich stylów tekstu.
 * 
 * @return Typography z zastosowanymi czcionkami
 */
@Composable
fun provideTypography(): Typography {
    val fontFamily = fontFamily()

    return defaultTypography.copy(
        h1 = defaultTypography.h1.copy(fontFamily = fontFamily),
        h2 = defaultTypography.h2.copy(fontFamily = fontFamily),
        h3 = defaultTypography.h3.copy(fontFamily = fontFamily),
        h4 = defaultTypography.h4.copy(fontFamily = fontFamily),
        body1 = defaultTypography.body1.copy(fontFamily = fontFamily),
        body2 = defaultTypography.body2.copy(fontFamily = fontFamily),
        body3 = defaultTypography.body3.copy(fontFamily = fontFamily),
        label1 = defaultTypography.label1.copy(fontFamily = fontFamily),
        label2 = defaultTypography.label2.copy(fontFamily = fontFamily),
        label3 = defaultTypography.label3.copy(fontFamily = fontFamily),
        button = defaultTypography.button.copy(fontFamily = fontFamily),
        input = defaultTypography.input.copy(fontFamily = fontFamily),
    )
}

/** CompositionLocal zapewniający dostęp do systemu typografii */
val LocalTypography = staticCompositionLocalOf { defaultTypography }
/** CompositionLocal dla aktualnego stylu tekstu */
val LocalTextStyle = compositionLocalOf(structuralEqualityPolicy()) { TextStyle.Default }
