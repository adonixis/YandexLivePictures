package ru.adonixis.yandexlivepictures

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import ru.adonixis.yandexlivepictures.components.BottomToolBar
import ru.adonixis.yandexlivepictures.components.ProgressIndicator
import ru.adonixis.yandexlivepictures.components.TopActionBar
import ru.adonixis.yandexlivepictures.components.dialog.DeleteAllFramesDialog
import ru.adonixis.yandexlivepictures.components.dialog.DuplicateFrameDialog
import ru.adonixis.yandexlivepictures.components.dialog.GenerateFramesDialog
import ru.adonixis.yandexlivepictures.components.dialog.PlaybackSpeedDialog
import ru.adonixis.yandexlivepictures.components.panel.ColorPanel
import ru.adonixis.yandexlivepictures.components.panel.FrameListPanel
import ru.adonixis.yandexlivepictures.components.panel.ShapePanel
import ru.adonixis.yandexlivepictures.components.panel.WidthSliderPanel
import ru.adonixis.yandexlivepictures.theme.Black
import ru.adonixis.yandexlivepictures.theme.YandexLivePicturesTheme
import ru.adonixis.yandexlivepictures.utils.UiExtensions.drawArrow
import ru.adonixis.yandexlivepictures.utils.UiExtensions.drawCircle
import ru.adonixis.yandexlivepictures.utils.UiExtensions.drawSmoothLine
import ru.adonixis.yandexlivepictures.utils.UiExtensions.drawSquare
import ru.adonixis.yandexlivepictures.utils.UiExtensions.drawTriangle
import ru.adonixis.yandexlivepictures.utils.UiExtensions.getDistanceTo
import kotlin.math.min

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

                                val previousFrameLayer = saveLayer(null, null)

                                val previousFrame = state.frames[state.currentFrameIndex - 1]
                                previousFrame.actionHistory.take(previousFrame.currentHistoryPosition + 1)
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

                                restoreToCount(previousFrameLayer)
                                drawRect(
                                    color = Color.White.copy(alpha = 0.3f),
                                    blendMode = BlendMode.DstIn
                                )

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
                FrameListPanel(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    isVisible = state.isFrameListVisible,
                    currentFrameIndex = state.currentFrameIndex,
                    frames = state.frames,
                    canvasSize = canvasSize,
                    thumbnailHeight = ScreenConstants.THUMBNAIL_HEIGHT.dp,
                    animationDurationMillis = ScreenConstants.ANIMATION_DURATION,
                    alphaBackground = ScreenConstants.ALPHA_SEMI_TRANSPARENT,
                    alphaBorder = ScreenConstants.ALPHA_BORDER,
                    onFrameSelected = { index ->
                        viewModel.onAction(MainAction.SelectFrame(index))
                    },
                    onFrameDuplicated = { index ->
                        viewModel.onAction(MainAction.SelectFrame(index))
                        viewModel.onAction(MainAction.DuplicateCurrentFrame)
                    },
                    onFrameDeleted = { index ->
                        viewModel.onAction(MainAction.SelectFrame(index))
                        viewModel.onAction(MainAction.DeleteCurrentFrame)
                    }
                )

                ShapePanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    isVisible = state.currentTool == Tool.SHAPES,
                    animationDurationMillis = ScreenConstants.ANIMATION_DURATION,
                    alphaBackground = ScreenConstants.ALPHA_SEMI_TRANSPARENT,
                    alphaBorder = ScreenConstants.ALPHA_BORDER,
                    onSquareClick = {
                        viewModel.onAction(
                            MainAction.AddShape(
                                shape = Shape.Square,
                                center = Offset(canvasSize.width / 2, canvasSize.height / 2),
                                size = min(canvasSize.width, canvasSize.height) * 0.8f
                            )
                        )
                    },
                    onCircleClick = {
                        viewModel.onAction(
                            MainAction.AddShape(
                                shape = Shape.Circle,
                                center = Offset(canvasSize.width / 2, canvasSize.height / 2),
                                size = min(canvasSize.width, canvasSize.height) * 0.8f
                            )
                        )
                    },
                    onTriangleClick = {
                        viewModel.onAction(
                            MainAction.AddShape(
                                shape = Shape.Triangle,
                                center = Offset(canvasSize.width / 2, canvasSize.height / 2),
                                size = min(canvasSize.width, canvasSize.height) * 0.8f
                            )
                        )
                    },
                    onArrowClick = {
                        viewModel.onAction(
                            MainAction.AddShape(
                                shape = Shape.Arrow,
                                center = Offset(canvasSize.width / 2, canvasSize.height / 2),
                                size = min(canvasSize.width, canvasSize.height) * 0.8f
                            )
                        )
                    },
                    onGenerateFramesClick = {
                        viewModel.onAction(MainAction.ShowGenerateFramesDialog)
                    }
                )

                ColorPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    isVisible = state.currentTool == Tool.COLORS,
                    isExtendedColorsVisible = state.isExtendedColorsVisible,
                    selectedColor = state.selectedColor,
                    animationDurationMillis = ScreenConstants.ANIMATION_DURATION,
                    alphaBackground = ScreenConstants.ALPHA_SEMI_TRANSPARENT,
                    alphaBorder = ScreenConstants.ALPHA_BORDER,
                    onColorSelected = { color ->
                        viewModel.onAction(MainAction.SelectColor(color))
                    },
                    onToggleExtendedColors = {
                        viewModel.onAction(MainAction.ToggleExtendedColorsPanel)
                    }
                )

                WidthSliderPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 32.dp, end = 32.dp),
                    sliderMin = ScreenConstants.SLIDER_MIN,
                    sliderMax = ScreenConstants.SLIDER_MAX,
                    animationDurationMillis = ScreenConstants.ANIMATION_DURATION,
                    alphaBackground = ScreenConstants.ALPHA_SEMI_TRANSPARENT,
                    alphaBorder = ScreenConstants.ALPHA_BORDER,
                    isVisible = state.isEraserWidthSliderVisible,
                    eraserWidth = state.eraserWidth,
                    density = density,
                    onWidthChange = { width ->
                        viewModel.onAction(MainAction.UpdateEraserWidth(width))
                    }
                )

                WidthSliderPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 32.dp, end = 32.dp),
                    sliderMin = ScreenConstants.SLIDER_MIN,
                    sliderMax = ScreenConstants.SLIDER_MAX,
                    animationDurationMillis = ScreenConstants.ANIMATION_DURATION,
                    alphaBackground = ScreenConstants.ALPHA_SEMI_TRANSPARENT,
                    alphaBorder = ScreenConstants.ALPHA_BORDER,
                    isVisible = state.isBrushWidthSliderVisible,
                    eraserWidth = state.brushWidth,
                    density = density,
                    onWidthChange = { width ->
                        viewModel.onAction(MainAction.UpdateBrushWidth(width))
                    }
                )
            }
        }

        BottomToolBar(
            state = state,
            onAction = viewModel::onAction
        )
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
