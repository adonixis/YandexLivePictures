package ru.adonixis.yandexlivepictures.components.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.adonixis.yandexlivepictures.R

@Composable
fun ShapePanel(
    isVisible: Boolean,
    animationDurationMillis: Int,
    alphaBackground: Float,
    alphaBorder: Float,
    onSquareClick: () -> Unit,
    onCircleClick: () -> Unit,
    onTriangleClick: () -> Unit,
    onArrowClick: () -> Unit,
    onGenerateFramesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = animationDurationMillis)),
        exit = fadeOut(animationSpec = tween(durationMillis = animationDurationMillis)),
        modifier = modifier
    ) {
        Row(
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
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = onSquareClick
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_square_24),
                    contentDescription = "Square"
                )
            }

            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = onCircleClick
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_circle_24),
                    contentDescription = "Circle"
                )
            }

            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = onTriangleClick
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_triangle_24),
                    contentDescription = "Triangle"
                )
            }

            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = onArrowClick
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_up_24),
                    contentDescription = "Arrow up"
                )
            }

            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = onGenerateFramesClick
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_dice_24),
                    contentDescription = "Generate frames"
                )
            }
        }
    }
} 
