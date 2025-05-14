package ru.maplib.printmap.core.theme.colors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalColorScheme = staticCompositionLocalOf { lightColorSchema() }


fun lightColorSchema(): ColorSchema {
    return ColorSchema(
        buttonBackgroundColor = Color(0xffCCCCCC),
        backgroundColor = Color(0xffFFFFFF),
        textColor = Color(0xff1c1c1c),
        primary = Color(0xff1277f3)
    )
}

fun darkColorSchema(): ColorSchema {
    return ColorSchema(
        buttonBackgroundColor = Color(0xA6000000),
        backgroundColor = Color(0xff383839),
        textColor = Color(0xffffffff),
        primary = Color(0xff1277f3)
    )
}

@Immutable
data class ColorSchema(
    val buttonBackgroundColor: Color,
    val backgroundColor: Color,
    val textColor: Color,
    val primary: Color
)
object PrintMapColorSchema {
    val colors: ColorSchema
        @[Composable ReadOnlyComposable] get() = LocalColorScheme.current
}
