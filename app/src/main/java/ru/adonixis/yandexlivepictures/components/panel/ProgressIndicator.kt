package ru.adonixis.yandexlivepictures.components.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(enabled = false) { }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}
