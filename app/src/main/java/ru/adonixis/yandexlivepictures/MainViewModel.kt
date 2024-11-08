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
import androidx.core.content.FileProvider.getUriForFile
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

private object ViewModelConstants {
    const val ANIMATION_QUALITY = 80
    const val ANIMATION_REPEAT = 0
    const val SHAPE_MIN_SCALE = 0.1f
    const val SHAPE_MAX_SCALE = 2f
    const val BOUNCING_BALL_DX = 30f
    const val BOUNCING_BALL_DY = 20f
    const val BALL_SIZE_RATIO = 0.1f
    const val DEFAULT_FPS = 5
}

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
                    
                    val width = if (currentState.currentTool == Tool.BRUSH) currentState.brushWidth else currentState.pencilWidth
                    
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
                updateThumbnailsAt(listOf(state.value.currentFrameIndex))
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
                updateThumbnailsAt(listOf(state.value.currentFrameIndex))
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
                updateThumbnailsAt(listOf(state.value.currentFrameIndex))
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
                updateThumbnailsAt(listOf(state.value.currentFrameIndex))
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
                updateThumbnailsAt(listOf(state.value.frames.size - 1))
            }
            MainAction.DeleteCurrentFrame -> {
                _state.update { currentState ->
                    if (currentState.frames.size <= 1) {
                        updateThumbnailsAt(listOf(0))
                        val newFrames = listOf(Frame())
                        currentState.copy(
                            frames = newFrames,
                            currentFrameIndex = 0
                        )
                    } else {
                        val newFrames = currentState.frames.toMutableList()
                        if (currentState.currentFrameIndex < currentState.frames.size) {
                            newFrames.removeAt(currentState.currentFrameIndex)
                        }
                        val newIndex = when {
                            currentState.currentFrameIndex == 0 -> 0
                            currentState.currentFrameIndex >= newFrames.size -> newFrames.size - 1
                            else -> currentState.currentFrameIndex
                        }
                        currentState.copy(
                            frames = newFrames,
                            currentFrameIndex = newIndex
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
                    
                    currentState.copy(
                        frames = newFrames,
                        isShapeMovable = true
                    )
                }
                updateThumbnailsAt(listOf(state.value.currentFrameIndex))
            }
            MainAction.ShowGenerateFramesDialog -> {
                _state.update { it.copy(isGenerateFramesDialogVisible = true) }
            }
            MainAction.HideGenerateFramesDialog -> {
                _state.update { it.copy(isGenerateFramesDialogVisible = false) }
            }
            is MainAction.GenerateBouncingBallFrames -> {
                val startIndex = state.value.frames.size
                _state.update { currentState ->
                    val newFrames = currentState.frames.toMutableList()
                    
                    var x = currentState.canvasSize.width / 2
                    var y = currentState.canvasSize.height / 2
                    var dx = ViewModelConstants.BOUNCING_BALL_DX
                    var dy = ViewModelConstants.BOUNCING_BALL_DY
                    val radius = minOf(currentState.canvasSize.width, currentState.canvasSize.height) * ViewModelConstants.BALL_SIZE_RATIO
                    
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
                val newIndices = (startIndex until state.value.frames.size).toList()
                updateThumbnailsAt(newIndices)
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
                updateThumbnailsAt(listOf(0))
            }
            MainAction.ShowDuplicateFrameDialog -> {
                _state.update { it.copy(isDuplicateFrameDialogVisible = true) }
            }
            MainAction.HideDuplicateFrameDialog -> {
                _state.update { it.copy(isDuplicateFrameDialogVisible = false) }
            }
            MainAction.DuplicateCurrentFrame -> {
                val originalIndex = state.value.currentFrameIndex
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
                updateThumbnailsAt(listOf(originalIndex + 1))
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
            is MainAction.SaveAsGif -> saveAsGif(action.context)
            is MainAction.SelectFrame -> {
                _state.update { it.copy(
                    currentFrameIndex = action.index
                ) }
            }
            MainAction.ShowFrameList -> {
                _state.update { it.copy(
                    isFrameListVisible = true
                ) }
            }
            MainAction.HideFrameList -> {
                _state.update { it.copy(
                    isFrameListVisible = false
                ) }
            }
            is MainAction.UpdateShapePosition -> {
                if (!state.value.isShapeMovable) return
                
                _state.update { currentState ->
                    val currentFrame = currentState.frames[currentState.currentFrameIndex]
                    val lastAction = currentFrame.actionHistory.getOrNull(currentFrame.currentHistoryPosition)
                    
                    if (lastAction is DrawAction.DrawShape) {
                        val newHistory = currentFrame.actionHistory.toMutableList()
                        newHistory[currentFrame.currentHistoryPosition] = lastAction.copy(
                            center = action.newCenter
                        )
                        
                        val updatedFrame = currentFrame.copy(
                            actionHistory = newHistory
                        )
                        
                        val newFrames = currentState.frames.toMutableList()
                        newFrames[currentState.currentFrameIndex] = updatedFrame
                        
                        currentState.copy(frames = newFrames)
                    } else currentState
                }
                updateThumbnailsAt(listOf(state.value.currentFrameIndex))
            }
            is MainAction.UpdateShapeScale -> {
                if (!state.value.isShapeMovable) return
                
                _state.update { currentState ->
                    val currentFrame = currentState.frames[currentState.currentFrameIndex]
                    val lastAction = currentFrame.actionHistory.getOrNull(currentFrame.currentHistoryPosition)
                    
                    if (lastAction is DrawAction.DrawShape) {
                        val newHistory = currentFrame.actionHistory.toMutableList()
                        newHistory[currentFrame.currentHistoryPosition] = lastAction.copy(
                            scale = action.scale.coerceIn(ViewModelConstants.SHAPE_MIN_SCALE, ViewModelConstants.SHAPE_MAX_SCALE)
                        )
                        
                        val updatedFrame = currentFrame.copy(
                            actionHistory = newHistory
                        )
                        
                        val newFrames = currentState.frames.toMutableList()
                        newFrames[currentState.currentFrameIndex] = updatedFrame
                        
                        currentState.copy(frames = newFrames)
                    } else currentState
                }
                updateThumbnailsAt(listOf(state.value.currentFrameIndex))
            }
            is MainAction.UpdateShapeRotation -> {
                if (!state.value.isShapeMovable) return
                
                _state.update { currentState ->
                    val currentFrame = currentState.frames[currentState.currentFrameIndex]
                    val lastAction = currentFrame.actionHistory.getOrNull(currentFrame.currentHistoryPosition)
                    
                    if (lastAction is DrawAction.DrawShape) {
                        val newHistory = currentFrame.actionHistory.toMutableList()
                        newHistory[currentFrame.currentHistoryPosition] = lastAction.copy(
                            rotation = action.rotation
                        )
                        
                        val updatedFrame = currentFrame.copy(
                            actionHistory = newHistory
                        )
                        
                        val newFrames = currentState.frames.toMutableList()
                        newFrames[currentState.currentFrameIndex] = updatedFrame
                        
                        currentState.copy(frames = newFrames)
                    } else currentState
                }
                updateThumbnailsAt(listOf(state.value.currentFrameIndex))
            }
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

    private fun saveAsGif(context: Context) {
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
                                            strokeWidth = action.width
                                            style = Paint.Style.STROKE
                                            strokeCap = Paint.Cap.ROUND
                                            strokeJoin = Paint.Join.ROUND
                                        }
                                        drawSmoothLine(drawingCanvas, action.path, paint)
                                    }
                                    is DrawAction.ErasePath -> {
                                        val paint = Paint().apply {
                                            strokeWidth = action.width
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
                                            strokeWidth = state.value.pencilWidth
                                            style = Paint.Style.STROKE
                                            strokeCap = Paint.Cap.ROUND
                                            strokeJoin = Paint.Join.ROUND
                                        }
                                        val scaledSize = action.size * action.scale
                                        
                                        drawingCanvas.save()
                                        drawingCanvas.rotate(action.rotation, action.center.x, action.center.y)
                                        
                                        when (action.shape) {
                                            Shape.Square -> {
                                                val path = Path().apply {
                                                    moveTo(action.center.x - scaledSize/2, action.center.y - scaledSize/2)
                                                    lineTo(action.center.x + scaledSize/2, action.center.y - scaledSize/2)
                                                    lineTo(action.center.x + scaledSize/2, action.center.y + scaledSize/2)
                                                    lineTo(action.center.x - scaledSize/2, action.center.y + scaledSize/2)
                                                    close()
                                                }
                                                drawingCanvas.drawPath(path, paint)
                                            }
                                            Shape.Circle -> {
                                                drawingCanvas.drawCircle(
                                                    action.center.x,
                                                    action.center.y,
                                                    scaledSize / 2,
                                                    paint
                                                )
                                            }
                                            Shape.Triangle -> {
                                                val path = Path().apply {
                                                    moveTo(action.center.x, action.center.y - scaledSize/2)
                                                    lineTo(action.center.x + scaledSize/2, action.center.y + scaledSize/2)
                                                    lineTo(action.center.x - scaledSize/2, action.center.y + scaledSize/2)
                                                    close()
                                                }
                                                drawingCanvas.drawPath(path, paint)
                                            }
                                            Shape.Arrow -> {
                                                val path = Path().apply {
                                                    moveTo(action.center.x, action.center.y - scaledSize/2)
                                                    lineTo(action.center.x - scaledSize/3, action.center.y - scaledSize/6)
                                                    moveTo(action.center.x, action.center.y - scaledSize/2)
                                                    lineTo(action.center.x + scaledSize/3, action.center.y - scaledSize/6)
                                                    moveTo(action.center.x, action.center.y - scaledSize/2)
                                                    lineTo(action.center.x, action.center.y + scaledSize/2)
                                                }
                                                drawingCanvas.drawPath(path, paint)
                                            }
                                        }
                                        
                                        drawingCanvas.restore()
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
                                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, ViewModelConstants.ANIMATION_QUALITY, out)
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
                    encoder.setRepeat(ViewModelConstants.ANIMATION_REPEAT)
                    
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

    fun initializeSizes(
        pencilWidth: Float,
        brushWidth: Float,
        eraserWidth: Float,
        thumbnailHeight: Float
    ) {
        _state.update { it.copy(
            pencilWidth = pencilWidth,
            brushWidth = brushWidth,
            eraserWidth = eraserWidth,
            thumbnailHeight = thumbnailHeight
        ) }
        updateThumbnailsAt(listOf(0))
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancelChildren()
    }

    private fun generateThumbnail(frame: Frame, height: Int): ImageBitmap? {
        if (state.value.canvasSize.width <= 0 || state.value.canvasSize.height <= 0) {
            return null
        }
        
        val fullBitmap = Bitmap.createBitmap(
            state.value.canvasSize.width.toInt(),
            state.value.canvasSize.height.toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(fullBitmap)
        
        canvas.drawColor(android.graphics.Color.WHITE)
        
        frame.actionHistory.take(frame.currentHistoryPosition + 1).forEach { action ->
            when (action) {
                is DrawAction.DrawPath -> {
                    val paint = Paint().apply {
                        color = action.color
                        strokeWidth = action.width
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    drawSmoothLine(canvas, action.path, paint)
                }
                is DrawAction.ErasePath -> {
                    val paint = Paint().apply {
                        strokeWidth = action.width
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        color = android.graphics.Color.WHITE
                    }
                    drawSmoothLine(canvas, action.path, paint)
                }
                is DrawAction.DrawShape -> {
                    val paint = Paint().apply {
                        color = action.color
                        strokeWidth = state.value.pencilWidth
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    val scaledSize = action.size * action.scale
                    
                    canvas.save()
                    canvas.rotate(action.rotation, action.center.x, action.center.y)
                    
                    when (action.shape) {
                        Shape.Square -> {
                            val path = Path().apply {
                                moveTo(action.center.x - scaledSize/2, action.center.y - scaledSize/2)
                                lineTo(action.center.x + scaledSize/2, action.center.y - scaledSize/2)
                                lineTo(action.center.x + scaledSize/2, action.center.y + scaledSize/2)
                                lineTo(action.center.x - scaledSize/2, action.center.y + scaledSize/2)
                                close()
                            }
                            canvas.drawPath(path, paint)
                        }
                        Shape.Circle -> {
                            canvas.drawCircle(
                                action.center.x,
                                action.center.y,
                                scaledSize/2,
                                paint
                            )
                        }
                        Shape.Triangle -> {
                            val path = Path().apply {
                                moveTo(action.center.x, action.center.y - scaledSize/2)
                                lineTo(action.center.x + scaledSize/2, action.center.y + scaledSize/2)
                                lineTo(action.center.x - scaledSize/2, action.center.y + scaledSize/2)
                                close()
                            }
                            canvas.drawPath(path, paint)
                        }
                        Shape.Arrow -> {
                            val path = Path().apply {
                                moveTo(action.center.x, action.center.y - scaledSize/2)
                                lineTo(action.center.x - scaledSize/3, action.center.y - scaledSize/6)
                                moveTo(action.center.x, action.center.y - scaledSize/2)
                                lineTo(action.center.x + scaledSize/3, action.center.y - scaledSize/6)
                                moveTo(action.center.x, action.center.y - scaledSize/2)
                                lineTo(action.center.x, action.center.y + scaledSize/2)
                            }
                            canvas.drawPath(path, paint)
                        }
                    }
                    
                    canvas.restore()
                }
            }
        }
        
        val aspectRatio = state.value.canvasSize.width / state.value.canvasSize.height
        val width = (height * aspectRatio).toInt()
        
        if (width <= 0 || height <= 0) {
            fullBitmap.recycle()
            return null
        }
        
        val scaledBitmap = Bitmap.createScaledBitmap(fullBitmap, width, height, true)
        fullBitmap.recycle()
        
        return scaledBitmap.asImageBitmap()
    }

    private fun updateSingleThumbnail(frame: Frame): Frame {
        return frame.copy(
            thumbnail = generateThumbnail(frame, state.value.thumbnailHeight.toInt())
        )
    }

    private fun updateThumbnailsAt(indices: List<Int>) {
        viewModelScope.launch(Dispatchers.Default) {
            val newFrames = state.value.frames.toMutableList()
            indices.forEach { index ->
                if (index in newFrames.indices) {
                    newFrames[index] = updateSingleThumbnail(newFrames[index])
                }
            }
            _state.update { it.copy(frames = newFrames) }
        }
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
    val isBrushWidthSliderVisible: Boolean = false,
    val isGenerateFramesDialogVisible: Boolean = false,
    val canvasSize: Size = Size.Zero,
    val isDeleteAllDialogVisible: Boolean = false,
    val isDuplicateFrameDialogVisible: Boolean = false,
    val isPlaybackSpeedDialogVisible: Boolean = false,
    val playbackFps: Int = ViewModelConstants.DEFAULT_FPS,
    val isSavingGif: Boolean = false,
    val gifSavingResult: GifSavingResult? = null,
    val isFrameListVisible: Boolean = false,
    val pencilWidth: Float = 0f,
    val eraserWidth: Float = 0f,
    val brushWidth: Float = 0f,
    val thumbnailHeight: Float = 0f,
    val isShapeMovable: Boolean = false,
)

data class Frame(
    val actionHistory: List<DrawAction> = emptyList(),
    val currentHistoryPosition: Int = -1,
    val thumbnail: ImageBitmap? = null
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
    data class SaveAsGif(val context: Context) : MainAction
    data class SelectFrame(val index: Int) : MainAction
    data object ShowFrameList : MainAction
    data object HideFrameList : MainAction
    data class UpdateShapePosition(val newCenter: Offset) : MainAction
    data class UpdateShapeScale(val scale: Float) : MainAction
    data class UpdateShapeRotation(val rotation: Float) : MainAction
}

sealed interface DrawAction {
    data class DrawPath(val path: List<Offset>, val color: Int, val width: Float) : DrawAction
    data class ErasePath(val path: List<Offset>, val width: Float) : DrawAction
    data class DrawShape(
        val shape: Shape, 
        val center: Offset, 
        val size: Float, 
        val color: Int,
        val scale: Float = 1f,
        val rotation: Float = 0f
    ) : DrawAction
}

sealed interface Shape {
    data object Square : Shape
    data object Circle : Shape
    data object Triangle : Shape
    data object Arrow : Shape
}

enum class Tool {
    PENCIL, BRUSH, ERASER, SHAPES, COLORS, ZOOM
}

sealed interface GifSavingResult {
    data class Success(val uri: Uri) : GifSavingResult
    data class Error(val message: String) : GifSavingResult
}
