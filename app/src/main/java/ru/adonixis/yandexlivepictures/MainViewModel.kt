package ru.adonixis.yandexlivepictures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val playbackFps: Int = 5
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
