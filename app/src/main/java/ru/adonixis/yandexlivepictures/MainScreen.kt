package ru.adonixis.yandexlivepictures

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.adonixis.yandexlivepictures.theme.Blue
import ru.adonixis.yandexlivepictures.theme.YandexLivePicturesTheme
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.nativeCanvas

private fun Path.drawSmoothLine(points: List<Offset>) {
    if (points.size > 1) {
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val p0 = if (i > 0) points[i - 1] else points[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i < points.size - 2) points[i + 2] else p2
            
            val controlPoint1X = p1.x + (p2.x - p0.x) / 6
            val controlPoint1Y = p1.y + (p2.y - p0.y) / 6
            val controlPoint2X = p2.x - (p3.x - p1.x) / 6
            val controlPoint2Y = p2.y - (p3.y - p1.y) / 6
            
            cubicTo(
                controlPoint1X, controlPoint1Y,
                controlPoint2X, controlPoint2Y,
                p2.x, p2.y
            )
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val state = viewModel.state.collectAsState().value
    val currentPath = remember { mutableStateListOf<Offset>() }
    val currentEraserPath = remember { mutableStateListOf<Offset>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Верхняя панель с кнопками
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая группа кнопок (Undo/Redo)
            Row(
                modifier = Modifier.alpha(if (state.isPlaybackActive) 0f else 1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = { viewModel.onAction(MainAction.Undo) },
                    enabled = !state.isPlaybackActive && state.frames[state.currentFrameIndex].currentHistoryPosition >= 0
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_left_24),
                        contentDescription = "Undo icon",
                        tint = if (state.frames[state.currentFrameIndex].currentHistoryPosition >= 0)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = { viewModel.onAction(MainAction.Redo) },
                    enabled = !state.isPlaybackActive && 
                        state.frames[state.currentFrameIndex].currentHistoryPosition < 
                        state.frames[state.currentFrameIndex].actionHistory.size - 1
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right_24),
                        contentDescription = "Redo icon",
                        tint = if (state.frames[state.currentFrameIndex].currentHistoryPosition < 
                            state.frames[state.currentFrameIndex].actionHistory.size - 1)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Центральная группа кнопок
            Row(
                modifier = Modifier.alpha(if (state.isPlaybackActive) 0f else 1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = { viewModel.onAction(MainAction.DeleteCurrentFrame) },
                    enabled = !state.isPlaybackActive && state.frames.size > 1
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bin_32),
                        contentDescription = "Delete current frame",
                        tint = if (state.frames.size > 1)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = { viewModel.onAction(MainAction.AddNewFrame) },
                    enabled = !state.isPlaybackActive
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_file_plus_32),
                        contentDescription = "Add new frame"
                    )
                }

                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = { },
                    enabled = !state.isPlaybackActive
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_layers_32),
                        contentDescription = "Layers icon"
                    )
                }
            }

            // Правая группа кнопок (Play и Stop) - всегда видима
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = { viewModel.onAction(MainAction.StopPlayback) },
                    enabled = state.isPlaybackActive
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_stop_32),
                        contentDescription = "Stop playback",
                        tint = if (state.isPlaybackActive)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = { viewModel.onAction(MainAction.StartPlayback) },
                    enabled = !state.isPlaybackActive && state.frames.size > 1
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play_32),
                        contentDescription = "Start playback",
                        tint = if (!state.isPlaybackActive && state.frames.size > 1)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 32.dp, bottom = 22.dp)
                .clip(shape = RoundedCornerShape(20.dp))
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = R.drawable.bg_paper),
                contentDescription = "Paper background",
                contentScale = ContentScale.Crop
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.isPencilEnabled, state.isEraserEnabled) {
                        if (!state.isPlaybackActive) {
                            when {
                                state.isPencilEnabled -> {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPath.add(offset)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentPath.add(change.position)
                                        },
                                        onDragEnd = {
                                            viewModel.onAction(MainAction.AddDrawingPath(currentPath.toList()))
                                            currentPath.clear()
                                        }
                                    )
                                }
                                state.isEraserEnabled -> {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentEraserPath.add(offset)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentEraserPath.add(change.position)
                                        },
                                        onDragEnd = {
                                            viewModel.onAction(MainAction.AddEraserPath(currentEraserPath.toList()))
                                            currentEraserPath.clear()
                                        }
                                    )
                                }
                            }
                        }
                    }
            ) {
                with(drawContext.canvas.nativeCanvas) {
                    val checkPoint = saveLayer(null, null)

                    if (state.isPlaybackActive) {
                        // Отрисовка текущего кадра анимации
                        val playbackFrame = state.frames[state.playbackFrameIndex]
                        playbackFrame.actionHistory.take(playbackFrame.currentHistoryPosition + 1).forEach { action ->
                            when (action) {
                                is DrawAction.DrawPath -> {
                                    drawPath(
                                        path = Path().apply { drawSmoothLine(action.path) },
                                        color = Color.Black,
                                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                                is DrawAction.ErasePath -> {
                                    drawPath(
                                        path = Path().apply { drawSmoothLine(action.path) },
                                        color = Color.Transparent,
                                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                                        blendMode = BlendMode.Clear
                                    )
                                }
                            }
                        }
                    } else {
                        // Слой для предыдущего кадра
                        if (state.currentFrameIndex > 0) {
                            val previousCheckPoint = saveLayer(null, null)
                            
                            val previousFrame = state.frames[state.currentFrameIndex - 1]
                            previousFrame.actionHistory.take(previousFrame.currentHistoryPosition + 1).forEach { action ->
                                when (action) {
                                    is DrawAction.DrawPath -> {
                                        drawPath(
                                            path = Path().apply { drawSmoothLine(action.path) },
                                            color = Color.Gray.copy(alpha = 0.3f),
                                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                        )
                                    }
                                    is DrawAction.ErasePath -> {
                                        drawPath(
                                            path = Path().apply { drawSmoothLine(action.path) },
                                            color = Color.Transparent,
                                            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                                            blendMode = BlendMode.Clear
                                        )
                                    }
                                }
                            }
                            
                            restoreToCount(previousCheckPoint)
                        }

                        // Отдельный слой для текущего кадра
                        val currentCheckPoint = saveLayer(null, null)

                        // Рисуем текущий кадр
                        val currentFrame = state.frames[state.currentFrameIndex]
                        currentFrame.actionHistory.take(currentFrame.currentHistoryPosition + 1).forEach { action ->
                            when (action) {
                                is DrawAction.DrawPath -> {
                                    drawPath(
                                        path = Path().apply { drawSmoothLine(action.path) },
                                        color = Color.Black,
                                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                                is DrawAction.ErasePath -> {
                                    drawPath(
                                        path = Path().apply { drawSmoothLine(action.path) },
                                        color = Color.Transparent,
                                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                                        blendMode = BlendMode.Clear
                                    )
                                }
                            }
                        }

                        // Отрисовка текущих путей
                        if (currentPath.size > 1) {
                            drawPath(
                                path = Path().apply { drawSmoothLine(currentPath) },
                                color = Color.Black,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        if (currentEraserPath.size > 1) {
                            drawPath(
                                path = Path().apply { drawSmoothLine(currentEraserPath) },
                                color = Color.Transparent,
                                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                                blendMode = BlendMode.Clear
                            )
                        }

                        restoreToCount(currentCheckPoint)
                    }

                    restoreToCount(checkPoint)
                }
            }
        }

        // Нижняя панель с инструментами
        Row(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .height(32.dp)
                .alpha(if (state.isPlaybackActive) 0f else 1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = { viewModel.onAction(MainAction.TogglePencilTool) },
                enabled = !state.isPlaybackActive
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pencil_32),
                    contentDescription = "Pencil icon",
                    tint = if (state.isPencilEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = { },
                enabled = !state.isPlaybackActive
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_brush_32),
                    contentDescription = "Brush icon"
                )
            }

            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = { viewModel.onAction(MainAction.ToggleEraserTool) },
                enabled = !state.isPlaybackActive
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_erase_32),
                    contentDescription = "Erase icon",
                    tint = if (state.isEraserEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = { },
                enabled = !state.isPlaybackActive
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_instruments_32),
                    contentDescription = "Instruments icon"
                )
            }

            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = { },
                enabled = !state.isPlaybackActive
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = Blue, shape = CircleShape),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    YandexLivePicturesTheme {
        MainScreen()
    }
}
