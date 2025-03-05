package ru.mapolib.printmap.gui.presentation.settings.expand

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.maplyb.printmap.api.model.Expandable
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapType

@Composable
fun ExpandableItem(
    items: List<Expandable>,
    onHeaderChange: (Boolean) -> Unit,
    onChange: (Expandable) -> Unit
) {
    val showHeader by remember(items) {
        derivedStateOf {
            items.isNotEmpty() && items.first().header != null
        }
    }
    if (showHeader) {
        val headerChecked by remember(items) {
            mutableStateOf(
                if (items.any { it.selected}) true else false
            )
        }
        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(
                    start = 8.dp,
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                text = items.first().header!!,
            )
            Checkbox(
                checked = headerChecked,
                onCheckedChange = {
                    onHeaderChange(!headerChecked)
                }
            )
        }

    }
    items.forEach { map ->
        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = map.name
                )
            }
            Checkbox(
                checked = map.selected,
                onCheckedChange = {
                    onChange(map)
                }
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
@Preview
fun PreviewExpandableItem() {
    ExpandableItem(
        items = listOf(
            MapItem(
                name = "OpenStreetMap",
                type = MapType.Online("https://mt0.google.com//vt/lyrs=s"),
                isVisible = true,
                alpha = 1f,
                position = 1,
                zoomMin = 0,
                zoomMax = 13

            ),
            MapItem(
                name = "Google",
                type = MapType.Online("https://mt0.google.com//vt/lyrs=y&x={x}&y={y}&z={z}"),
                isVisible = true,
                alpha = 1f,
                position = 2,
                zoomMin = 0,
                zoomMax = 13
            ),
            MapItem(
                name = "LocalTest",
                type = MapType.Offline("storage/emulated/0/Download/Relief_Ukraine.mbtiles"),
                isVisible = true,
                alpha = 1f,
                position = 3,
                zoomMin = 0,
                zoomMax = 13
            )
        ),
        onHeaderChange = {},
        onChange = {}
    )
}