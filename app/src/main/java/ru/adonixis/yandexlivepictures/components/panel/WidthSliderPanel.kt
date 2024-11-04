package ru.adonixis.yandexlivepictures.components.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ru.adonixis.yandexlivepictures.components.WidthSlider

@Composable
fun WidthSliderPanel(
    sliderMin: Float,
    sliderMax: Float,
    animationDurationMillis: Int,
    alphaBackground: Float,
    alphaBorder: Float,
    isVisible: Boolean,
    eraserWidth: Float,
    density: Density,
    onWidthChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = animationDurationMillis)),
        exit = fadeOut(animationSpec = tween(durationMillis = animationDurationMillis)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clickable(enabled = false) { }
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaBackground),
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alphaBorder),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(16.dp)
        ) {
            WidthSlider(
                value = with(density) { eraserWidth.toDp().value },
                onValueChange = { dpValue ->
                    with(density) {
                        onWidthChange(dpValue.dp.toPx())
                    }
                },
                valueRange = sliderMin..sliderMax
            )
        }
    }
}
