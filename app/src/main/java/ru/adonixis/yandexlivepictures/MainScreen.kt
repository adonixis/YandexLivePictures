package ru.adonixis.yandexlivepictures

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import ru.adonixis.yandexlivepictures.theme.Black
import ru.adonixis.yandexlivepictures.theme.White
import ru.adonixis.yandexlivepictures.theme.YandexLivePicturesTheme
import kotlin.math.ceil
import kotlin.math.min

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

private fun DrawScope.drawSquare(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x - size/2, center.y - size/2)
        lineTo(center.x + size/2, center.y - size/2)
        lineTo(center.x + size/2, center.y + size/2)
        lineTo(center.x - size/2, center.y + size/2)
        close()
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawCircle(center: Offset, size: Float, color: Color) {
    drawCircle(
        color = color,
        radius = size/2,
        center = center,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawTriangle(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - size/2)
        lineTo(center.x + size/2, center.y + size/2)
        lineTo(center.x - size/2, center.y + size/2)
        close()
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawArrow(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - size/2)
        lineTo(center.x - size/3, center.y - size/6)
        moveTo(center.x, center.y - size/2)
        lineTo(center.x + size/3, center.y - size/6)
        moveTo(center.x, center.y - size/2)
        lineTo(center.x, center.y + size/2)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

@Composable
private fun SliderThumb() {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(White, CircleShape)
    )
}

@Composable
private fun SliderTrack(
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF9CCB0C),
            Color(0xFFEAFFAB)
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val currentPath = remember { mutableStateListOf<Offset>() }
    val currentEraserPath = remember { mutableStateListOf<Offset>() }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frameCount by remember { mutableStateOf("10") }
    var playbackSpeed by remember { mutableStateOf("5") }
    val context = LocalContext.current
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        with(density) {
            viewModel.initializeSizes(
                pencilWidth = 2.dp.toPx(),
                brushWidth = 20.dp.toPx(),
                eraserWidth = 20.dp.toPx(),
                thumbnailHeight = 100.dp.toPx()
            )
        }
    }

    LaunchedEffect(state.gifSavingResult) {
        state.gifSavingResult?.let { result ->
            when (result) {
                is GifSavingResult.Success -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/gif"
                        putExtra(Intent.EXTRA_STREAM, result.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share GIF"))
                }
                is GifSavingResult.Error -> {
                    Toast.makeText(
                        context,
                        "Ошибка при сохранении GIF: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    if (state.isGenerateFramesDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(MainAction.HideGenerateFramesDialog) },
            title = { Text("Генерация кадров") },
            text = {
                TextField(
                    value = frameCount,
                    onValueChange = { text ->
                        if (text.isEmpty()) {
                            frameCount = ""
                        } else if ((text.toIntOrNull() ?: 0) > 0) {
                            frameCount = text
                        }
                    },
                    label = { Text("Количество кадров") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = (frameCount.toIntOrNull() ?: 0) > 0,
                    onClick = {
                        frameCount.toIntOrNull()?.let {
                            if (it > 0)
                                viewModel.onAction(MainAction.GenerateBouncingBallFrames(it))
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onAction(MainAction.HideGenerateFramesDialog) }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    if (state.isPlaybackSpeedDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(MainAction.HidePlaybackSpeedDialog) },
            title = { Text("Скорость воспроизведения") },
            text = {
                TextField(
                    value = playbackSpeed,
                    onValueChange = { text ->
                        if (text.isEmpty()) {
                            playbackSpeed = ""
                        } else if (((text.toIntOrNull() ?: 0) > 0) && ((text.toIntOrNull() ?: 0) <= 1000)) {
                            playbackSpeed = text
                        }
                    },
                    label = { Text("Кадов в секунду (от 0 до 1000)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = ((playbackSpeed.toIntOrNull() ?: 0) > 0) && ((playbackSpeed.toIntOrNull() ?: 0) <= 1000),
                    onClick = {
                        playbackSpeed.toIntOrNull()?.let {
                            if (it in 1..1000)
                                viewModel.onAction(MainAction.UpdatePlaybackSpeed(it))
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onAction(MainAction.HidePlaybackSpeedDialog) }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Верхняя панель с кнопками
        Row(
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .height(36.dp),
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            enabled = !state.isPlaybackActive,
                            onClick = {
                                viewModel.onAction(MainAction.DeleteCurrentFrame)
                            },
                            onLongClick = {
                                viewModel.onAction(MainAction.ShowDeleteAllDialog)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bin_32),
                        contentDescription = "Delete current frame"
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            enabled = !state.isPlaybackActive,
                            onClick = {
                                viewModel.onAction(MainAction.AddNewFrame)
                            },
                            onLongClick = {
                                viewModel.onAction(MainAction.ShowDuplicateFrameDialog)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_file_plus_32),
                        contentDescription = "Add new frame"
                    )
                }

                IconButton(
                    modifier = Modifier.size(36.dp),
                    onClick = {
                        if (state.isFrameListVisible)
                            viewModel.onAction(MainAction.HideFrameList)
                        else
                            viewModel.onAction(MainAction.ShowFrameList)
                    },
                    enabled = !state.isPlaybackActive
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_layers_32),
                        contentDescription = "Layers icon",
                        tint = if (state.isFrameListVisible)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Правая группа кнопок (Save, Play и Stop) - всегда видима
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.isPlaybackActive) {
                    IconButton(
                        modifier = Modifier.size(36.dp),
                        onClick = {
                            viewModel.onAction(MainAction.StopPlayback)
                            viewModel.onAction(MainAction.SaveAsGif(context))
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_save_32),
                            contentDescription = "Save as GIF"
                        )
                    }
                }

                IconButton(
                    modifier = Modifier.size(36.dp),
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

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            enabled = !state.isPlaybackActive,
                            onClick = {
                                viewModel.onAction(MainAction.StartPlayback)
                            },
                            onLongClick = {
                                viewModel.onAction(MainAction.ShowPlaybackSpeedDialog)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play_32),
                        contentDescription = "Start playback",
                        tint = if (!state.isPlaybackActive)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                        .onSizeChanged {
                            canvasSize = it.toSize()
                            viewModel.onAction(MainAction.UpdateCanvasSize(canvasSize))
                        }
                        .pointerInput(state.currentTool) {
                            if (!state.isPlaybackActive) {
                                when (state.currentTool) {
                                    Tool.PENCIL, Tool.BRUSH -> {
                                        awaitPointerEventScope {
                                            var path = mutableListOf<Offset>()
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                when (event.type) {
                                                    PointerEventType.Press -> {
                                                        val position =
                                                            event.changes.first().position
                                                        path = mutableListOf(position)
                                                        currentPath.clear()
                                                        currentPath.addAll(path)

                                                        viewModel.onAction(MainAction.HideBrushWidthSlider)
                                                        viewModel.onAction(MainAction.HideEraserWidthSlider)
                                                        viewModel.onAction(MainAction.HideFrameList)
                                                    }

                                                    PointerEventType.Move -> {
                                                        val position =
                                                            event.changes.first().position
                                                        path.add(position)
                                                        currentPath.clear()
                                                        currentPath.addAll(path)
                                                    }

                                                    PointerEventType.Release -> {
                                                        if (path.size == 1) {
                                                            path.add(path.first())
                                                        }
                                                        viewModel.onAction(
                                                            MainAction.AddDrawingPath(path)
                                                        )
                                                        path = mutableListOf()
                                                        currentPath.clear()
                                                    }

                                                    else -> { /* ignore */
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Tool.ERASER -> {
                                        awaitPointerEventScope {
                                            var path = mutableListOf<Offset>()
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                when (event.type) {
                                                    PointerEventType.Press -> {
                                                        val position =
                                                            event.changes.first().position
                                                        path = mutableListOf(position)
                                                        currentEraserPath.clear()
                                                        currentEraserPath.addAll(path)

                                                        viewModel.onAction(MainAction.HideEraserWidthSlider)
                                                        viewModel.onAction(MainAction.HideBrushWidthSlider)
                                                        viewModel.onAction(MainAction.HideFrameList)
                                                    }

                                                    PointerEventType.Move -> {
                                                        val position =
                                                            event.changes.first().position
                                                        path.add(position)
                                                        currentEraserPath.clear()
                                                        currentEraserPath.addAll(path)
                                                    }

                                                    PointerEventType.Release -> {
                                                        if (path.size == 1) {
                                                            path.add(path.first())
                                                        }
                                                        viewModel.onAction(
                                                            MainAction.AddEraserPath(path)
                                                        )
                                                        path = mutableListOf()
                                                        currentEraserPath.clear()
                                                    }

                                                    else -> { /* ignore */
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    else -> { /* ignore */
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
                            playbackFrame.actionHistory.take(playbackFrame.currentHistoryPosition + 1)
                                .forEach { action ->
                                    when (action) {
                                        is DrawAction.DrawPath -> {
                                            drawPath(
                                                path = Path().apply { drawSmoothLine(action.path) },
                                                color = Color(action.color),
                                                style = Stroke(
                                                    width = action.width,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }

                                        is DrawAction.ErasePath -> {
                                            drawPath(
                                                path = Path().apply { drawSmoothLine(action.path) },
                                                color = Color.Transparent,
                                                style = Stroke(
                                                    width = action.width,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                ),
                                                blendMode = BlendMode.Clear
                                            )
                                        }
                                        is DrawAction.DrawShape -> {
                                            when (action.shape) {
                                                Shape.Square -> drawSquare(action.center, action.size, Color(action.color))
                                                Shape.Circle -> drawCircle(action.center, action.size, Color(action.color))
                                                Shape.Triangle -> drawTriangle(action.center, action.size, Color(action.color))
                                                Shape.Arrow -> drawArrow(action.center, action.size, Color(action.color))
                                            }
                                        }
                                    }
                                }
                        } else {
                            // Слой для предыдущего кадра
                            if (state.currentFrameIndex > 0) {
                                val previousCheckPoint = saveLayer(null, null)

                                val previousFrame = state.frames[state.currentFrameIndex - 1]
                                previousFrame.actionHistory.take(previousFrame.currentHistoryPosition + 1)
                                    .forEach { action ->
                                        when (action) {
                                            is DrawAction.DrawPath -> {
                                                drawPath(
                                                    path = Path().apply { drawSmoothLine(action.path) },
                                                    color = Color(action.color).copy(alpha = 0.3f),
                                                    style = Stroke(
                                                        width = action.width,
                                                        cap = StrokeCap.Round,
                                                        join = StrokeJoin.Round
                                                    )
                                                )
                                            }

                                            is DrawAction.ErasePath -> {
                                                drawPath(
                                                    path = Path().apply { drawSmoothLine(action.path) },
                                                    color = Color.Transparent,
                                                    style = Stroke(
                                                        width = action.width,
                                                        cap = StrokeCap.Round,
                                                        join = StrokeJoin.Round
                                                    ),
                                                    blendMode = BlendMode.Clear
                                                )
                                            }
                                            is DrawAction.DrawShape -> {
                                                when (action.shape) {
                                                    Shape.Square -> drawSquare(action.center, action.size, Color(action.color).copy(alpha = 0.3f))
                                                    Shape.Circle -> drawCircle(action.center, action.size, Color(action.color).copy(alpha = 0.3f))
                                                    Shape.Triangle -> drawTriangle(action.center, action.size, Color(action.color).copy(alpha = 0.3f))
                                                    Shape.Arrow -> drawArrow(action.center, action.size, Color(action.color).copy(alpha = 0.3f))
                                                }
                                            }
                                        }
                                    }

                                restoreToCount(previousCheckPoint)
                            }

                            // Отдельный слой для текущего кадра
                            val currentCheckPoint = saveLayer(null, null)

                            // Рисуем текущий кадр
                            val currentFrame = state.frames[state.currentFrameIndex]
                            currentFrame.actionHistory.take(currentFrame.currentHistoryPosition + 1)
                                .forEach { action ->
                                    when (action) {
                                        is DrawAction.DrawPath -> {
                                            drawPath(
                                                path = Path().apply { drawSmoothLine(action.path) },
                                                color = Color(action.color),
                                                style = Stroke(
                                                    width = action.width,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }

                                        is DrawAction.ErasePath -> {
                                            drawPath(
                                                path = Path().apply { drawSmoothLine(action.path) },
                                                color = Color.Transparent,
                                                style = Stroke(
                                                    width = action.width,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                ),
                                                blendMode = BlendMode.Clear
                                            )
                                        }
                                        is DrawAction.DrawShape -> {
                                            when (action.shape) {
                                                Shape.Square -> drawSquare(action.center, action.size, Color(action.color))
                                                Shape.Circle -> drawCircle(action.center, action.size, Color(action.color))
                                                Shape.Triangle -> drawTriangle(action.center, action.size, Color(action.color))
                                                Shape.Arrow -> drawArrow(action.center, action.size, Color(action.color))
                                            }
                                        }
                                    }
                                }

                            // Отрисовка текущих путей
                            if (currentPath.size > 1) {
                                drawPath(
                                    path = Path().apply { drawSmoothLine(currentPath) },
                                    color = Color(state.selectedColor),
                                    style = Stroke(
                                        width = if (state.currentTool == Tool.BRUSH) state.brushWidth else state.pencilWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            if (currentEraserPath.isNotEmpty()) {
                                drawPath(
                                    path = Path().apply { drawSmoothLine(currentEraserPath) },
                                    color = Color.Transparent,
                                    style = Stroke(
                                        width = state.eraserWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    ),
                                    blendMode = BlendMode.Clear
                                )
                                
                                // Добавляем индикатор размера ластика
                                drawCircle(
                                    color = Black.copy(alpha = 0.5f),
                                    radius = state.eraserWidth / 2,
                                    center = currentEraserPath.last(),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                                    )
                                )
                            }

                            restoreToCount(currentCheckPoint)
                        }

                        restoreToCount(checkPoint)
                    }
                }
            }

            if (!state.isPlaybackActive) {
                // Панель с кадрами
                this@Column.AnimatedVisibility(
                    visible = state.isFrameListVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 200)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            .clickable(enabled = false) { }
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(12.dp)
                    ) {
                        val listState = rememberLazyListState()

                        LaunchedEffect(state.isFrameListVisible, state.currentFrameIndex) {
                            if (state.isFrameListVisible) {
                                val isItemVisible = listState.layoutInfo.visibleItemsInfo.any { 
                                    it.index == state.currentFrameIndex 
                                }

                                if (!isItemVisible) {
                                    listState.animateScrollToItem(state.currentFrameIndex)
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            state = listState
                        ) {
                            items(state.frames.indices.toList()) { index ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(100.dp)
                                            .aspectRatio(
                                                state.canvasSize.width / state.canvasSize.height
                                            )
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                color = if (index == state.currentFrameIndex)
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                else
                                                    MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (index == state.currentFrameIndex)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                viewModel.onAction(MainAction.SelectFrame(index))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        state.thumbnails[index]?.let { thumbnail ->
                                            Image(
                                                bitmap = thumbnail,
                                                contentDescription = "Frame thumbnail",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (index == state.currentFrameIndex)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    Color.Black
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            modifier = Modifier.size(24.dp),
                                            onClick = {
                                                viewModel.onAction(MainAction.SelectFrame(index))
                                                viewModel.onAction(MainAction.DuplicateCurrentFrame)
                                            }
                                        ) {
                                            Icon(
                                                modifier = Modifier.size(16.dp),
                                                painter = painterResource(id = R.drawable.ic_duplicate_24),
                                                contentDescription = "Duplicate frame",
                                                tint = Black
                                            )
                                        }

                                        IconButton(
                                            modifier = Modifier.size(24.dp),
                                            onClick = {
                                                viewModel.onAction(MainAction.SelectFrame(index))
                                                viewModel.onAction(MainAction.DeleteCurrentFrame)
                                            }
                                        ) {
                                            Icon(
                                                modifier = Modifier.size(16.dp),
                                                painter = painterResource(id = R.drawable.ic_bin_32),
                                                contentDescription = "Delete frame",
                                                tint = Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Панель с фигурами
                this@Column.AnimatedVisibility(
                    visible = state.currentTool == Tool.SHAPES,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 200)
                    ),
                    exit = fadeOut(
                        animationSpec = tween(durationMillis = 200)
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .clickable(enabled = false) { }
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = {
                                viewModel.onAction(
                                    MainAction.AddShape(
                                        shape = Shape.Square,
                                        center = Offset(canvasSize.width / 2, canvasSize.height / 2),
                                        size = min(canvasSize.width, canvasSize.height) * 0.8f
                                    )
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_square_24),
                                contentDescription = "Square icon"
                            )
                        }

                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = {
                                viewModel.onAction(
                                    MainAction.AddShape(
                                        shape = Shape.Circle,
                                        center = Offset(canvasSize.width / 2, canvasSize.height / 2),
                                        size = min(canvasSize.width, canvasSize.height) * 0.8f
                                    )
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_circle_24),
                                contentDescription = "Circle icon"
                            )
                        }

                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = {
                                viewModel.onAction(
                                    MainAction.AddShape(
                                        shape = Shape.Triangle,
                                        center = Offset(canvasSize.width / 2, canvasSize.height / 2),
                                        size = min(canvasSize.width, canvasSize.height) * 0.8f
                                    )
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_triangle_24),
                                contentDescription = "Triangle icon"
                            )
                        }

                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = {
                                viewModel.onAction(
                                    MainAction.AddShape(
                                        shape = Shape.Arrow,
                                        center = Offset(canvasSize.width / 2, canvasSize.height / 2),
                                        size = min(canvasSize.width, canvasSize.height) * 0.8f
                                    )
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_up_24),
                                contentDescription = "Arrow up icon"
                            )
                        }

                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = { viewModel.onAction(MainAction.ShowGenerateFramesDialog) }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_dice_24),
                                contentDescription = "Generate frames"
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clickable(enabled = false) { },
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Расширенная панель с цветами
                    AnimatedVisibility(
                        visible = state.isExtendedColorsVisible,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 200)
                        ),
                        exit = fadeOut(
                            animationSpec = tween(durationMillis = 200)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
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
                                            onClick = {
                                                viewModel.onAction(MainAction.SelectColor(color.toArgb()))
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(color = color, shape = CircleShape)
                                                    .then(
                                                        if (state.selectedColor == color.toArgb()) {
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

                    // Панель с цветами
                    AnimatedVisibility(
                        visible = state.currentTool == Tool.COLORS,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 200)
                        ),
                        exit = fadeOut(
                            animationSpec = tween(durationMillis = 200)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                modifier = Modifier.size(32.dp),
                                onClick = { viewModel.onAction(MainAction.ToggleExtendedColorsPanel) }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_color_palette_32),
                                    contentDescription = "Color palette icon",
                                    tint = if (state.isExtendedColorsVisible)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                modifier = Modifier.size(32.dp),
                                onClick = {
                                    viewModel.onAction(MainAction.SelectColor(Colors.White.toArgb()))
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(color = Colors.White, shape = CircleShape)
                                        .then(
                                            if (state.selectedColor == Colors.White.toArgb()) {
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

                            IconButton(
                                modifier = Modifier.size(32.dp),
                                onClick = {
                                    viewModel.onAction(MainAction.SelectColor(Colors.Red.toArgb()))
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(color = Colors.Red, shape = CircleShape)
                                        .then(
                                            if (state.selectedColor == Colors.Red.toArgb()) {
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

                            IconButton(
                                modifier = Modifier.size(32.dp),
                                onClick = {
                                    viewModel.onAction(MainAction.SelectColor(Colors.Black.toArgb()))
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(color = Colors.Black, shape = CircleShape)
                                        .then(
                                            if (state.selectedColor == Colors.Black.toArgb()) {
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

                            IconButton(
                                modifier = Modifier.size(32.dp),
                                onClick = {
                                    viewModel.onAction(MainAction.SelectColor(Colors.Blue.toArgb()))
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(color = Colors.Blue, shape = CircleShape)
                                        .then(
                                            if (state.selectedColor == Colors.Blue.toArgb()) {
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

                // Панель со слайдером ластика
                this@Column.AnimatedVisibility(
                    visible = state.isEraserWidthSliderVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 200)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp, start = 32.dp, end = 32.dp)
                            .clickable(enabled = false) { }
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Slider(
                            modifier = Modifier.height(20.dp),
                            value = with(density) { state.eraserWidth.toDp().value },
                            onValueChange = { dpValue ->
                                with(density) {
                                    viewModel.onAction(MainAction.UpdateEraserWidth(dpValue.dp.toPx()))
                                }
                            },
                            valueRange = 2f..100f,
                            thumb = { SliderThumb() },
                            track = { SliderTrack() }
                        )
                    }
                }

                // Панель со слайдером кисти
                this@Column.AnimatedVisibility(
                    visible = state.isBrushWidthSliderVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 200)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp, start = 32.dp, end = 32.dp)
                            .clickable(enabled = false) { }
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Slider(
                            modifier = Modifier.height(20.dp),
                            value = with(density) { state.brushWidth.toDp().value },
                            onValueChange = { dpValue ->
                                with(density) {
                                    viewModel.onAction(MainAction.UpdateBrushWidth(dpValue.dp.toPx()))
                                }
                            },
                            valueRange = 2f..100f,
                            thumb = { SliderThumb() },
                            track = { SliderTrack() }
                        )
                    }
                }
            }
        }

        // Нижняя панель с инструментами
        Row(
            modifier = Modifier
                .padding(bottom = 14.dp)
                .height(36.dp)
                .alpha(if (state.isPlaybackActive) 0f else 1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { viewModel.onAction(MainAction.SelectTool(Tool.PENCIL)) },
                enabled = !state.isPlaybackActive
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pencil_32),
                    contentDescription = "Pencil icon",
                    tint = if (state.currentTool == Tool.PENCIL)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { viewModel.onAction(MainAction.SelectTool(Tool.BRUSH)) },
                enabled = !state.isPlaybackActive
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_brush_32),
                    contentDescription = "Brush icon",
                    tint = if (state.currentTool == Tool.BRUSH)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { viewModel.onAction(MainAction.SelectTool(Tool.ERASER)) },
                enabled = !state.isPlaybackActive
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_erase_32),
                    contentDescription = "Erase icon",
                    tint = if (state.currentTool == Tool.ERASER)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { viewModel.onAction(MainAction.SelectTool(Tool.SHAPES)) },
                enabled = !state.isPlaybackActive
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_instruments_32),
                    contentDescription = "Instruments icon",
                    tint = if (state.currentTool == Tool.SHAPES)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { viewModel.onAction(MainAction.SelectTool(Tool.COLORS)) },
                enabled = !state.isPlaybackActive
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = Color(state.selectedColor),
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
        }
    }

    if (state.isDeleteAllDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(MainAction.HideDeleteAllDialog) },
            title = { Text("Удаление всех кадров") },
            text = { Text("Все кадры будут удалены") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onAction(MainAction.DeleteAllFrames) }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onAction(MainAction.HideDeleteAllDialog) }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    if (state.isDuplicateFrameDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(MainAction.HideDuplicateFrameDialog) },
            title = { Text("Дублирование кадра") },
            text = { Text("Текущий кадр будет продублирован") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onAction(MainAction.DuplicateCurrentFrame) }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onAction(MainAction.HideDuplicateFrameDialog) }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // Добавляем индикатор прогресса
    if (state.isSavingGif) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (state.isSavingGif) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) { }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                }
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
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
