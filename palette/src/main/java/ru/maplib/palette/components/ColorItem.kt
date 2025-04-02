package ru.maplib.palette.components

import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun ColorItem(
    @ColorInt color: Int,
    selected: Boolean,
    select: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .size(64.dp)
            .border(1.dp, Color.Black, RoundedCornerShape(50.dp))
            .padding(1.dp)
            .background(color = Color(color), RoundedCornerShape(50.dp))
            .clickable(interactionSource = null, indication = null) {
                select()
            },
        contentAlignment = Alignment.Center
    ) {
        if(selected) {
            Icon(
                modifier = Modifier.size(48.dp),
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
@Preview
private fun PreviewColorItem() {
    ColorItem(
        color = android.graphics.Color.RED,
        selected = true,
        select = {}
    )
}