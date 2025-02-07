package ru.maplyb.printmap.impl.service

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.DownloadedImage
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource.Companion.MAP_PATH_KEY
import ru.maplyb.printmap.impl.domain.repo.DownloadTilesManager
import ru.maplyb.printmap.impl.util.FileSaveUtil
import ru.maplyb.printmap.impl.util.GeoCalculator
import ru.maplyb.printmap.impl.util.TilesUtil
import ru.maplyb.printmap.impl.util.saveBitmapToExternalStorage
import ru.maplyb.printmap.impl.util.serializable

internal class DownloadMapService : Service() {

    private val binder = LocalBinder()

    /*private var mapResult: MapResult? = null*/
    private var prefs: PreferencesDataSource? = null

    /* fun setMapResult(callback: (List<DownloadedImage>) -> Unit) {
         this.mapResult = object : MapResult {
             override fun onMapReady(images: List<DownloadedImage>) {
                 callback(images)
             }
         }
     }*/

    override fun onBind(p0: Intent?): IBinder = binder

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    inner class LocalBinder : Binder() {
        fun getService(): DownloadMapService = this@DownloadMapService
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesDataSource.create(this.applicationContext)
    }

    private fun downloadMap(
        mapList: List<MapItem>,
        bound: BoundingBox,
        zoom: Int,
    ) {
        val tiles = GeoCalculator().calculateTotalTilesCount(bound, zoom).successDataOrNull() ?: return
        val visibleMaps = mapList.filter { it.isVisible }
        val fullSize = visibleMaps.size * tiles.size
        startForeground(
            DOWNLOAD_MAP_NOTIFICATION_ID,
            createNotification(
                progressText = "Начало загрузки",
                maxProgress = fullSize,
                progress = 0
            )
        )
        coroutineScope.launch {
            val tileManager = DownloadTilesManager.create(this@DownloadMapService)

            val downloadedTiles = tileManager.getTiles(visibleMaps, tiles) {
                updateNotification("Загрузка тайлов", fullSize, it)
            }
            val resultBitmap = TilesUtil()
                .mergeTilesSortedByCoordinates(
                    downloadedTiles,
                    tiles.minOf { it.x },
                    tiles.maxOf { it.x },
                    tiles.minOf { it.y },
                    tiles.maxOf { it.y },
                    zoom
                )
//            val saver = FileSaveUtil(this@DownloadMapService)
            saveBitmapToExternalStorage(
                context = this@DownloadMapService,
                bitmap = resultBitmap!!,
                fileName = "${System.currentTimeMillis()}"
            )
                ?.let {
                    prefs?.saveMapPath(MAP_PATH_KEY, it)
                }
            /*saver.saveBitmapToGallery(
                this@DownloadMapService,
                resultBitmap!!,
                "${System.currentTimeMillis()}"
            )?.let {
                prefs?.saveMapPath(MAP_PATH_KEY, it)
            }*/
            tileManager.deleteTiles(downloadedTiles.values.flatten())
            stopForeground(true)
            stopSelf()
        }
    }

    private fun updateNotification(message: String, maxProgress: Int, progress: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this@DownloadMapService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notifyNewNotification(message, maxProgress, progress)
            }
        } else {
            notifyNewNotification(message, maxProgress, progress)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun notifyNewNotification(message: String, maxProgress: Int, progress: Int) {
        val updatedNotification = createNotification(
            progressText = message,
            maxProgress = maxProgress,
            progress = progress
        )
        NotificationManagerCompat.from(this@DownloadMapService)
            .notify(DOWNLOAD_MAP_NOTIFICATION_ID, updatedNotification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val mapList = intent.serializable(MAP_LIST_ARG) as? ArrayList<MapItem>
                ?: throw TypeCastException("Fail cast to MapItem")
            val bound = intent.serializable(BOUND_ARG) as? BoundingBox
                ?: throw TypeCastException("fail cast to BoundingBox")
            val zoom = intent.getIntExtra(ZOOM_ARG, 0)
            downloadMap(mapList.toList(), bound, zoom)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotification(
        maxProgress: Int,
        progress: Int,
        progressText: String
    ): Notification {
        return NotificationCompat.Builder(this, NotificationChannel.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("Загрузка карты")
            .setContentText(progressText)
            .setProgress(maxProgress, progress, false)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val MAP_LIST_ARG = "mapList"
        const val BOUND_ARG = "bound"
        const val ZOOM_ARG = "zoom"
        const val DOWNLOAD_MAP_NOTIFICATION_ID = 788843
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.coroutineContext.cancelChildren()
    }
}