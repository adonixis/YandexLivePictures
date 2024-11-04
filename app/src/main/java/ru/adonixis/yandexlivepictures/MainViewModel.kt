package ru.adonixis.yandexlivepictures

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.gifencoder.AnimatedGifEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider.getUriForFile

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.SelectTool -> {
                _state.update { currentState ->
                    currentState.copy(
                        currentTool = action.tool,
                        isExtendedColorsVisible = false,
                        isEraserWidthSliderVisible = action.tool == Tool.ERASER,
                        isBrushWidthSliderVisible = action.tool == Tool.BRUSH,
                        previousTool = currentState.currentTool,
                    )
                }
            }
            is MainAction.SelectColor -> {
                _state.update { currentState ->
                    currentState.copy(
                        selectedColor = action.color,
                        currentTool = currentState.previousTool,
                        isExtendedColorsVisible = false,
                        isEraserWidthSliderVisible = false,
                        isBrushWidthSliderVisible = false
                    )
                }
            }
            MainAction.ToggleExtendedColorsPanel -> {
                _state.update { it.copy(
                    isExtendedColorsVisible = !it.isExtendedColorsVisible
                ) }
            }
            MainAction.HideEraserWidthSlider -> {
                _state.update { it.copy(
                    isEraserWidthSliderVisible = false
                ) }
            }
            is MainAction.UpdateEraserWidth -> {
                _state.update { it.copy(
                    eraserWidth = action.width
                ) }
            }
            MainAction.HideBrushWidthSlider -> {
                _state.update { it.copy(
                    isBrushWidthSliderVisible = false
                ) }
            }
            is MainAction.UpdateBrushWidth -> {
                _state.update { it.copy(
                    brushWidth = action.width
                ) }
            }
            is MainAction.AddDrawingPath -> {
                _state.update { currentState ->
                    val currentFrame = currentState.frames[currentState.currentFrameIndex]
                    val newHistory = currentFrame.actionHistory.take(currentFrame.currentHistoryPosition + 1).toMutableList()
                    
                    val width = if (currentState.currentTool == Tool.BRUSH) currentState.brushWidth else 2f
                    
                    newHistory.add(DrawAction.DrawPath(
                        path = action.path,
                        color = currentState.selectedColor,
                        width = width
                    ))
                    
                    val updatedFrame = currentFrame.copy(
                        actionHistory = newHistory,
                        currentHistoryPosition = newHistory.size - 1
                    )
                    
                    val newFrames = currentState.frames.toMutableList()
                    newFrames[currentState.currentFrameIndex] = updatedFrame
                    
                    currentState.copy(frames = newFrames)
                }
            }
            is MainAction.AddEraserPath -> {
                _state.update { currentState ->
                    val currentFrame = currentState.frames[currentState.currentFrameIndex]
                    val newHistory = currentFrame.actionHistory.take(currentFrame.currentHistoryPosition + 1).toMutableList()
                    newHistory.add(DrawAction.ErasePath(
                        path = action.path,
                        width = currentState.eraserWidth
                    ))
                    
                    val updatedFrame = currentFrame.copy(
                        actionHistory = newHistory,
                        currentHistoryPosition = newHistory.size - 1
                    )
                    
                    val newFrames = currentState.frames.toMutableList()
                    newFrames[currentState.currentFrameIndex] = updatedFrame
                    
                    currentState.copy(frames = newFrames)
                }
            }
            MainAction.Undo -> {
                _state.update { currentState ->
                    val currentFrame = currentState.frames[currentState.currentFrameIndex]
                    if (currentFrame.currentHistoryPosition >= 0) {
                        val updatedFrame = currentFrame.copy(
                            currentHistoryPosition = currentFrame.currentHistoryPosition - 1
                        )
                        val newFrames = currentState.frames.toMutableList()
                        newFrames[currentState.currentFrameIndex] = updatedFrame
                        currentState.copy(frames = newFrames)
                    } else currentState
                }
            }
            MainAction.Redo -> {
                _state.update { currentState ->
                    val currentFrame = currentState.frames[currentState.currentFrameIndex]
                    if (currentFrame.currentHistoryPosition < currentFrame.actionHistory.size - 1) {
                        val updatedFrame = currentFrame.copy(
                            currentHistoryPosition = currentFrame.currentHistoryPosition + 1
                        )
                        val newFrames = currentState.frames.toMutableList()
                        newFrames[currentState.currentFrameIndex] = updatedFrame
                        currentState.copy(frames = newFrames)
                    } else currentState
                }
            }
            MainAction.AddNewFrame -> {
                _state.update { currentState ->
                    val newFrames = currentState.frames.toMutableList()
                    newFrames.add(Frame())
                    currentState.copy(
                        frames = newFrames,
                        currentFrameIndex = newFrames.size - 1
                    )
                }
            }
            MainAction.DeleteCurrentFrame -> {
                _state.update { currentState ->
                    if (currentState.frames.size <= 1) {
                        val newFrames = listOf(Frame())
                        currentState.copy(
                            frames = newFrames,
                            currentFrameIndex = 0
                        )
                    } else {
                        val newFrames = currentState.frames.toMutableList()
                        newFrames.removeAt(currentState.currentFrameIndex)
                        
                        currentState.copy(
                            frames = newFrames,
                            currentFrameIndex = currentState.currentFrameIndex - 1
                        )
                    }
                }
            }
            MainAction.StartPlayback -> {
                _state.update { currentState ->
                    currentState.copy(
                        isPlaybackActive = true,
                        playbackFrameIndex = 0
                    )
                }
                startPlayback()
            }
            MainAction.StopPlayback -> {
                _state.update { currentState ->
                    currentState.copy(
                        isPlaybackActive = false,
                        playbackFrameIndex = currentState.currentFrameIndex
                    )
                }
            }
            is MainAction.AddShape -> {
                _state.update { currentState ->
                    val currentFrame = currentState.frames[currentState.currentFrameIndex]
                    val newHistory = currentFrame.actionHistory.take(currentFrame.currentHistoryPosition + 1).toMutableList()
                    newHistory.add(DrawAction.DrawShape(action.shape, action.center, action.size, currentState.selectedColor))
                    
                    val updatedFrame = currentFrame.copy(
                        actionHistory = newHistory,
                        currentHistoryPosition = newHistory.size - 1
                    )
                    
                    val newFrames = currentState.frames.toMutableList()
                    newFrames[currentState.currentFrameIndex] = updatedFrame
                    
                    currentState.copy(frames = newFrames)
                }
            }
            MainAction.ShowGenerateFramesDialog -> {
                _state.update { it.copy(isGenerateFramesDialogVisible = true) }
            }
            MainAction.HideGenerateFramesDialog -> {
                _state.update { it.copy(isGenerateFramesDialogVisible = false) }
            }
            is MainAction.GenerateBouncingBallFrames -> {
                _state.update { currentState ->
                    val newFrames = currentState.frames.toMutableList()
                    
                    var x = currentState.canvasSize.width / 2
                    var y = currentState.canvasSize.height / 2
                    var dx = 30f
                    var dy = 20f
                    val radius = minOf(currentState.canvasSize.width, currentState.canvasSize.height) * 0.1f
                    
                    repeat(action.count) {
                        x += dx
                        y += dy
                        
                        if (x - radius < 0 || x + radius > currentState.canvasSize.width) {
                            dx = -dx
                            x = x.coerceIn(radius, currentState.canvasSize.width - radius)
                        }
                        if (y - radius < 0 || y + radius > currentState.canvasSize.height) {
                            dy = -dy
                            y = y.coerceIn(radius, currentState.canvasSize.height - radius)
                        }
                        
                        val frame = Frame(
                            actionHistory = listOf(
                                DrawAction.DrawShape(
                                    shape = Shape.Circle,
                                    center = Offset(x, y),
                                    size = radius * 2,
                                    color = currentState.selectedColor
                                )
                            ),
                            currentHistoryPosition = 0
                        )
                        newFrames.add(frame)
                    }
                    
                    currentState.copy(
                        frames = newFrames,
                        currentFrameIndex = newFrames.size - 1,
                        isGenerateFramesDialogVisible = false
                    )
                }
            }
            is MainAction.UpdateCanvasSize -> {
                _state.update { it.copy(canvasSize = action.size) }
            }
            MainAction.ShowDeleteAllDialog -> {
                _state.update { it.copy(isDeleteAllDialogVisible = true) }
            }
            MainAction.HideDeleteAllDialog -> {
                _state.update { it.copy(isDeleteAllDialogVisible = false) }
            }
            MainAction.DeleteAllFrames -> {
                _state.update { currentState ->
                    currentState.copy(
                        frames = listOf(Frame()),
                        currentFrameIndex = 0,
                        isDeleteAllDialogVisible = false
                    )
                }
            }
            MainAction.ShowDuplicateFrameDialog -> {
                _state.update { it.copy(isDuplicateFrameDialogVisible = true) }
            }
            MainAction.HideDuplicateFrameDialog -> {
                _state.update { it.copy(isDuplicateFrameDialogVisible = false) }
            }
            MainAction.DuplicateCurrentFrame -> {
                _state.update { currentState ->
                    val currentFrame = currentState.frames[currentState.currentFrameIndex]
                    val duplicatedFrame = currentFrame.copy()
                    val newFrames = currentState.frames.toMutableList()
                    newFrames.add(currentState.currentFrameIndex + 1, duplicatedFrame)
                    
                    currentState.copy(
                        frames = newFrames,
                        currentFrameIndex = currentState.currentFrameIndex + 1,
                        isDuplicateFrameDialogVisible = false
                    )
                }
            }
            MainAction.ShowPlaybackSpeedDialog -> {
                _state.update { it.copy(isPlaybackSpeedDialogVisible = true) }
            }
            MainAction.HidePlaybackSpeedDialog -> {
                _state.update { it.copy(isPlaybackSpeedDialogVisible = false) }
            }
            is MainAction.UpdatePlaybackSpeed -> {
                _state.update { it.copy(
                    playbackFps = action.fps,
                    isPlaybackSpeedDialogVisible = false
                ) }
                startPlayback()
            }
            is MainAction.SaveAsGif -> saveAsGif(action.context, action.density)
        }
    }

    private fun startPlayback() {
        viewModelScope.launch {
            while (state.value.isPlaybackActive) {
                delay(1000L / state.value.playbackFps)
                _state.update { currentState ->
                    val nextFrame = (currentState.playbackFrameIndex + 1) % currentState.frames.size
                    currentState.copy(playbackFrameIndex = nextFrame)
                }
            }
        }
    }

    private fun saveAsGif(context: Context, density: Density) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                _state.update { it.copy(isSavingGif = true, gifSavingResult = null) }
                
                val cacheDir = context.cacheDir
                val framesDir = File(cacheDir, "frames").apply { mkdirs() }
                
                val backgroundBitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.bg_paper
                    )
                }
                
                val scaledWidth = (state.value.canvasSize.width / 2).toInt()
                val scaledHeight = (state.value.canvasSize.height / 2).toInt()
                
                val framePaths = coroutineScope {
                    state.value.frames.mapIndexed { index, frame ->
                        async {
                            val drawingBitmap = Bitmap.createBitmap(
                                state.value.canvasSize.width.toInt(),
                                state.value.canvasSize.height.toInt(),
                                Bitmap.Config.ARGB_8888
                            )
                            val finalBitmap = Bitmap.createBitmap(
                                state.value.canvasSize.width.toInt(),
                                state.value.canvasSize.height.toInt(),
                                Bitmap.Config.ARGB_8888
                            )
                            
                            val drawingCanvas = Canvas(drawingBitmap)
                            val finalCanvas = Canvas(finalBitmap)
                            
                            val scaledBackground = Bitmap.createScaledBitmap(
                                backgroundBitmap,
                                finalBitmap.width,
                                finalBitmap.height,
                                true
                            )
                            finalCanvas.drawBitmap(scaledBackground, 0f, 0f, null)
                            scaledBackground.recycle()
                            
                            frame.actionHistory.take(frame.currentHistoryPosition + 1).forEach { action ->
                                when (action) {
                                    is DrawAction.DrawPath -> {
                                        val paint = Paint().apply {
                                            color = action.color
                                            strokeWidth = with(density) { action.width.dp.toPx() }
                                            style = Paint.Style.STROKE
                                            strokeCap = Paint.Cap.ROUND
                                            strokeJoin = Paint.Join.ROUND
                                        }
                                        drawSmoothLine(drawingCanvas, action.path, paint)
                                    }
                                    is DrawAction.ErasePath -> {
                                        val paint = Paint().apply {
                                            strokeWidth = with(density) { action.width.dp.toPx() }
                                            style = Paint.Style.STROKE
                                            strokeCap = Paint.Cap.ROUND
                                            strokeJoin = Paint.Join.ROUND
                                            color = android.graphics.Color.TRANSPARENT
                                            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                                        }
                                        drawSmoothLine(drawingCanvas, action.path, paint)
                                    }
                                    is DrawAction.DrawShape -> {
                                        val paint = Paint().apply {
                                            color = action.color
                                            strokeWidth = with(density) { 2.dp.toPx() }
                                            style = Paint.Style.STROKE
                                            strokeCap = Paint.Cap.ROUND
                                            strokeJoin = Paint.Join.ROUND
                                        }
                                        when (action.shape) {
                                            Shape.Square -> {
                                                val path = Path().apply {
                                                    moveTo(action.center.x - action.size/2, action.center.y - action.size/2)
                                                    lineTo(action.center.x + action.size/2, action.center.y - action.size/2)
                                                    lineTo(action.center.x + action.size/2, action.center.y + action.size/2)
                                                    lineTo(action.center.x - action.size/2, action.center.y + action.size/2)
                                                    close()
                                                }
                                                drawingCanvas.drawPath(path, paint)
                                            }
                                            Shape.Circle -> {
                                                drawingCanvas.drawCircle(
                                                    action.center.x,
                                                    action.center.y,
                                                    action.size / 2,
                                                    paint
                                                )
                                            }
                                            Shape.Triangle -> {
                                                val path = Path().apply {
                                                    moveTo(action.center.x, action.center.y - action.size/2)
                                                    lineTo(action.center.x + action.size/2, action.center.y + action.size/2)
                                                    lineTo(action.center.x - action.size/2, action.center.y + action.size/2)
                                                    close()
                                                }
                                                drawingCanvas.drawPath(path, paint)
                                            }
                                            Shape.Arrow -> {
                                                val path = Path().apply {
                                                    moveTo(action.center.x, action.center.y - action.size/2)
                                                    lineTo(action.center.x - action.size/3, action.center.y - action.size/6)
                                                    moveTo(action.center.x, action.center.y - action.size/2)
                                                    lineTo(action.center.x + action.size/3, action.center.y - action.size/6)
                                                    moveTo(action.center.x, action.center.y - action.size/2)
                                                    lineTo(action.center.x, action.center.y + action.size/2)
                                                }
                                                drawingCanvas.drawPath(path, paint)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            finalCanvas.drawBitmap(drawingBitmap, 0f, 0f, null)
                            
                            drawingBitmap.recycle()
                            
                            val scaledBitmap = Bitmap.createScaledBitmap(
                                finalBitmap,
                                scaledWidth,
                                scaledHeight,
                                true
                            )
                            finalBitmap.recycle()

                            withContext(Dispatchers.IO) {
                                val file = File(framesDir, "frame_$index.png")
                                FileOutputStream(file).use { out ->
                                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 80, out)
                                }
                                scaledBitmap.recycle()
                                file.absolutePath
                            }
                        }
                    }.awaitAll()
                }
                
                backgroundBitmap.recycle()
                
                withContext(Dispatchers.IO) {
                    val gifFile = File(context.getExternalFilesDir(null), "animation.gif")
                    val encoder = AnimatedGifEncoder()
                    encoder.start(gifFile.absolutePath)
                    encoder.setDelay(1000 / state.value.playbackFps)
                    encoder.setRepeat(0)
                    
                    framePaths.forEach { path ->
                        val bitmap = BitmapFactory.decodeFile(path)
                        encoder.addFrame(bitmap)
                        bitmap.recycle()
                    }
                    encoder.finish()
                    
                    framesDir.deleteRecursively()
                    
                    val uri = getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        gifFile
                    )
                    
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(
                            isSavingGif = false,
                            gifSavingResult = GifSavingResult.Success(uri)
                        ) }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(
                        isSavingGif = false,
                        gifSavingResult = GifSavingResult.Error(e.localizedMessage ?: "Unknown error")
                    ) }
                }
            }
        }
    }

    private fun drawSmoothLine(canvas: Canvas, points: List<Offset>, paint: Paint) {
        if (points.size > 1) {
            val path = Path()
            if (points.size > 1) {
                path.moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = if (i > 0) points[i - 1] else points[i]
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val p3 = if (i < points.size - 2) points[i + 2] else p2

                    val controlPoint1X = p1.x + (p2.x - p0.x) / 6
                    val controlPoint1Y = p1.y + (p2.y - p0.y) / 6
                    val controlPoint2X = p2.x - (p3.x - p1.x) / 6
                    val controlPoint2Y = p2.y - (p3.y - p1.y) / 6

                    path.cubicTo(
                        controlPoint1X, controlPoint1Y,
                        controlPoint2X, controlPoint2Y,
                        p2.x, p2.y
                    )
                }
            }
            canvas.drawPath(path, paint)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancelChildren()
    }
}

data class MainState(
    val currentTool: Tool = Tool.PENCIL,
    val previousTool: Tool = Tool.PENCIL,
    val isExtendedColorsVisible: Boolean = false,
    val frames: List<Frame> = listOf(Frame()),
    val currentFrameIndex: Int = 0,
    val isPlaybackActive: Boolean = false,
    val playbackFrameIndex: Int = 0,
    val selectedColor: Int = Colors.Black.toArgb(),
    val isEraserWidthSliderVisible: Boolean = false,
    val eraserWidth: Float = 20f,
    val isBrushWidthSliderVisible: Boolean = false,
    val brushWidth: Float = 20f,
    val isGenerateFramesDialogVisible: Boolean = false,
    val canvasSize: Size = Size.Zero,
    val isDeleteAllDialogVisible: Boolean = false,
    val isDuplicateFrameDialogVisible: Boolean = false,
    val isPlaybackSpeedDialogVisible: Boolean = false,
    val playbackFps: Int = 5,
    val isSavingGif: Boolean = false,
    val gifSavingResult: GifSavingResult? = null
)

data class Frame(
    val actionHistory: List<DrawAction> = emptyList(),
    val currentHistoryPosition: Int = -1
)

sealed interface MainAction {
    data class SelectTool(val tool: Tool) : MainAction
    data object ToggleExtendedColorsPanel : MainAction
    data object HideEraserWidthSlider : MainAction
    data class UpdateEraserWidth(val width: Float) : MainAction
    data object HideBrushWidthSlider : MainAction
    data class UpdateBrushWidth(val width: Float) : MainAction
    data class AddDrawingPath(val path: List<Offset>) : MainAction
    data class AddEraserPath(val path: List<Offset>) : MainAction
    data class AddShape(val shape: Shape, val center: Offset, val size: Float) : MainAction
    data object Undo : MainAction
    data object Redo : MainAction
    data object AddNewFrame : MainAction
    data object DeleteCurrentFrame : MainAction
    data object StartPlayback : MainAction
    data object StopPlayback : MainAction
    data class SelectColor(val color: Int) : MainAction
    data object ShowGenerateFramesDialog : MainAction
    data object HideGenerateFramesDialog : MainAction
    data class GenerateBouncingBallFrames(val count: Int) : MainAction
    data class UpdateCanvasSize(val size: Size) : MainAction
    data object ShowDeleteAllDialog : MainAction
    data object HideDeleteAllDialog : MainAction
    data object DeleteAllFrames : MainAction
    data object ShowDuplicateFrameDialog : MainAction
    data object HideDuplicateFrameDialog : MainAction
    data object DuplicateCurrentFrame : MainAction
    data object ShowPlaybackSpeedDialog : MainAction
    data object HidePlaybackSpeedDialog : MainAction
    data class UpdatePlaybackSpeed(val fps: Int) : MainAction
    data class SaveAsGif(val context: Context, val density: Density) : MainAction
}

sealed interface DrawAction {
    data class DrawPath(val path: List<Offset>, val color: Int, val width: Float) : DrawAction
    data class ErasePath(val path: List<Offset>, val width: Float) : DrawAction
    data class DrawShape(val shape: Shape, val center: Offset, val size: Float, val color: Int) : DrawAction
}

sealed interface Shape {
    data object Square : Shape
    data object Circle : Shape
    data object Triangle : Shape
    data object Arrow : Shape
}

enum class Tool {
    PENCIL, BRUSH, ERASER, SHAPES, COLORS
}

sealed interface GifSavingResult {
    data class Success(val uri: Uri) : GifSavingResult
    data class Error(val message: String) : GifSavingResult
}
