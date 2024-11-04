package ru.adonixis.yandexlivepictures.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.adonixis.yandexlivepictures.MainAction
import ru.adonixis.yandexlivepictures.MainState
import ru.adonixis.yandexlivepictures.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopActionBar(
    state: MainState,
    onAction: (MainAction) -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .height(36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.alpha(if (state.isPlaybackActive) 0f else 1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                modifier = Modifier.size(24.dp),
                onClick = { onAction(MainAction.Undo) },
                enabled = !state.isPlaybackActive && state.frames[state.currentFrameIndex].currentHistoryPosition >= 0
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_left_24),
                    contentDescription = "Undo",
                    tint = if (state.frames[state.currentFrameIndex].currentHistoryPosition >= 0)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                modifier = Modifier.size(24.dp),
                onClick = { onAction(MainAction.Redo) },
                enabled = !state.isPlaybackActive && 
                    state.frames[state.currentFrameIndex].currentHistoryPosition < 
                    state.frames[state.currentFrameIndex].actionHistory.size - 1
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right_24),
                    contentDescription = "Redo",
                    tint = if (state.frames[state.currentFrameIndex].currentHistoryPosition < 
                        state.frames[state.currentFrameIndex].actionHistory.size - 1)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.alpha(if (state.isPlaybackActive) 0f else 1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        enabled = !state.isPlaybackActive,
                        onClick = {
                            onAction(MainAction.DeleteCurrentFrame)
                        },
                        onLongClick = {
                            onAction(MainAction.ShowDeleteAllDialog)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bin_32),
                    contentDescription = "Delete current frame"
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        enabled = !state.isPlaybackActive,
                        onClick = {
                            onAction(MainAction.AddNewFrame)
                        },
                        onLongClick = {
                            onAction(MainAction.ShowDuplicateFrameDialog)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_file_plus_32),
                    contentDescription = "Add new frame"
                )
            }

            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = {
                    if (state.isFrameListVisible)
                        onAction(MainAction.HideFrameList)
                    else
                        onAction(MainAction.ShowFrameList)
                },
                enabled = !state.isPlaybackActive
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_layers_32),
                    contentDescription = "Frames",
                    tint = if (state.isFrameListVisible)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isPlaybackActive) {
                IconButton(
                    modifier = Modifier.size(36.dp),
                    onClick = {
                        onAction(MainAction.StopPlayback)
                        onAction(MainAction.SaveAsGif(context))
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_save_32),
                        contentDescription = "Save as GIF"
                    )
                }
            }

            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { onAction(MainAction.StopPlayback) },
                enabled = state.isPlaybackActive
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop_32),
                    contentDescription = "Stop playback",
                    tint = if (state.isPlaybackActive)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        enabled = !state.isPlaybackActive,
                        onClick = {
                            onAction(MainAction.StartPlayback)
                        },
                        onLongClick = {
                            onAction(MainAction.ShowPlaybackSpeedDialog)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play_32),
                    contentDescription = "Start playback",
                    tint = if (!state.isPlaybackActive)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 
