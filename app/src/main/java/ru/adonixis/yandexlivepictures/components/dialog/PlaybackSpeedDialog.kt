package ru.adonixis.yandexlivepictures.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import ru.adonixis.yandexlivepictures.MainAction
import ru.adonixis.yandexlivepictures.R

@Composable
fun PlaybackSpeedDialog(
    maxFps: Int,
    playbackSpeed: String,
    onPlaybackSpeedChange: (String) -> Unit,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onAction(MainAction.HidePlaybackSpeedDialog) },
        title = { Text(stringResource(R.string.playback_speed)) },
        text = {
            TextField(
                value = playbackSpeed,
                onValueChange = { text ->
                    if (text.isEmpty()) {
                        onPlaybackSpeedChange("")
                    } else if (((text.toIntOrNull() ?: 0) > 0) && ((text.toIntOrNull() ?: 0) <= maxFps)) {
                        onPlaybackSpeedChange(text)
                    }
                },
                label = { Text(stringResource(R.string.frames_per_seconds, maxFps)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(
                enabled = ((playbackSpeed.toIntOrNull() ?: 0) > 0) && ((playbackSpeed.toIntOrNull() ?: 0) <= maxFps),
                onClick = {
                    playbackSpeed.toIntOrNull()?.let {
                        if (it in 1..maxFps)
                            onAction(MainAction.UpdatePlaybackSpeed(it))
                    }
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(MainAction.HidePlaybackSpeedDialog) }
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
} 
