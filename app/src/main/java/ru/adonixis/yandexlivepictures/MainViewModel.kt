package ru.adonixis.yandexlivepictures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import ru.adonixis.yandexlivepictures.theme.Black

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    fun onAction(action: MainAction) {
        when (action) {
            MainAction.TogglePencilTool -> {
                _state.update { it.copy(
                    isPencilEnabled = !it.isPencilEnabled,
                    isEraserEnabled = false,
                    isShapesVisible = false,
                    isColorsVisible = false
                ) }
            }
            MainAction.ToggleEraserTool -> {
                _state.update { it.copy(
                    isEraserEnabled = !it.isEraserEnabled,
                    isPencilEnabled = false,
                    isShapesVisible = false,
                    isColorsVisible = false
                ) }
            }
            MainAction.ToggleShapesPanel -> {
                _state.update { it.copy(
                    isShapesVisible = !it.isShapesVisible,
                    isPencilEnabled = false,
                    isEraserEnabled = false,
                    isColorsVisible = false
                ) }
            }
            MainAction.ToggleColorsPanel -> {
                _state.update { it.copy(
                    isColorsVisible = !it.isColorsVisible,
                    isExtendedColorsVisible = false,
                    isPencilEnabled = false,
                    isEraserEnabled = false,
                    isShapesVisible = false
                ) }
            }
            MainAction.ToggleExtendedColorsPanel -> {
                _state.update { it.copy(
                    isExtendedColorsVisible = !it.isExtendedColorsVisible,
                    isColorsVisible = true
                ) }
            }
            is MainAction.AddDrawingPath -> {
                _state.update { currentState ->
                    val currentFrame = currentState.frames[currentState.currentFrameIndex]
                    val newHistory = currentFrame.actionHistory.take(currentFrame.currentHistoryPosition + 1).toMutableList()
                    newHistory.add(DrawAction.DrawPath(action.path, currentState.selectedColor))
                    
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
                    newHistory.add(DrawAction.ErasePath(action.path))
                    
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
                    if (currentState.frames.size <= 1) return@update currentState
                    
                    val newFrames = currentState.frames.toMutableList()
                    newFrames.removeAt(currentState.currentFrameIndex)
                    
                    currentState.copy(
                        frames = newFrames,
                        currentFrameIndex = currentState.currentFrameIndex - 1
                    )
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
            is MainAction.SelectColor -> {
                _state.update { it.copy(selectedColor = action.color) }
            }
        }
    }

    private fun startPlayback() {
        viewModelScope.launch {
            while (state.value.isPlaybackActive) {
                delay(200) // 5 кадров в секунду
                _state.update { currentState ->
                    val nextFrame = (currentState.playbackFrameIndex + 1) % currentState.frames.size
                    currentState.copy(playbackFrameIndex = nextFrame)
                }
            }
        }
    }
}

data class MainState(
    val isPencilEnabled: Boolean = false,
    val isEraserEnabled: Boolean = false,
    val isShapesVisible: Boolean = false,
    val isColorsVisible: Boolean = false,
    val isExtendedColorsVisible: Boolean = false,
    val frames: List<Frame> = listOf(Frame()),
    val currentFrameIndex: Int = 0,
    val isPlaybackActive: Boolean = false,
    val playbackFrameIndex: Int = 0,
    val selectedColor: Int = Black.toArgb()
)

data class Frame(
    val actionHistory: List<DrawAction> = emptyList(),
    val currentHistoryPosition: Int = -1
)

sealed interface MainAction {
    data object TogglePencilTool : MainAction
    data object ToggleEraserTool : MainAction
    data object ToggleShapesPanel : MainAction
    data object ToggleColorsPanel : MainAction
    data object ToggleExtendedColorsPanel : MainAction
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
}

sealed interface DrawAction {
    data class DrawPath(val path: List<Offset>, val color: Int) : DrawAction
    data class ErasePath(val path: List<Offset>) : DrawAction
    data class DrawShape(val shape: Shape, val center: Offset, val size: Float, val color: Int) : DrawAction
}

sealed interface Shape {
    data object Square : Shape
    data object Circle : Shape
    data object Triangle : Shape
    data object Arrow : Shape
}
