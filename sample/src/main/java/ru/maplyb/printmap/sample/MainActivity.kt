package ru.maplyb.printmap.sample

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.DownloadedImage
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapType
import ru.maplyb.printmap.impl.domain.model.TileParams
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getStoragePermission()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }
        val mapPrint = MapPrint.create(this)
        mapPrint.init(this)
        val bbox = BoundingBox(
            latNorth = 51.655322,
            lonWest = 22.327316,
            latSouth = 46.976288,
            lonEast = 38.433272,
        )
        //накладывает offline на online
        /*latNorth = 48.80033250943958,
        lonWest = 20.30710968815696,
        latSouth = 47.5057647015311,
        lonEast = 24.176979174180058,*/

        /* ,беларусб
          latNorth = 53.85397,
            lonWest = 25.77757,
            latSouth = 53.37413,
            lonEast = 29.49843,
            */
        //киев
        /*
        latNorth = 50.85835,
                    lonWest = 29.70180,
                    latSouth = 50.01655,
                    lonEast = 31.72720,
                    */

        //украина + беларусь
        /*
        latNorth = 52.33238,
                    latSouth = 51.13283,
                    lonEast = 27.85486,
                    lonWest = 24.85486,
                    */
        val map = listOf(
            /*MapItem(
                name = "OpenStreetMap",
                type = MapType.Online("https://mt0.google.com/vt/lyrs=s"),
                isVisible = true,
                alpha = 250f,
                position = 1
            ),
            MapItem(
                name = "Google",
                type = MapType.Online("https://mt0.google.com//vt/lyrs=m"),
                isVisible = true,
                alpha = 100f,
                position = 2
            ),*/
            MapItem(
                name = "LocalTest",
                type = MapType.Offline("storage/emulated/0/Download/Relief_Ukraine.mbtiles"),
                isVisible = true,
                alpha = 255f,
                position = 3
            )
        )
        setContent {
            val coroutineScope = rememberCoroutineScope()
            var images by remember {
                mutableStateOf<List<DownloadedImage>>(emptyList())
            }
            LaunchedEffect(Unit) {
                mapPrint
                    .onMapReady {
                        images = it
                    }
            }
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ///storage/emulated/0/Download/basic.mbtiles
                    ///storage/emulated/0/Download/Relief_Ukraine.mbtiles
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                    mapPrint.startFormingAMap(
                                        map,
                                        bbox,
                                        zoom = 10,
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
                    ImageItem(images) {
                        mapPrint.deleteExistedMap()
                    }
                }
            }
        }
    }

    @Composable
    fun ImageItem(
        images: List<DownloadedImage>,
        delete: () -> Unit) {
        if (images.isEmpty()) return
        var selectedImage by remember {
            mutableIntStateOf(0)
        }
        Column {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                items(images.size) { index ->
                    val image = images[index]
                    Button(
                        content = {
                            Text(text = image.description)
                        },
                        onClick = {
                            selectedImage = index
                        }
                    )
                }
            }
            Text(
                text = "Размер файла: ${formatSize(images[0].bitmap?.byteCount ?: 0)}"
            )
            AsyncImage(
                model = images[selectedImage].bitmap,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = images[selectedImage].description
            )
            Image(
                modifier = Modifier.clickable {
                    delete()
                },
                imageVector = Icons.Default.Delete,
                contentDescription = null
            )
        }
    }

    fun getStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(
                        this,
                        "Устройство не поддерживает запрос этого разрешения",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestNotificationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
    }
    fun formatSize(bytes: Int): String {
        val units = arrayOf("Б", "КБ", "МБ", "ГБ", "ТБ")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.lastIndex) {
            size /= 1024
            unitIndex++
        }

        return "%.2f %s".format(size, units[unitIndex])
    }
}