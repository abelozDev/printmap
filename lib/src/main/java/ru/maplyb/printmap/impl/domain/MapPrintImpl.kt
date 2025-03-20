package ru.maplyb.printmap.impl.domain

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.FormingMapArgs
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.impl.domain.local.MapPath
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource
import ru.maplyb.printmap.impl.domain.model.TileParams
import ru.maplyb.printmap.impl.service.DownloadMapService
import ru.maplyb.printmap.impl.service.MapResult
import ru.maplyb.printmap.impl.service.NotificationChannel
import ru.maplyb.printmap.impl.util.DestroyLifecycleCallback
import ru.maplyb.printmap.impl.util.GeoCalculator

internal class MapPrintImpl(
    private val activity: Activity,
    private val prefs: PreferencesDataSource
) : MapPrint {
    private var mapResult: MapResult? = null
    private lateinit var mService: DownloadMapService
    private var mBound: Boolean = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as DownloadMapService.LocalBinder
            println("onServiceConnected")
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    init {
        println("onServiceConnected init")
        activity.application.registerActivityLifecycleCallbacks(DestroyLifecycleCallback { activity ->
            if (activity == this@MapPrintImpl.activity) {
                try {
                    activity.unbindService(connection)
                } catch (_: IllegalArgumentException) {
                }
            }
        }
        )
        if (isServiceRunning(activity, DownloadMapService::class.java)) {
            println("onServiceConnected isServiceRunning")
            val intent = Intent(activity.applicationContext, DownloadMapService::class.java)
            activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onMapReady(result: (MapPath?) -> Unit) {
        this.mapResult = object : MapResult {
            override fun onMapReady(image: MapPath?) {
                result(image)
            }
        }
    }

    override fun cancelDownloading() {
        mService.cancelDownloading()
        if (mBound) {
            activity.unbindService(connection)
        }
    }

    override suspend fun deleteExistedMap(path: String) {
        prefs.remove(activity, path)
    }

    override suspend fun getTilesCount(
        bound: BoundingBox,
        zoom: Int,
    ): OperationResult<List<TileParams>> {
        return GeoCalculator.calculateTotalTiles(bound, zoom)
    }

    override suspend fun startFormingAMap(
        args: FormingMapArgs,
        ) {
        NotificationChannel.create(activity)
        val intent = Intent(activity.applicationContext, DownloadMapService::class.java).run {
            putExtra(DownloadMapService.FORMING_MAP_ARGS, args)
        }
        activity.startService(intent)
        activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    /** Считаем тестовый размер файла*/
    override suspend fun getPreviewSize(mapList: List<MapItem>, bound: BoundingBox, zoom: Int) {
        /*val tiles = GeoCalculator().calculateTotalTilesCount(bound, zoom)
        val approximateSize = 255 * 255 * 4 * tiles.size
        debugLog(TILES_SIZE_TAG, "Approximate size: $approximateSize")*/
    }
}

private fun isServiceRunning(activity: Activity, serviceClass: Class<*>): Boolean {
    val manager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return manager.getRunningServices(Int.MAX_VALUE)
        .any { it.service.className == serviceClass.name }
}


