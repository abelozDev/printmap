package ru.mapolib.printmap.gui.presentation.downloaded.expandable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.maplyb.printmap.R
import ru.maplyb.printmap.api.model.Layer
import ru.mapolib.printmap.gui.presentation.settings.expand.ExpandableItem
import kotlin.math.roundToInt

@Composable
fun LayersExpandable(
    layers: List<Layer>,
    updateLayer: (Layer) -> Unit,
) {
    var isOpen by remember {
        mutableStateOf(false)
    }
    Row(
        modifier = Modifier
            .clickable {
                isOpen = !isOpen
            }
    ) {
        Text(
            text = stringResource(R.string.selected_layers)
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null
        )
    }
    Spacer(Modifier.height(16.dp))
    AnimatedVisibility(
        visible = isOpen,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column {
            ExpandableItem(
                items = layers,
                onChange = { layer ->
                    updateLayer(
                        (layer as Layer).copy(
                            selected = !layer.selected
                        )
                    )
                },
                onHeaderChange = {
                    //todo
                }
            )
        }
    }
}