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
fun GenerateFramesDialog(
    frameCount: String,
    onFrameCountChange: (String) -> Unit,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onAction(MainAction.HideGenerateFramesDialog) },
        title = { Text(stringResource(R.string.frames_generation)) },
        text = {
            TextField(
                value = frameCount,
                onValueChange = { text ->
                    if (text.isEmpty()) {
                        onFrameCountChange("")
                    } else if ((text.toIntOrNull() ?: 0) > 0) {
                        onFrameCountChange(text)
                    }
                },
                label = { Text(stringResource(R.string.frames_count)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(
                enabled = (frameCount.toIntOrNull() ?: 0) > 0,
                onClick = {
                    frameCount.toIntOrNull()?.let {
                        if (it > 0)
                            onAction(MainAction.GenerateBouncingBallFrames(it))
                    }
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(MainAction.HideGenerateFramesDialog) }
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
} 
