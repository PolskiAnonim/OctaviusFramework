package org.octavius.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp,
)

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

val LocalSpacing = staticCompositionLocalOf { Spacing() }

val AppTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current