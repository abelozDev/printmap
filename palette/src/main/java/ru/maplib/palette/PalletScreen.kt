package ru.maplib.palette

import androidx.compose.foundation.background
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

@Composable
fun PalletScreen(
    initialColor: android.graphics.Color
) {
    var currentColor by remember(initialColor) {
            mutableStateOf(initialColor)
    }
    Column(
        modifier = Modifier
            .background(Color.White)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center),
                text = "Выбор цвета",
                fontWeight = FontWeight.Bold
            )
            Icon(
                modifier = Modifier
                    .align(Alignment.TopEnd),
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
    }
}

internal fun palletColors(): List<Color> {
    return listOf(
        Color(0xffD32F2F),
        Color(0xff1976D2),
        Color(0xff388E3C),
        Color(0xffFBC02D),
        Color(0xff8E24AA),
        Color(0xffFF5722),
        Color(0xff00BCD4),
        Color(0xffE91E63),
        Color(0xff4CAF50),
        Color(0xff303F9F),
        Color(0xffFFEB3B),
        Color(0xff1DE9B6),
        Color(0xffE040FB),
        Color(0xffFFA000),
        Color(0xff512DA8),
        Color(0xffFF3D00),
        Color(0xff689F38),
        Color(0xffD84315),
        Color(0xff0288D1),
        Color(0xff212121),
        Color(0xffFFFFFF),
    )
}

/*val randomColor: Int
    get() = Random.nextInt(1, 255)

fun generateColors(): List<Int> {
    return List(12) {
        Color(
            red = randomColor,
            green = randomColor,
            blue = randomColor
        ).toArgb()
    }
}*/

@Composable
@Preview
private fun PreviewPalletScreen() {
//    PalletScreen(
//
//    )
}
