package org.octavius.util

import androidx.compose.ui.graphics.Color

/**
 * Utility functions for color conversion.
 */
object ColorUtils {
    /**
     * Parses a hex string to a [Color].
     * Supports formats: #RRGGBB, #AARRGGBB.
     * Returns null if the format is invalid.
     */
    fun hexToColor(hex: String): Color? {
        val clean = hex.removePrefix("#")
        return try {
            when (clean.length) {
                6 -> {
                    val value = clean.toLong(16)
                    Color(
                        red = ((value shr 16) and 0xFF) / 255f,
                        green = ((value shr 8) and 0xFF) / 255f,
                        blue = (value and 0xFF) / 255f
                    )
                }

                8 -> {
                    val value = clean.toLong(16)
                    Color(
                        alpha = ((value shr 24) and 0xFF) / 255f,
                        red = ((value shr 16) and 0xFF) / 255f,
                        green = ((value shr 8) and 0xFF) / 255f,
                        blue = (value and 0xFF) / 255f
                    )
                }

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts a [Color] to a hex string (#RRGGBB or #AARRGGBB).
     */
    fun colorToHex(color: Color, includeAlpha: Boolean = false): String {
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)

        return if (includeAlpha) {
            val a = (color.alpha * 255).toInt().coerceIn(0, 255)
            "#%02X%02X%02X%02X".format(a, r, g, b)
        } else {
            "#%02X%02X%02X".format(r, g, b)
        }
    }

    fun hsvToColor(hue: Float, saturation: Float, value: Float, alpha: Float): Color {
        val c = value * saturation
        val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
        val m = value - c
        val (r, g, b) = when {
            hue < 60f -> Triple(c, x, 0f)
            hue < 120f -> Triple(x, c, 0f)
            hue < 180f -> Triple(0f, c, x)
            hue < 240f -> Triple(0f, x, c)
            hue < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(red = r + m, green = g + m, blue = b + m, alpha = alpha)
    }

    fun colorToHsv(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val h = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }

        val s = if (max == 0f) 0f else delta / max
        val v = max
        return floatArrayOf(h, s, v)
    }
}
