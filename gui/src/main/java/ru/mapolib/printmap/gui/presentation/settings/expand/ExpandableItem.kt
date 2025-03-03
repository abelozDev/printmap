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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.maplyb.printmap.api.model.Expandable

@Composable
fun ExpandableItem(
    items: List<Expandable>,
    onChange: (Expandable) -> Unit
) {
    val showHeader by remember(items) {
        derivedStateOf {
            items.isNotEmpty() && items.first().header != null
        }
    }
    if (showHeader) {
        Text(
            modifier = Modifier.padding(
                start = 8.dp,
                bottom = 8.dp
            ),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            text = items.first().header!!,
        )
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