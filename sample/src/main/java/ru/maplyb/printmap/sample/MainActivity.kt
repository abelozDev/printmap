package ru.maplyb.printmap.sample

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val bbox = BoundingBox(
            latNorth = 85.0511287798066, // Почти самый верхний край карты
            latSouth = 0.0,             // Экватор
            lonEast = 90.0,             // 2 тайла по X (45° * 2)
            lonWest = 0.0               // От нулевого меридиана
        )
        val map = MapItem(
            name = "OpenStreetMap",
            type = MapType.Online("https://mt0.google.com//vt/lyrs=s"),
            isVisible = true,
            alpha = 140f,
            position = 1
        )
        setContent {
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current
            var bitmap by remember {
                mutableStateOf<Bitmap?>(null)
            }
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                MapPrint.create(context)
                                    .startFormingAMap(
                                        listOf(map),
                                        bbox,
                                        zoom = 5,
                                        onResult = {
                                            bitmap = it
                                        }
                                    )
                            }
                        },
                        content = {
                            Text(
                                text = "Get Map",
                                modifier = Modifier.padding()
                            )
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
}