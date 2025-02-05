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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
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
                    ImageItem(images)
                }
            }
        }
    }

    @Composable
    fun ImageItem(images: List<DownloadedImage>) {
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
            AsyncImage(
                model = images[selectedImage].bitmap,
                modifier = Modifier.fillMaxSize(),
                contentDescription = images[selectedImage].description
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

}

//z = 8, x = 190, y = 225
//z = 8, x = 190, y = 226
//z = 8, x = 190, y = 227
//z = 8, x = 190, y = 228
suspend fun getTileDataMbtiles(path: String, param: List<TileParams>): ByteArray {
    return withContext(Dispatchers.IO) {
        var result = byteArrayOf()
        param.forEach { (x, y, z) ->
            val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
            println("z = $z, x = $x, y = $y")
            val query =
                "SELECT tile_data FROM tiles WHERE zoom_level = $z AND tile_column = $x AND tile_row = $y;"
            try {
                val cur = db.rawQuery(query, null)
                if (cur.moveToFirst()) {
                    do {
                        result = cur.getBlob(0)
                    } while (cur.moveToNext())
                }
                cur.close()
            } catch (e: android.database.sqlite.SQLiteException) {
            } finally {
                db.close()
            }
            /*try {
                val cursor = db.rawQuery(query, args)
                if (cursor.moveToFirst()) {
                    result[cursor.getInt(0)] = cursor.getInt(1)
                }
                cursor.close()
            } catch (e: android.database.sqlite.SQLiteException) {
                println("Error while fetching tile data: ${e.printStackTrace()}")
                e.printStackTrace()
            } finally {
                db.close()
            }*/
        }
        return@withContext result
    }
}

suspend fun getMetadataMbtiles(path: String): String? {
    return withContext(Dispatchers.IO) {
        var bounds: String? = null
        val db: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(File(path), null)

        val query = "SELECT name,value FROM metadata WHERE name = 'bounds';"
        try {
            val cur = db.rawQuery(query, null)
            if (cur.moveToFirst()) {
                do {
                    bounds = cur.getString(1)
                } while (cur.moveToNext())
            }
            cur.close()
        } catch (e: android.database.sqlite.SQLiteException) {
            bounds = null
        }
        db.close()
        bounds
    }
}