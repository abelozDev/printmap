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
import ru.maplyb.printmap.api.model.MapItem
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
        enableEdgeToEdge()
        getStoragePermission(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission(this)
        }
        val downloadManager = DownloadMapManager.create(this)
        val type = MapType.Online("https://mt0.google.com//vt/lyrs=y&x={x}&y={y}&z={z}")
        val item = MapItem("google", type, true, 1f, 0, 1, 24)
        val list = listOf(item)
        btn.setOnClickListener {
            when (downloadManager.state.value) {
                DownloadMapState.Idle -> {
                    downloadManager.prepareDownloading(
                        boundingBox = BoundingBox(55.892186, 55.816814, 38.516507, 38.357271),
                        maps = list,
                        zoom = 10
                    )
                }

                else -> downloadManager.open()
            }
        }

        val bbox = BoundingBox(
            latNorth = 60.0,
            lonWest = -10.0,
            latSouth = 52.0,
            lonEast = 10.0,
        )
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
        /*setContent {
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
            *//*DownloadMapSettingsScreen(
                *//**//*activity = this,*//**//*
                boundingBox = bbox,
                maps = map,
                zoom = 10
            )*//*
        }*/

        composeView.setContent {
            val downloadState by downloadManager.state.collectAsStateWithLifecycle()
            /*when (downloadState) {
                DownloadMapState.Idle -> {
                    downloadManager.prepareDownloading(
                        boundingBox = BoundingBox(55.892186, 55.816814, 38.516507, 38.357271),
                        maps = list,
                        zoom = 10
                    )
                }

                else -> downloadManager.open()
            }*/
            MainScreen()
        }
    }
}

fun openPrintMapDialog(activity: Activity, view: ComposeView, downloadManager: DownloadMapManager) {

    view.setContent {

    }
}