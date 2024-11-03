package ru.adonixis.yandexlivepictures.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Green,
    background = Black,
    onBackground = White,
    onSurface = White,
    onSurfaceVariant = Gray
)

private val LightColorScheme = lightColorScheme(
    primary = Green,
    background = White,
    onBackground = Black,
    onSurface = Black,
    onSurfaceVariant = Gray
)

@Composable
fun YandexLivePicturesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
