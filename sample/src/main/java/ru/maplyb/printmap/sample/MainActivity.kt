package ru.maplyb.printmap.sample

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.GeoPoint
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.LayerObject
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapObjectStyle
import ru.maplyb.printmap.api.model.MapType
import ru.maplyb.printmap.api.model.ObjectRes
import ru.maplyb.printmap.api.model.RectangularCoordinates
import ru.maplyb.printmap.impl.excel_generator.exportAndSendExcel
import ru.mapolib.printmap.gui.api.DownloadMapManager
import ru.mapolib.printmap.gui.api.DownloadMapState
import ru.mapolib.printmap.gui.halpers.permission.getStoragePermission
import ru.mapolib.printmap.gui.halpers.permission.requestNotificationPermission
import ru.mapolib.printmap.gui.presentation.MainScreen

class MainActivity : ComponentActivity(ru.maplyb.printmap.R.layout.activity_main) {

    private lateinit var btn: Button
    private lateinit var moscow: Button
    private lateinit var moscow_oblast: Button
    private lateinit var ukraina: Button
    private lateinit var belarus: Button
    private lateinit var dif_zone: Button
    private lateinit var report: Button
    private lateinit var composeView: ComposeView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btn = findViewById(ru.maplyb.printmap.R.id.get_map)
        moscow = findViewById(ru.maplyb.printmap.R.id.moscow_bb)
        moscow_oblast = findViewById(ru.maplyb.printmap.R.id.moscow_oblast_bb)
        ukraina = findViewById(ru.maplyb.printmap.R.id.ukraina_bb)
        belarus = findViewById(ru.maplyb.printmap.R.id.belarus_bb)
        dif_zone = findViewById(ru.maplyb.printmap.R.id.dif_zone)
        report = findViewById(ru.maplyb.printmap.R.id.report)
        composeView = findViewById(ru.maplyb.printmap.R.id.compose_view)
        getStoragePermission(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission(this)
        }
        val downloadManager = DownloadMapManager.create(this)
        val type = MapType.Online("https://mt0.google.com//vt/lyrs=s&x={x}&y={y}&z={z}")
        val item = MapItem("google", type, true, 1f, 0, 1, 24)
        val local = MapItem(
            name = "storage/emulated/0/Download/Relief_Ukraine.mbtiles",
            type = MapType.Offline("storage/emulated/0/Download/Relief_Ukraine.mbtiles"),
            isVisible = true,
            alpha = 1f,
            position = 3,
            zoomMin = 0,
            zoomMax = 13
        )
        val topoType = MapType.Online(
            path = "http://88.99.52.155/tmg/{z}/{x}/{y}"
        )
        val topoMap = MapItem(
            name = "Topo",
            type = topoType,
            isVisible = true,
            alpha = 1f,
            position = 2,
            zoomMax = 24,
            zoomMin = 0
        )
        val lats = generateSequence(51.655322, 46.976288, 5000).map {
            GeoPoint(it, 38.2526255)
        }

        val polygonLines = listOf(
            GeoPoint(49.83923201414832, 24.066391599459905),
            GeoPoint(49.549615195415925, 25.603866647224624),
            GeoPoint(48.909705689879495, 24.691518816682922),
            GeoPoint(49.25965321366459, 23.85797202028449),
        )
        val generatedObjects = lats.map {
            LayerObject.Object(
                style = MapObjectStyle(
                    color = null,
                    width = 25f,
                    name = "object"
                ),
                coords = it,
                angle = -450f,
                res = ObjectRes.Local(R.drawable.ic_mkb),/*ObjectRes.Storage("/storage/emulated/0/Download/rls.png")*/
                name = "test name",
                description = "test description to report test description to report test description to report test description to report test description to report"
            )
        }
        report.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Default) {
                exportMapToPdf(generatedObjects)
            }
        }
        val objects = listOf(
            Layer(
                objects = listOf(
                    LayerObject.Line(
                        style = MapObjectStyle(
                            color = android.graphics.Color.RED,
                            width = 5f,
                            name = "qwe"
                        ),
                        objects = lines,
                        pathEffect = "ATGM_PTUR_INSIDE"
                    )
                ),
                name = "LBS",
                selected = true,
            ),
            Layer(
                objects = listOf(
                    LayerObject.Line(
                        style = MapObjectStyle(
                            color = android.graphics.Color.RED,
                            width = 5f,
                            name = "qwe"
                        ),
                        objects = lats,
                        pathEffect = "ATGM_PTUR_INSIDE"
                    )
                ),
                name = "LBS",
                selected = true,
            ),
            Layer(
                objects = listOf(
                    LayerObject.Polygon(
                        style = MapObjectStyle(
                            color = android.graphics.Color.BLUE,
                            width = 5f,
                            name = "polygon"
                        ),
                        alpha = 25f,
                        pathEffect = "TANK_LINE",
                        objects = polygonLines,
                    )
                ),
                selected = true,
                name = "polygon"
            ),
            Layer(
                objects = listOf(
                    LayerObject.Text(
                        style = MapObjectStyle(
                            color = android.graphics.Color.BLUE,
                            width = 14f,
                            name = "text"
                        ),
                        text = "text",
                        angle = 0f,
                        coords = GeoPoint(48.36492246251455, 30.67441794587366)
                    )
                ),
                selected = true,
                name = "textetxtetxtetxtetxtetxte"
            ),
            Layer(
                objects = generatedObjects,
                selected = true,
                name = "object"
            ),
            Layer(
                objects = listOf(
                    LayerObject.Image(
                        style = MapObjectStyle(
                            color = null,
                            width = 25f,
                            name = "Image"
                        ),
                        alpha = 256f,
                        path = "/storage/emulated/0/Download/12581051_8091135_-1_1755006656662.jpg",
                        coords = RectangularCoordinates(
                            topLeft = GeoPoint(55.76898642771224, 37.579804187921255),
                            topRight = GeoPoint(55.77303248057991, 37.66059654098449),
                            bottomLeft = GeoPoint(55.73149533538331, 37.59576269971548),
                            bottomRight = GeoPoint(55.737286245963794, 37.66082294136311)
                        )
                    )
                ),
                selected = true,
                name = "Image"
            )
        )

        val list = listOf(item, local, topoMap)
        var selectedBb = BoundingBox(
            latNorth = 56.288990849810155,
            lonWest = 36.56275940461466,
            latSouth = 55.174063075936566,
            lonEast = 38.76066425183658
        )
        initBtns {
            selectedBb = it
        }

        btn.setOnClickListener {
            when (downloadManager.state.value) {
                DownloadMapState.Idle -> {
                    downloadManager.prepareDownloading(
                        boundingBox = selectedBb,
                        maps = list,
                        zoom = 10,
                        objects = objects,
                        appName = "Гроза",
                        author = "Артем"
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
            /*PalletScreen(
                android.graphics.Color.valueOf(
                    214f/255.0f, 134f/255.0f, 189f/255.0f, 255f/255.0f
                ),

            )*/
        }
    }

    private fun initBtns(selectBb: (BoundingBox) -> Unit) {
        dif_zone.setBackgroundColor(Color.GRAY)
        moscow.setBackgroundColor(Color.GRAY)
        moscow_oblast.setBackgroundColor(Color.GRAY)
        ukraina.setBackgroundColor(Color.GREEN)
        belarus.setBackgroundColor(Color.GRAY)
        moscow.setOnClickListener {
            selectBb(
                BoundingBox(
                    latNorth = 55.815435965076425,
                    lonWest = 37.51182379097406,
                    latSouth = 55.750216032030885,
                    lonEast = 37.664861616294765
                )
            )
            dif_zone.setBackgroundColor(Color.GRAY)
            moscow.setBackgroundColor(Color.GREEN)
            belarus.setBackgroundColor(Color.GRAY)
            moscow_oblast.setBackgroundColor(Color.GRAY)
            ukraina.setBackgroundColor(Color.GRAY)
        }
        moscow_oblast.setOnClickListener {
            selectBb(
                BoundingBox(
                    latNorth = 56.288990849810155,
                    lonWest = 36.56275940461466,
                    latSouth = 55.174063075936566,
                    lonEast = 38.76066425183658
                )
            )
            dif_zone.setBackgroundColor(Color.GRAY)
            moscow.setBackgroundColor(Color.GRAY)
            belarus.setBackgroundColor(Color.GRAY)
            moscow_oblast.setBackgroundColor(Color.GREEN)
            ukraina.setBackgroundColor(Color.GRAY)
        }
        dif_zone.setOnClickListener {
            //48.5161047761417, 35.18711552957413, 47.393866545904984, 38.45360194924245
            //47.61002687910439, 35.522939012989454, 47.49697788636777, 35.872305931328896
            //47.582707, 35.889823, 47.460820, 36.079036
            selectBb(
                BoundingBox(
                    latNorth = 47.582707,
                    lonWest = 35.889823,
                    latSouth = 47.460820,
                    lonEast = 36.079036
                )
            )
            dif_zone.setBackgroundColor(Color.GREEN)
            moscow.setBackgroundColor(Color.GRAY)
            moscow_oblast.setBackgroundColor(Color.GRAY)
            belarus.setBackgroundColor(Color.GRAY)
            ukraina.setBackgroundColor(Color.GRAY)
        }
        ukraina.setOnClickListener {
            selectBb(
                BoundingBox(
                    latNorth = 51.655322,
                    lonWest = 22.327316,
                    latSouth = 46.976288,
                    lonEast = 38.433272
                )
            )
            //, 24.763435134398158, 52.86889278920229, 31.755632543331444
            dif_zone.setBackgroundColor(Color.GRAY)
            belarus.setBackgroundColor(Color.GRAY)
            moscow.setBackgroundColor(Color.GRAY)
            moscow_oblast.setBackgroundColor(Color.GRAY)
            ukraina.setBackgroundColor(Color.GREEN)
        }
        belarus.setOnClickListener {
            selectBb(
                BoundingBox(
                    latNorth = 52.758111272063594,
                    lonWest = 24.579482146131223,
                    latSouth = 52.421228437452136,
                    lonEast = 25.06219033910322
                )
            )
            dif_zone.setBackgroundColor(Color.GRAY)
            belarus.setBackgroundColor(Color.GREEN)
            moscow.setBackgroundColor(Color.GRAY)
            moscow_oblast.setBackgroundColor(Color.GRAY)
            ukraina.setBackgroundColor(Color.GRAY)
        }
    }

    suspend fun exportMapToPdf(mapObjects: List<LayerObject.Object>) {
        exportAndSendExcel(this, mapObjects)

// Вариант 2: Создать файл, затем отправить позже
        /*val filePath = exportToExcel(context, objects)
        if (filePath != null) {
            sendExcelFile(context, filePath)
        }
    }*/
    }

    fun generateSequence(start: Double, end: Double, count: Int): List<Double> {
        if (count <= 0) return emptyList()

        val step = (end - start) / (count + 1)
        return List(count) { index -> start + (index + 1) * step }
    }
}