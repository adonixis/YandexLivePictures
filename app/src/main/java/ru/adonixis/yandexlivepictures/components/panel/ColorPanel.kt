package ru.adonixis.yandexlivepictures.components.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.adonixis.yandexlivepictures.Colors
import ru.adonixis.yandexlivepictures.R
import kotlin.math.ceil

@Composable
fun ColorPanel(
    isVisible: Boolean,
    isExtendedColorsVisible: Boolean,
    selectedColor: Int,
    animationDurationMillis: Int,
    alphaBackground: Float,
    alphaBorder: Float,
    onColorSelected: (Int) -> Unit,
    onToggleExtendedColors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(enabled = false) { },
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = isExtendedColorsVisible,
            enter = fadeIn(
                animationSpec = tween(durationMillis = animationDurationMillis)
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = animationDurationMillis)
            )
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaBackground),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alphaBorder),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val rows = ceil((Colors.extendedColors.size.toFloat() / 5.0f)).toInt()
                for (row in 0 until rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (col in 0..4) {
                            val color = Colors.extendedColors[row * 5 + col]
                            IconButton(
                                modifier = Modifier.size(32.dp),
                                onClick = { onColorSelected(color.toArgb()) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(color = color, shape = CircleShape)
                                        .then(
                                            if (selectedColor == color.toArgb()) {
                                                Modifier.border(
                                                    width = 1.5.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                            } else {
                                                Modifier
                                            }
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(
                animationSpec = tween(durationMillis = animationDurationMillis)
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = animationDurationMillis)
            )
        ) {
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaBackground),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alphaBorder),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = onToggleExtendedColors
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_color_palette_32),
                        contentDescription = "Color palette",
                        tint = if (isExtendedColorsVisible)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                for (color in listOf(Colors.White, Colors.Red, Colors.Black, Colors.Blue)) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = { onColorSelected(color.toArgb()) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(color = color, shape = CircleShape)
                                .then(
                                    if (selectedColor == color.toArgb()) {
                                        Modifier.border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                        )
                    }
                }
            }
        }
    }
} 
