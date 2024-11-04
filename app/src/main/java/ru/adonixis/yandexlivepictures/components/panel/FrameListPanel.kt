package ru.adonixis.yandexlivepictures.components.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.adonixis.yandexlivepictures.Frame
import ru.adonixis.yandexlivepictures.R
import ru.adonixis.yandexlivepictures.theme.Black

@Composable
fun FrameListPanel(
    isVisible: Boolean,
    currentFrameIndex: Int,
    frames: List<Frame>,
    canvasSize: Size,
    thumbnailHeight: Dp,
    animationDurationMillis: Int,
    alphaBackground: Float,
    alphaBorder: Float,
    onFrameSelected: (Int) -> Unit,
    onFrameDuplicated: (Int) -> Unit,
    onFrameDeleted: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = animationDurationMillis)),
        exit = fadeOut(animationSpec = tween(durationMillis = animationDurationMillis)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clickable(enabled = false) { }
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaBackground),
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alphaBorder),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(12.dp)
        ) {
            val listState = rememberLazyListState()

            LaunchedEffect(isVisible, currentFrameIndex) {
                if (isVisible) {
                    val isItemVisible = listState.layoutInfo.visibleItemsInfo.any { 
                        it.index == currentFrameIndex 
                    }

                    if (!isItemVisible) {
                        listState.animateScrollToItem(currentFrameIndex)
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                state = listState
            ) {
                items(frames.indices.toList()) { index ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(thumbnailHeight)
                                .aspectRatio(
                                    canvasSize.width / canvasSize.height
                                )
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    color = if (index == currentFrameIndex)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (index == currentFrameIndex)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = alphaBackground),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { onFrameSelected(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            frames[index].thumbnail?.let { thumbnail ->
                                Image(
                                    bitmap = thumbnail,
                                    contentDescription = "Frame thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (index == currentFrameIndex)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Black
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                modifier = Modifier.size(24.dp),
                                onClick = { onFrameDuplicated(index) }
                            ) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    painter = painterResource(id = R.drawable.ic_duplicate_24),
                                    contentDescription = "Duplicate frame",
                                    tint = Black
                                )
                            }

                            IconButton(
                                modifier = Modifier.size(24.dp),
                                onClick = { onFrameDeleted(index) }
                            ) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    painter = painterResource(id = R.drawable.ic_bin_32),
                                    contentDescription = "Delete frame",
                                    tint = Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
