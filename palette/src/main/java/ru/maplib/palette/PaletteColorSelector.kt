package ru.maplib.palette

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColor
import ru.maplib.palette.components.ColorItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PallerColorSelector(
    initialColor: android.graphics.Color,
    selectColor: (android.graphics.Color) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        FlowRow(
            maxItemsInEachRow = 4,
        ) {
            palletColors().forEach {
                ColorItem(
                    color = it.toArgb(),
                    selected = it.toArgb() == initialColor.toArgb(),
                    select = {
                        selectColor(it.toArgb().toColor())
                    }
                )
            }
        }
    }
}