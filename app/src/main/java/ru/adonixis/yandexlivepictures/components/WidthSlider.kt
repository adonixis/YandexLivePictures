package ru.adonixis.yandexlivepictures.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import ru.adonixis.yandexlivepictures.theme.GreenDark
import ru.adonixis.yandexlivepictures.theme.GreenLight
import ru.adonixis.yandexlivepictures.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidthSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Slider(
        modifier = modifier.height(20.dp),
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        thumb = { WidthSliderThumb() },
        track = { WidthSliderTrack() }
    )
}

@Composable
fun WidthSliderThumb(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .background(White, CircleShape)
    )
}

@Composable
fun WidthSliderTrack(
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            GreenDark,
            GreenLight
        )
    )
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
    ) {
        val sliderWidth = size.width
        val centerY = size.height / 2
        
        val path = Path().apply {
            arcTo(
                rect = Rect(
                    left = 0f,
                    top = centerY - 2.dp.toPx(),
                    right = 4.dp.toPx(),
                    bottom = centerY + 2.dp.toPx()
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = true
            )
            
            lineTo(sliderWidth - 12.dp.toPx(), centerY - 6.dp.toPx())
            
            arcTo(
                rect = Rect(
                    left = sliderWidth - 12.dp.toPx(),
                    top = centerY - 6.dp.toPx(),
                    right = sliderWidth,
                    bottom = centerY + 6.dp.toPx()
                ),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            
            lineTo(2.dp.toPx(), centerY + 2.dp.toPx())
            
            close()
        }
        
        drawPath(
            path = path,
            brush = gradientBrush
        )
    }
} 
