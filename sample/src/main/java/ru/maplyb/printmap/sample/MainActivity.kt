package ru.maplyb.printmap.sample

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapType
import ru.mapolib.printmap.gui.api.DownloadMapManager
import ru.mapolib.printmap.gui.api.DownloadMapState
import ru.mapolib.printmap.gui.halpers.permission.getStoragePermission
import ru.mapolib.printmap.gui.halpers.permission.requestNotificationPermission
import ru.mapolib.printmap.gui.presentation.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getStoragePermission(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission(this)
        }

        val downloadManager = DownloadMapManager.create(this)

        val bbox = BoundingBox(
            latNorth = 60.0,
            lonWest = -10.0,
            latSouth = 52.0,
            lonEast = 10.0,
        )
        /*latNorth = 51.655322,
            lonWest = 22.327316,
            latSouth = 46.976288,
            lonEast = 38.433272,*/

        /*latNorth = 48.80033250943958,
        lonWest = 20.30710968815696,
        latSouth = 47.5057647015311,
        lonEast = 24.176979174180058,*/
        val map = listOf(
            MapItem(
                name = "OpenStreetMap",
                type = MapType.Online("https://mt0.google.com/vt/lyrs=s"),
                isVisible = true,
                alpha = 250f,
                position = 1,
                zoomMin = 0,
                zoomMax = 13

            ),
            MapItem(
                name = "Google",
                type = MapType.Online("https://mt0.google.com//vt/lyrs=m"),
                isVisible = true,
                alpha = 250f,
                position = 2,
                zoomMin = 0,
                zoomMax = 13
            ),
            MapItem(
                name = "LocalTest",
                type = MapType.Offline("storage/emulated/0/Download/Relief_Ukraine.mbtiles"),
                isVisible = true,
                alpha = 255f,
                position = 3,
                zoomMin = 0,
                zoomMax = 13
            )
        )
        setContent {
            val downloadState by downloadManager.state.collectAsStateWithLifecycle()
            Button(
                onClick = {
                    when(downloadState) {
                        DownloadMapState.Idle -> {
                            downloadManager.prepareDownloading(
                                boundingBox = bbox,
                                maps = map,
                                zoom = 10
                            )
                        }
                        else -> downloadManager.open()
                    }
                },
                content = {
                    Text(
                        text = "Get Map",
                    )
                }
            )
            MainScreen()
            /*DownloadMapSettingsScreen(
                *//*activity = this,*//*
                boundingBox = bbox,
                maps = map,
                zoom = 10
            )*/
        }
    }
}