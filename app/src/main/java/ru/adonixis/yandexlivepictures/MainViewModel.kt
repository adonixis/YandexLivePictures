package ru.adonixis.yandexlivepictures

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
                _state.update { it.copy(isPencilEnabled = !it.isPencilEnabled) }
            }
        }
    }
}

data class MainState(
    val isPencilEnabled: Boolean = false
)

sealed interface MainAction {
    data object TogglePencilTool : MainAction
} 
