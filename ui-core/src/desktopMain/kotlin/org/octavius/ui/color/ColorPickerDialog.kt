package org.octavius.ui.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.octavius.localization.Tr
import org.octavius.util.ColorUtils.colorToHex
import org.octavius.util.ColorUtils.colorToHsv
import org.octavius.util.ColorUtils.hexToColor
import org.octavius.util.ColorUtils.hsvToColor

/**
 * Okno dialogowe do wyboru koloru.
 *
 * Zawiera panel nasycenia/jasności (SV), suwak odcienia (Hue),
 * opcjonalny suwak przezroczystości (Alpha), pole tekstowe hex
 * oraz podgląd wybranego koloru.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color = Color.Red,
    showAlphaSlider: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val initialHsv = remember { colorToHsv(initialColor) }
    val initialAlpha = remember { initialColor.alpha }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember { mutableFloatStateOf(initialAlpha) }

    val currentColor by remember(hue, saturation, value, alpha) {
        derivedStateOf {  hsvToColor(hue, saturation, value, alpha) }
    }

    var hexInput by remember(currentColor) {
        mutableStateOf(colorToHex(currentColor, includeAlpha = showAlphaSlider))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Tr.ColorPicker.title()) },
        text = {
            Column(
                modifier = Modifier.width(300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SaturationValuePanel(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onSaturationValueChange = { s, v ->
                        saturation = s
                        value = v
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                HueSlider(
                    hue = hue,
                    onHueChange = { hue = it },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showAlphaSlider) {
                    AlphaSlider(
                        alpha = alpha,
                        color = hsvToColor(hue, saturation, value, 1f),
                        onAlphaChange = { alpha = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ColorPreview(
                        color = currentColor,
                        size = 48.dp
                    )
                    HexTextField(
                        value = hexInput,
                        onValueChange = { newHex ->
                            hexInput = newHex
                            hexToColor(newHex)?.let { parsed ->
                                val hsv = colorToHsv(parsed)
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                                if (showAlphaSlider) alpha = parsed.alpha
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentColor) }) {
                Text(Tr.Action.ok())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Tr.Action.cancel())
            }
        }
    )
}

@Composable
private fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColor = hsvToColor(hue, 1f, 1f, 1f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val s = (offset.x / size.width).coerceIn(0f, 1f)
                        val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        onSaturationValueChange(s, v)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val s = (change.position.x / size.width).coerceIn(0f, 1f)
                        val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        onSaturationValueChange(s, v)
                    }
                }
        ) {
            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)), size = size)
            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)), size = size)
            drawSelectionIndicator(saturation * size.width, (1f - value) * size.height)
        }
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColors = remember {
        List(7) { i -> hsvToColor(i * 60f, 1f, 1f, 1f) }
    }

    GradientSlider(
        value = hue,
        maxValue = 360f,
        colors = hueColors,
        onChange = onHueChange,
        modifier = modifier
    )
}

@Composable
private fun AlphaSlider(
    alpha: Float,
    color: Color,
    onAlphaChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    GradientSlider(
        value = alpha,
        maxValue = 1f,
        colors = listOf(color.copy(alpha = 0f), color),
        onChange = onAlphaChange,
        modifier = modifier,
        drawBackground = { drawCheckerboard(size) }
    )
}

@Composable
private fun GradientSlider(
    value: Float,
    maxValue: Float,
    colors: List<Color>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    drawBackground: (DrawScope.() -> Unit)? = null
) {
    val sliderHeight = 24.dp
    val thumbRadius = 10.dp

    Box(
        modifier = modifier
            .height(sliderHeight)
            .clip(RoundedCornerShape(sliderHeight / 2))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(sliderHeight / 2))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onChange((offset.x / size.width).coerceIn(0f, 1f) * maxValue)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onChange((change.position.x / size.width).coerceIn(0f, 1f) * maxValue)
                    }
                }
        ) {
            drawBackground?.invoke(this)
            drawRect(brush = Brush.horizontalGradient(colors), size = size)

            val thumbX = (value / maxValue) * size.width
            val thumbRadiusPx = thumbRadius.toPx()
            val center = Offset(thumbX.coerceIn(thumbRadiusPx, size.width - thumbRadiusPx), size.height / 2)
            drawCircle(color = Color.White, radius = thumbRadiusPx, center = center)
            drawCircle(color = Color.Black, radius = thumbRadiusPx, center = center, style = Stroke(width = 2f))
        }
    }
}

@Composable
private fun ColorPreview(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .background(color)
    )
}

@Composable
private fun HexTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val isValid = remember(value) { hexToColor(value) != null }

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val filtered = newValue.uppercase().filter { it in "0123456789ABCDEF#" }
            if (filtered.length <= 9) onValueChange(filtered)
        },
        label = { Text("Hex") },
        singleLine = true,
        isError = !isValid,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier
    )
}

private fun DrawScope.drawSelectionIndicator(x: Float, y: Float) {
    val radius = 8.dp.toPx()
    val center = Offset(x.coerceIn(0f, size.width), y.coerceIn(0f, size.height))
    drawCircle(color = Color.White, radius = radius, center = center, style = Stroke(width = 3f))
    drawCircle(color = Color.Black, radius = radius, center = center, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawCheckerboard(canvasSize: Size) {
    val tileSize = 6.dp.toPx()
    val cols = (canvasSize.width / tileSize).toInt() + 1
    val rows = (canvasSize.height / tileSize).toInt() + 1
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val color = if ((row + col) % 2 == 0) Color.LightGray else Color.White
            drawRect(color, Offset(col * tileSize, row * tileSize), Size(tileSize, tileSize))
        }
    }
}
