package ru.maplyb.printmap.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.maplyb.printmap.api.presentation.DownloadMapSettingsScreen
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        ru.printmap.print_gui.halpers.permission.getStoragePermission(this)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            ru.printmap.print_gui.halpers.permission.requestNotificationPermission(this)
//        }
        val bbox = BoundingBox(
            latNorth = 51.655322,
            lonWest = 22.327316,
            latSouth = 46.976288,
            lonEast = 38.433272,
        )
        val map = listOf(
            MapItem(
                name = "OpenStreetMap",
                type = MapType.Online("https://mt0.google.com/vt/lyrs=s"),
                isVisible = true,
                alpha = 250f,
                position = 1,
                zoomMin = 0,
                zoomMax = 8

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
            DownloadMapSettingsScreen(
                activity = this,
                boundingBox = bbox,
                maps = map,
                zoom = 10
            )
        }
    }
}