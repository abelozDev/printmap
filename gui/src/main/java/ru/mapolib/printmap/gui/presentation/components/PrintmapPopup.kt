package ru.mapolib.printmap.gui.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import ru.maplib.printmap.core.theme.colors.PrintMapColorSchema

@Composable
internal fun <T> PrintmapPopup(
    name: String,
    items: List<T>,
    selected: T,
    update: (T) -> Unit
) {
    var visibility by remember {
        mutableStateOf(false)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable {
            visibility = !visibility
        }
    ) {
        Text(
            color = PrintMapColorSchema.colors.textColor,
            text = name
        )
        Spacer(Modifier.weight(1f))
        Text(
            color = PrintMapColorSchema.colors.textColor,
            text = selected.toString()
        )
        Icon(
            imageVector = if (visibility) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            tint = PrintMapColorSchema.colors.textColor,
            contentDescription = null
        )
    }
    Spacer(Modifier.height(8.dp))
    if (visibility) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = {
                    visibility = !visibility
                },
                properties = PopupProperties()
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.DarkGray)
                        .padding(8.dp)
                ) {
                    Column {
                        items.forEach {
                            Text(
                                modifier = Modifier
                                    .clickable {
                                        update(it)
                                        visibility = false
                                    }
                                    .padding(8.dp),
                                text = it.toString(),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}