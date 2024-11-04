package ru.adonixis.yandexlivepictures.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.adonixis.yandexlivepictures.MainAction
import ru.adonixis.yandexlivepictures.R

@Composable
fun DuplicateFrameDialog(
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onAction(MainAction.HideDuplicateFrameDialog) },
        title = { Text(stringResource(R.string.duplicate_frame)) },
        text = { Text(stringResource(R.string.current_frame_will_be_deleted)) },
        confirmButton = {
            TextButton(
                onClick = { onAction(MainAction.DuplicateCurrentFrame) }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(MainAction.HideDuplicateFrameDialog) }
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
} 
