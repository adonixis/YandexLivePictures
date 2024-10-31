package ru.adonixis.yandexlivepictures

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    fun onAction(action: MainAction) {
        when (action) {
            MainAction.TogglePencilTool -> {
                _state.update { it.copy(
                    isPencilEnabled = !it.isPencilEnabled,
                    isEraserEnabled = false
                ) }
            }
            MainAction.ToggleEraserTool -> {
                _state.update { it.copy(
                    isEraserEnabled = !it.isEraserEnabled,
                    isPencilEnabled = false
                ) }
            }
            is MainAction.AddDrawingPath -> {
                _state.update { currentState ->
                    val newHistory = currentState.actionHistory.take(currentState.currentHistoryPosition + 1).toMutableList()
                    newHistory.add(DrawAction.DrawPath(action.path))
                    
                    currentState.copy(
                        actionHistory = newHistory,
                        currentHistoryPosition = newHistory.size - 1
                    )
                }
            }
            is MainAction.AddEraserPath -> {
                _state.update { currentState ->
                    val newHistory = currentState.actionHistory.take(currentState.currentHistoryPosition + 1).toMutableList()
                    newHistory.add(DrawAction.ErasePath(action.path))
                    
                    currentState.copy(
                        actionHistory = newHistory,
                        currentHistoryPosition = newHistory.size - 1
                    )
                }
            }
            MainAction.Undo -> {
                _state.update { currentState ->
                    if (currentState.currentHistoryPosition >= 0) {
                        currentState.copy(
                            currentHistoryPosition = currentState.currentHistoryPosition - 1
                        )
                    } else currentState
                }
            }
            MainAction.Redo -> {
                _state.update { currentState ->
                    if (currentState.currentHistoryPosition < currentState.actionHistory.size - 1) {
                        currentState.copy(
                            currentHistoryPosition = currentState.currentHistoryPosition + 1
                        )
                    } else currentState
                }
            }
        }
    }
}

data class MainState(
    val isPencilEnabled: Boolean = false,
    val isEraserEnabled: Boolean = false,
    val actionHistory: List<DrawAction> = emptyList(),
    val currentHistoryPosition: Int = -1
)

sealed interface MainAction {
    data object TogglePencilTool : MainAction
    data object ToggleEraserTool : MainAction
    data class AddDrawingPath(val path: List<Offset>) : MainAction
    data class AddEraserPath(val path: List<Offset>) : MainAction
    data object Undo : MainAction
    data object Redo : MainAction
}

sealed interface DrawAction {
    data class DrawPath(val path: List<Offset>) : DrawAction
    data class ErasePath(val path: List<Offset>) : DrawAction
} 
