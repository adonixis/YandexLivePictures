package ru.adonixis.yandexlivepictures

import android.content.ClipData
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
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.adonixis.yandexlivepictures.components.ProgressIndicator
import ru.adonixis.yandexlivepictures.components.dialog.DeleteAllFramesDialog
import ru.adonixis.yandexlivepictures.components.dialog.DuplicateFrameDialog
import ru.adonixis.yandexlivepictures.components.dialog.GenerateFramesDialog
import ru.adonixis.yandexlivepictures.components.dialog.PlaybackSpeedDialog
import ru.adonixis.yandexlivepictures.components.WidthSlider
import ru.adonixis.yandexlivepictures.theme.Black
import ru.adonixis.yandexlivepictures.theme.YandexLivePicturesTheme
import ru.adonixis.yandexlivepictures.utils.UiExtensions.drawArrow
import ru.adonixis.yandexlivepictures.utils.UiExtensions.drawCircle
import ru.adonixis.yandexlivepictures.utils.UiExtensions.drawSmoothLine
import ru.adonixis.yandexlivepictures.utils.UiExtensions.drawSquare
import ru.adonixis.yandexlivepictures.utils.UiExtensions.drawTriangle
import ru.adonixis.yandexlivepictures.utils.UiExtensions.getDistanceTo
import kotlin.math.ceil
import kotlin.math.min
import ru.adonixis.yandexlivepictures.components.TopActionBar

private object ScreenConstants {
    const val ANIMATION_DURATION = 200
    const val ALPHA_SEMI_TRANSPARENT = 0.8f
    const val ALPHA_BORDER = 0.16f
    const val MAX_FPS = 1000
    const val SLIDER_MIN = 2f
    const val SLIDER_MAX = 100f
    const val THUMBNAIL_HEIGHT = 100f
    const val FRAME_COUNT_DEFAULT = 10
    const val PLAYBACK_SPEED_DEFAULT = 5
    const val CANVAS_SCALE_MIN = 1f
    const val CANVAS_SCALE_MAX = 5f
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val currentPath = remember { mutableStateListOf<Offset>() }
    val currentEraserPath = remember { mutableStateListOf<Offset>() }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frameCount by remember { mutableStateOf("${ScreenConstants.FRAME_COUNT_DEFAULT}") }
    var playbackSpeed by remember { mutableStateOf("${ScreenConstants.PLAYBACK_SPEED_DEFAULT}") }
    val context = LocalContext.current
    val density = LocalDensity.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var previousScale by remember { mutableFloatStateOf(1f) }
    var previousOffset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        if (state.currentTool == Tool.ZOOM) {
            val newScale = (scale * zoomChange).coerceIn(ScreenConstants.CANVAS_SCALE_MIN, ScreenConstants.CANVAS_SCALE_MAX)

            val maxX = (canvasSize.width * (newScale - 1)) / 2
            val maxY = (canvasSize.height * (newScale - 1)) / 2

            val newOffsetX = (offset.x + offsetChange.x).coerceIn(-maxX, maxX)
            val newOffsetY = (offset.y + offsetChange.y).coerceIn(-maxY, maxY)
            
            scale = newScale
            offset = Offset(newOffsetX, newOffsetY)
        }
    }

    LaunchedEffect(Unit) {
        with(density) {
            viewModel.initializeSizes(
                pencilWidth = 2.dp.toPx(),
                brushWidth = 20.dp.toPx(),
                eraserWidth = 20.dp.toPx(),
                thumbnailHeight = ScreenConstants.THUMBNAIL_HEIGHT.dp.toPx()
            )
        }
    }

    LaunchedEffect(state.gifSavingResult) {
        state.gifSavingResult?.let { result ->
            when (result) {
                is GifSavingResult.Success -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        clipData = ClipData.newRawUri(null, result.uri)
                        type = "image/gif"
                        putExtra(Intent.EXTRA_STREAM, result.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, ""))
                }
                is GifSavingResult.Error -> {
                    Toast.makeText(
                        context,
                        "${context.getString(R.string.gif_error)}: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    LaunchedEffect(state.isPlaybackActive) {
        if (state.isPlaybackActive) {
            previousScale = scale
            previousOffset = offset
            scale = 1f
            offset = Offset.Zero
        } else {
            scale = previousScale
            offset = previousOffset
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopActionBar(
            state = state,
            onAction = viewModel::onAction,
            context = context
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 22.dp)
                    .clip(shape = RoundedCornerShape(20.dp))
                    .transformable(
                        state = transformableState,
                        enabled = !state.isPlaybackActive && state.currentTool == Tool.ZOOM
                    )
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    painter = painterResource(id = R.drawable.bg_paper),
                    contentDescription = "Paper background",
                    contentScale = ContentScale.Crop
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
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

                                                    else -> { /* ignore */ }
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

                                                    else -> { /* ignore */ }
                                                }
                                            }
                                        }
                                    }

                                    Tool.SHAPES -> {
                                        awaitPointerEventScope {
                                            var initialDistance = 0f
                                            var initialScale = 1f
                                            var currentRotation = 0f
                                            var previousAngle = 0f
                                            var isDragging = false

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                when (event.type) {
                                                    PointerEventType.Press -> {
                                                        isDragging = true
                                                        if (event.changes.size == 2) {
                                                            val firstPoint =
                                                                event.changes[0].position
                                                            val secondPoint =
                                                                event.changes[1].position
                                                            initialDistance =
                                                                firstPoint.getDistanceTo(secondPoint)
                                                            val currentFrame =
                                                                state.frames[state.currentFrameIndex]
                                                            val lastAction =
                                                                currentFrame.actionHistory.getOrNull(
                                                                    currentFrame.currentHistoryPosition
                                                                )
                                                            if (lastAction is DrawAction.DrawShape) {
                                                                initialScale = lastAction.scale
                                                            }
                                                        }
                                                        previousAngle = 0f
                                                        viewModel.onAction(MainAction.HideFrameList)
                                                    }

                                                    PointerEventType.Move -> {
                                                        if (!state.isShapeMovable || !isDragging) continue

                                                        when (event.changes.size) {
                                                            1 -> {
                                                                val position =
                                                                    event.changes.first().position
                                                                viewModel.onAction(
                                                                    MainAction.UpdateShapePosition(
                                                                        position
                                                                    )
                                                                )
                                                                event.changes
                                                                    .first()
                                                                    .consume()
                                                            }

                                                            2 -> {
                                                                val firstPoint =
                                                                    event.changes[0].position
                                                                val secondPoint =
                                                                    event.changes[1].position

                                                                val center = Offset(
                                                                    (firstPoint.x + secondPoint.x) / 2f,
                                                                    (firstPoint.y + secondPoint.y) / 2f
                                                                )
                                                                viewModel.onAction(
                                                                    MainAction.UpdateShapePosition(
                                                                        center
                                                                    )
                                                                )

                                                                val currentDistance =
                                                                    firstPoint.getDistanceTo(
                                                                        secondPoint
                                                                    )
                                                                if (initialDistance > 0) {
                                                                    val scaleFactor =
                                                                        currentDistance / initialDistance
                                                                    val newScale =
                                                                        initialScale * scaleFactor
                                                                    viewModel.onAction(
                                                                        MainAction.UpdateShapeScale(
                                                                            newScale
                                                                        )
                                                                    )
                                                                }

                                                                val angle = kotlin.math.atan2(
                                                                    secondPoint.y - firstPoint.y,
                                                                    secondPoint.x - firstPoint.x
                                                                ) * 180f / kotlin.math.PI.toFloat()

                                                                if (previousAngle != 0f) {
                                                                    val deltaAngle =
                                                                        angle - previousAngle
                                                                    currentRotation =
                                                                        (currentRotation + deltaAngle) % 360f
                                                                    viewModel.onAction(
                                                                        MainAction.UpdateShapeRotation(
                                                                            currentRotation
                                                                        )
                                                                    )
                                                                }
                                                                previousAngle = angle

                                                                event.changes.forEach { it.consume() }
                                                            }
                                                        }
                                                    }

                                                    PointerEventType.Release -> {
                                                        isDragging = false
                                                        if (event.changes.size == 2) {
                                                            initialDistance = 0f
                                                            previousAngle = 0f
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    else -> { /* ignore */ }
                                }
                            }
                        }
                ) {
                    with(drawContext.canvas.nativeCanvas) {
                        val checkPoint = saveLayer(null, null)

                        if (state.isPlaybackActive) {
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
                                                Shape.Square -> drawSquare(action.center, action.size, Color(action.color), action.scale, action.rotation)
                                                Shape.Circle -> drawCircle(action.center, action.size, Color(action.color), action.scale, action.rotation)
                                                Shape.Triangle -> drawTriangle(action.center, action.size, Color(action.color), action.scale, action.rotation)
                                                Shape.Arrow -> drawArrow(action.center, action.size, Color(action.color), action.scale, action.rotation)
                                            }
                                        }
                                    }
                                }
                        } else {
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
                                                    Shape.Square -> drawSquare(action.center, action.size, Color(action.color).copy(alpha = 0.3f), action.scale, action.rotation)
                                                    Shape.Circle -> drawCircle(action.center, action.size, Color(action.color).copy(alpha = 0.3f), action.scale, action.rotation)
                                                    Shape.Triangle -> drawTriangle(action.center, action.size, Color(action.color).copy(alpha = 0.3f), action.scale, action.rotation)
                                                    Shape.Arrow -> drawArrow(action.center, action.size, Color(action.color).copy(alpha = 0.3f), action.scale, action.rotation)
                                                }
                                            }
                                        }
                                    }

                                restoreToCount(previousCheckPoint)
                            }

                            val currentCheckPoint = saveLayer(null, null)

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
                                                Shape.Square -> drawSquare(action.center, action.size, Color(action.color), action.scale, action.rotation)
                                                Shape.Circle -> drawCircle(action.center, action.size, Color(action.color), action.scale, action.rotation)
                                                Shape.Triangle -> drawTriangle(action.center, action.size, Color(action.color), action.scale, action.rotation)
                                                Shape.Arrow -> drawArrow(action.center, action.size, Color(action.color), action.scale, action.rotation)
                                            }
                                        }
                                    }
                                }

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
                this@Column.AnimatedVisibility(
                    visible = state.isFrameListVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = ScreenConstants.ANIMATION_DURATION)),
                    exit = fadeOut(animationSpec = tween(durationMillis = ScreenConstants.ANIMATION_DURATION)),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            .clickable(enabled = false) { }
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ScreenConstants.ALPHA_SEMI_TRANSPARENT),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = ScreenConstants.ALPHA_BORDER),
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
                                            .height(ScreenConstants.THUMBNAIL_HEIGHT.dp)
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
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = ScreenConstants.ALPHA_SEMI_TRANSPARENT),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                viewModel.onAction(MainAction.SelectFrame(index))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        state.frames[index].thumbnail?.let { thumbnail ->
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

                this@Column.AnimatedVisibility(
                    visible = state.currentTool == Tool.SHAPES,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = ScreenConstants.ANIMATION_DURATION)
                    ),
                    exit = fadeOut(
                        animationSpec = tween(durationMillis = ScreenConstants.ANIMATION_DURATION)
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .clickable(enabled = false) { }
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ScreenConstants.ALPHA_SEMI_TRANSPARENT),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = ScreenConstants.ALPHA_BORDER),
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
                                contentDescription = "Square"
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
                                contentDescription = "Circle"
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
                                contentDescription = "Triangle"
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
                                contentDescription = "Arrow up"
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
                    AnimatedVisibility(
                        visible = state.isExtendedColorsVisible,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = ScreenConstants.ANIMATION_DURATION)
                        ),
                        exit = fadeOut(
                            animationSpec = tween(durationMillis = ScreenConstants.ANIMATION_DURATION)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ScreenConstants.ALPHA_SEMI_TRANSPARENT),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = ScreenConstants.ALPHA_BORDER),
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

                    AnimatedVisibility(
                        visible = state.currentTool == Tool.COLORS,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = ScreenConstants.ANIMATION_DURATION)
                        ),
                        exit = fadeOut(
                            animationSpec = tween(durationMillis = ScreenConstants.ANIMATION_DURATION)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ScreenConstants.ALPHA_SEMI_TRANSPARENT),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = ScreenConstants.ALPHA_BORDER),
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
                                    contentDescription = "Color palette",
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

                this@Column.AnimatedVisibility(
                    visible = state.isEraserWidthSliderVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = ScreenConstants.ANIMATION_DURATION)),
                    exit = fadeOut(animationSpec = tween(durationMillis = ScreenConstants.ANIMATION_DURATION)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp, start = 32.dp, end = 32.dp)
                            .clickable(enabled = false) { }
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ScreenConstants.ALPHA_SEMI_TRANSPARENT),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = ScreenConstants.ALPHA_BORDER),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(16.dp)
                    ) {
                        WidthSlider(
                            value = with(density) { state.eraserWidth.toDp().value },
                            onValueChange = { dpValue ->
                                with(density) {
                                    viewModel.onAction(MainAction.UpdateEraserWidth(dpValue.dp.toPx()))
                                }
                            },
                            valueRange = ScreenConstants.SLIDER_MIN..ScreenConstants.SLIDER_MAX
                        )
                    }
                }

                this@Column.AnimatedVisibility(
                    visible = state.isBrushWidthSliderVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = ScreenConstants.ANIMATION_DURATION)),
                    exit = fadeOut(animationSpec = tween(durationMillis = ScreenConstants.ANIMATION_DURATION)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp, start = 32.dp, end = 32.dp)
                            .clickable(enabled = false) { }
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ScreenConstants.ALPHA_SEMI_TRANSPARENT),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = ScreenConstants.ALPHA_BORDER),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(16.dp)
                    ) {
                        WidthSlider(
                            value = with(density) { state.brushWidth.toDp().value },
                            onValueChange = { dpValue ->
                                with(density) {
                                    viewModel.onAction(MainAction.UpdateBrushWidth(dpValue.dp.toPx()))
                                }
                            },
                            valueRange = ScreenConstants.SLIDER_MIN..ScreenConstants.SLIDER_MAX
                        )
                    }
                }
            }
        }

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
                    contentDescription = "Pencil",
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
                    contentDescription = "Brush",
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
                    contentDescription = "Erase",
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
                    contentDescription = "Instruments",
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
                onClick = { viewModel.onAction(MainAction.SelectTool(Tool.ZOOM)) },
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

    if (state.isDeleteAllDialogVisible) {
        DeleteAllFramesDialog(onAction = viewModel::onAction)
    }

    if (state.isDuplicateFrameDialogVisible) {
        DuplicateFrameDialog(onAction = viewModel::onAction)
    }

    if (state.isGenerateFramesDialogVisible) {
        GenerateFramesDialog(
            frameCount = frameCount,
            onFrameCountChange = { frameCount = it },
            onAction = viewModel::onAction
        )
    }

    if (state.isPlaybackSpeedDialogVisible) {
        PlaybackSpeedDialog(
            maxFps = ScreenConstants.MAX_FPS,
            playbackSpeed = playbackSpeed,
            onPlaybackSpeedChange = { playbackSpeed = it },
            onAction = viewModel::onAction
        )
    }

    if (state.isSavingGif) {
        ProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    YandexLivePicturesTheme {
        MainScreen()
    }
}
