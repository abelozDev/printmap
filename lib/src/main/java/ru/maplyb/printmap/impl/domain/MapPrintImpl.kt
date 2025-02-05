package ru.maplyb.printmap.impl.domain

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.DownloadedImage
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource
import ru.maplyb.printmap.impl.domain.model.TileParams
import ru.maplyb.printmap.impl.domain.repo.DownloadTilesManager
import ru.maplyb.printmap.impl.service.DownloadMapService
import ru.maplyb.printmap.impl.service.MapResult
import ru.maplyb.printmap.impl.service.NotificationChannel
import ru.maplyb.printmap.impl.util.GeoCalculator
import ru.maplyb.printmap.impl.util.TILES_SIZE_TAG
import ru.maplyb.printmap.impl.util.cropCenter
import ru.maplyb.printmap.impl.util.debugLog
import ru.maplyb.printmap.impl.util.getBitmapFromPath
import ru.maplyb.printmap.impl.util.limitSize

internal class MapPrintImpl(private val context: Context) : MapPrint {

    private var mapResult: MapResult? = null
    private lateinit var mService: DownloadMapService
    private var mBound: Boolean = false
    private var prefs: PreferencesDataSource? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as DownloadMapService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onMapReady(result: (List<DownloadedImage>) -> Unit) {
        this.mapResult = object : MapResult {
            override fun onMapReady(images: List<DownloadedImage>) {
                result(images)
            }
        }
        prefs?.onUpdate {
            if (it == null) return@onUpdate
            val bitmap = getBitmapFromPath(it)
            val list = createBitmaps(bitmap, context)
            mapResult?.onMapReady(list)
        }
    }

    override fun init(context: Context) {
        prefs = PreferencesDataSource.create(context)
    }

    override fun getTilesCount(
        bound: BoundingBox,
        zoom: Int,
    ): List<TileParams> {
        return GeoCalculator().calculateTotalTilesCount(bound, zoom)
    }

    override suspend fun startFormingAMap(
        mapList: List<MapItem>,
        bound: BoundingBox,
        zoom: Int,
        /*onResult: (List<DownloadedImage>) -> Unit*/
    ) {
        NotificationChannel.create(context)
        val intent = Intent(context.applicationContext, DownloadMapService::class.java).run {
            putExtra(DownloadMapService.MAP_LIST_ARG, ArrayList(mapList))
            putExtra(DownloadMapService.BOUND_ARG, bound)
            putExtra(DownloadMapService.ZOOM_ARG, zoom)
        }
        context.startService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        /*delay(200)
        if (mBound) {
            mService.setMapResult {
                onResult(it)
            }
        }*/
    }

    /** Считаем тестовый размер файла*/
    override suspend fun getPreviewSize(mapList: List<MapItem>, bound: BoundingBox, zoom: Int) {
        val tiles = GeoCalculator().calculateTotalTilesCount(bound, zoom)
        val visibleMaps = mapList.filter { it.isVisible }
        val tileManager = DownloadTilesManager.create(context)
        val approximateSize = 255 * 255 * 4 * tiles.size
        debugLog(TILES_SIZE_TAG, "Approximate size: $approximateSize")
    }

    private fun createBitmaps(resultBitmap: Bitmap?, context: Context): List<DownloadedImage> {
        val detail = resultBitmap?.cropCenter(255 * 2, 255 * 2)
        return listOf(
            DownloadedImage(
                resultBitmap?.limitSize(context),
                "Превью"
            ),
            DownloadedImage(
                detail,
                "Детальный"
            )
        )
    }
}