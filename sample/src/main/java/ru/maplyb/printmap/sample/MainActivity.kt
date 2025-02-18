package ru.maplyb.printmap.sample

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.GeoPoint
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapObject
import ru.maplyb.printmap.api.model.MapObjectStyle
import ru.maplyb.printmap.api.model.MapType
import ru.mapolib.printmap.gui.api.DownloadMapManager
import ru.mapolib.printmap.gui.api.DownloadMapState
import ru.mapolib.printmap.gui.halpers.permission.getStoragePermission
import ru.mapolib.printmap.gui.halpers.permission.requestNotificationPermission
import ru.mapolib.printmap.gui.presentation.MainScreen

class MainActivity : ComponentActivity(ru.maplyb.printmap.R.layout.activity_main) {

    private lateinit var btn: Button
    private lateinit var composeView: ComposeView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btn = findViewById(ru.maplyb.printmap.R.id.get_map)
        composeView = findViewById(ru.maplyb.printmap.R.id.compose_view)
        getStoragePermission(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission(this)
        }
        val downloadManager = DownloadMapManager.create(this)
        val type = MapType.Online("https://mt0.google.com//vt/lyrs=m&x={x}&y={y}&z={z}")
        val item = MapItem("google", type, true, 1f, 0, 1, 24)
        val local = MapItem(
            name = "LocalTest",
            type = MapType.Offline("storage/emulated/0/Download/Relief_Ukraine.mbtiles"),
            isVisible = true,
            alpha = 1f,
            position = 3,
            zoomMin = 0,
            zoomMax = 13
        )
        val lines = listOf(
            GeoPoint(
                50.38030022353232, 30.226485489123323
            ),
            GeoPoint(
                49.00163585767624, 34.47819411725312
            )
        )
        val objects = mapOf(
            MapObjectStyle(
                color = android.graphics.Color.RED,
                width = 5f
            ) to listOf(
                MapObject(
                    name = "qwe",
                    position = GeoPoint(
                        50.38030022353232, 30.226485489123323
                    ),
                    isVisible = true
                ),
                MapObject(
                    name = "ewq",
                    position = GeoPoint(
                        49.00163585767624, 34.47819411725312
                    ),
                    isVisible = true
                )
            )
        )
        val list = listOf(item, local)
        btn.setOnClickListener {
            when (downloadManager.state.value) {
                DownloadMapState.Idle -> {
                    downloadManager.prepareDownloading(
                        boundingBox = BoundingBox(
                            latNorth = 51.655322,
                            lonWest = 22.327316,
                            latSouth = 46.976288,
                            lonEast = 38.433272
                        ),
                        maps = list,
                        zoom = 10,
                        objects = objects
                    )
                }

                else -> downloadManager.open()
            }
        }

        val map = listOf(
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
        )

        composeView.setContent {
            MainScreen()
        }
    }
}

fun openPrintMapDialog(activity: Activity, view: ComposeView, downloadManager: DownloadMapManager) {

    view.setContent {

    }
}