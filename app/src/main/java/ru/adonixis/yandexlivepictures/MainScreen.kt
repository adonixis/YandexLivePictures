package ru.adonixis.yandexlivepictures

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.adonixis.yandexlivepictures.theme.Blue
import ru.adonixis.yandexlivepictures.theme.YandexLivePicturesTheme
import androidx.compose.ui.graphics.StrokeJoin

private fun Path.drawSmoothLine(points: List<Offset>) {
    if (points.size > 1) {
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val p0 = if (i > 0) points[i - 1] else points[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i < points.size - 2) points[i + 2] else p2
            
            val controlPoint1X = p1.x + (p2.x - p0.x) / 6
            val controlPoint1Y = p1.y + (p2.y - p0.y) / 6
            val controlPoint2X = p2.x - (p3.x - p1.x) / 6
            val controlPoint2Y = p2.y - (p3.y - p1.y) / 6
            
            cubicTo(
                controlPoint1X, controlPoint1Y,
                controlPoint2X, controlPoint2Y,
                p2.x, p2.y
            )
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val state = viewModel.state.collectAsState().value

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    modifier = Modifier
                        .size(24.dp),
                    onClick = {

                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_left_24),
                        contentDescription = "Undo icon"
                    )
                }

                IconButton(
                    modifier = Modifier
                        .size(24.dp),
                    onClick = {

                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right_24),
                        contentDescription = "Redo icon"
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    modifier = Modifier
                        .size(32.dp),
                    onClick = {

                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bin_32),
                        contentDescription = "Bin icon"
                    )
                }

                IconButton(
                    modifier = Modifier
                        .size(32.dp),
                    onClick = {

                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_file_plus_32),
                        contentDescription = "File plus icon"
                    )
                }

                IconButton(
                    modifier = Modifier
                        .size(32.dp),
                    onClick = {

                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_layers_32),
                        contentDescription = "Layers icon"
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    modifier = Modifier
                        .size(32.dp),
                    onClick = {

                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_stop_32),
                        contentDescription = "Stop icon"
                    )
                }

                IconButton(
                    modifier = Modifier
                        .size(32.dp),
                    onClick = {

                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play_32),
                        contentDescription = "Play icon"
                    )
                }
            }
        }
        val paths = remember {
            mutableStateListOf<MutableList<Offset>>()
        }
        val currentPath = remember {
            mutableStateListOf<Offset>()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 32.dp, bottom = 22.dp)
                .clip(shape = RoundedCornerShape(20.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg_paper),
                contentDescription = "Paper background",
                contentScale = ContentScale.Crop
            )

            // Canvas for drawing
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.isPencilEnabled) {
                        if (state.isPencilEnabled) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPath.add(offset)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    currentPath.add(change.position)
                                },
                                onDragEnd = {
                                    paths.add(currentPath.toMutableList())
                                    currentPath.clear()
                                }
                            )
                        }
                    }
            ) {
                paths.forEach { path ->
                    if (path.size > 1) {
                        drawPath(
                            path = Path().apply { drawSmoothLine(path) },
                            color = Color.Black,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
                
                if (currentPath.size > 1) {
                    drawPath(
                        path = Path().apply { drawSmoothLine(currentPath) },
                        color = Color.Black,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(bottom = 16.dp) // TODO: Do we need this padding?
                .height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier
                    .size(32.dp),
                onClick = {
                    viewModel.onAction(MainAction.TogglePencilTool)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pencil_32),
                    contentDescription = "Pencil icon",
                    tint = if (state.isPencilEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                modifier = Modifier
                    .size(32.dp),
                onClick = {

                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_brush_32),
                    contentDescription = "Brush icon"
                )
            }

            IconButton(
                modifier = Modifier
                    .size(32.dp),
                onClick = {

                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_erase_32),
                    contentDescription = "Erase icon"
                )
            }

            IconButton(
                modifier = Modifier
                    .size(32.dp),
                onClick = {

                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_instruments_32),
                    contentDescription = "Instruments icon"
                )
            }

            IconButton(
                modifier = Modifier
                    .size(32.dp),
                onClick = {

                }
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = Blue, shape = CircleShape),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    YandexLivePicturesTheme {
        MainScreen()
    }
}
