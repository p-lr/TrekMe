package com.peterlaurence.trekme.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = Color(0xffb38b80),
    primaryVariant = Brown700,
    secondary = Blue500,
    surface = Color(0xff2b2b2b),
    onSurface = Color(0xffa9b7c6)
)

private val LightColorPalette = lightColors(
    primary = Brown500,
    primaryVariant = Brown700,
    secondary = Blue500

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun TrekMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    background: Color? = null,
    content: @Composable() () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette.let {
            if (background != null) {
                it.copy(background = background)
            } else it
        }
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun onSurfaceAccent(): Color {
    return if (isSystemInDarkTheme()) Color(0xff6ba1ff) else Color(0xff448aff)
}

@Composable
fun lazyListBackground(): Color {
    return if (isSystemInDarkTheme()) {
        Color(0xff121212)
    } else {
        /* By default, in light mode, the background isn't taking the color
         * from the theme (e.g, it isn't white). */
        Color(0xfffafafa)
    }
}