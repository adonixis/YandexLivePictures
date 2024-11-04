package ru.adonixis.yandexlivepictures.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.adonixis.yandexlivepictures.MainAction
import ru.adonixis.yandexlivepictures.MainState
import ru.adonixis.yandexlivepictures.R
import ru.adonixis.yandexlivepictures.Tool

@Composable
fun BottomToolBar(
    state: MainState,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(bottom = 14.dp)
            .height(36.dp)
            .alpha(if (state.isPlaybackActive) 0f else 1f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier.size(36.dp),
            onClick = { onAction(MainAction.SelectTool(Tool.PENCIL)) },
            enabled = !state.isPlaybackActive
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_pencil_32),
                contentDescription = "Pencil",
                tint = if (state.currentTool == Tool.PENCIL)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(
            modifier = Modifier.size(36.dp),
            onClick = { onAction(MainAction.SelectTool(Tool.BRUSH)) },
            enabled = !state.isPlaybackActive
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_brush_32),
                contentDescription = "Brush",
                tint = if (state.currentTool == Tool.BRUSH)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(
            modifier = Modifier.size(36.dp),
            onClick = { onAction(MainAction.SelectTool(Tool.ERASER)) },
            enabled = !state.isPlaybackActive
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_erase_32),
                contentDescription = "Erase",
                tint = if (state.currentTool == Tool.ERASER)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(
            modifier = Modifier.size(36.dp),
            onClick = { onAction(MainAction.SelectTool(Tool.SHAPES)) },
            enabled = !state.isPlaybackActive
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_instruments_32),
                contentDescription = "Instruments",
                tint = if (state.currentTool == Tool.SHAPES)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(
            modifier = Modifier.size(36.dp),
            onClick = { onAction(MainAction.SelectTool(Tool.COLORS)) },
            enabled = !state.isPlaybackActive
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = Color(state.selectedColor),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (state.currentTool == Tool.COLORS)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onBackground,
                        shape = CircleShape
                    ),
            )
        }

        IconButton(
            modifier = Modifier.size(36.dp),
            onClick = { onAction(MainAction.SelectTool(Tool.ZOOM)) },
            enabled = !state.isPlaybackActive
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_zoom_32),
                contentDescription = "Zoom",
                tint = if (state.currentTool == Tool.ZOOM)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
} 