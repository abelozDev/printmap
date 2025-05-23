package ru.maplib.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.maplib.printmap.core.theme.colors.PrintMapButtonColors
import ru.maplib.printmap.core.theme.colors.PrintMapColorSchema

@Composable
fun PalletScreen(
    initialColor: android.graphics.Color,
    dismiss: (android.graphics.Color?) -> Unit
) {
    var currentColor by remember(initialColor) {
            mutableStateOf(initialColor)
    }
    Column(
        modifier = Modifier
            .background(PrintMapColorSchema.colors.backgroundColor)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                color = PrintMapColorSchema.colors.textColor,
                modifier = Modifier
                    .align(Alignment.Center),
                text = "Выбор цвета",
                fontWeight = FontWeight.Bold
            )
            Icon(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable {
                        dismiss(currentColor)
                    },
                imageVector = Icons.Default.Close,
                tint = Color.Red,
                contentDescription = null
            )
        }
        Spacer(Modifier.height(16.dp))
        val pagerState = rememberPagerState(pageCount = { 2 })
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
        ) { page ->
            when (page) {
                1 -> CustomColorSelector(currentColor) {
                    currentColor = it
                }
                else -> PallerColorSelector(currentColor) {
                    currentColor = it
                }
            }
        }
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color =
                    if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            colors = PrintMapButtonColors(),
            onClick = {
                dismiss(null)
            },
            content = {
                Text(
                    color = PrintMapColorSchema.colors.textColor,
                    text = "ПО УМОЛЧАНИЮ"
                )
            }
        )
    }
}



@Composable
@Preview
private fun PreviewPalletScreen() {
//    PalletScreen(
//
//    )
}
