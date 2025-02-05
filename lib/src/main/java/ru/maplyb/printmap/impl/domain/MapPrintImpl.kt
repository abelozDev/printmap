package ru.maplyb.printmap.impl.domain

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Bundle
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

internal class MapPrintImpl(private val activity: Activity) : MapPrint {

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
    init {
        activity.application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityDestroyed(activity: Activity) {
                if (activity == this@MapPrintImpl.activity) {
                    activity.unbindService(connection)
                }
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })
    }

    override fun onMapReady(result: (List<DownloadedImage>) -> Unit) {
        this.mapResult = object : MapResult {
            override fun onMapReady(images: List<DownloadedImage>) {
                result(images)
            }
        }
        prefs?.onUpdate { path ->
            val list = path?.let {
                val bitmap = getBitmapFromPath(it)
                createBitmaps(bitmap, activity)
            } ?: emptyList()
            mapResult?.onMapReady(list)
        }
    }

    override fun init(context: Context) {
        prefs = PreferencesDataSource.create(context.applicationContext)
    }

    override fun deleteExistedMap() {
        prefs?.removeExistedMap()
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
    ) {
        NotificationChannel.create(activity)
        val intent = Intent(activity.applicationContext, DownloadMapService::class.java).run {
            putExtra(DownloadMapService.MAP_LIST_ARG, ArrayList(mapList))
            putExtra(DownloadMapService.BOUND_ARG, bound)
            putExtra(DownloadMapService.ZOOM_ARG, zoom)
        }
        activity.startService(intent)
        activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    /** Считаем тестовый размер файла*/
    override suspend fun getPreviewSize(mapList: List<MapItem>, bound: BoundingBox, zoom: Int) {
        val tiles = GeoCalculator().calculateTotalTilesCount(bound, zoom)
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

