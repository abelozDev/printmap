package ru.maplyb.printmap.impl.service

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.GeoPoint
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapObject
import ru.maplyb.printmap.api.model.MapObjectStyle
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource
import ru.maplyb.printmap.impl.domain.repo.DownloadTilesManager
import ru.maplyb.printmap.impl.util.DrawInBitmap
import ru.maplyb.printmap.impl.util.GeoCalculator
import ru.maplyb.printmap.impl.util.MergeTiles
import ru.maplyb.printmap.impl.util.saveBitmapToExternalStorage
import ru.maplyb.printmap.impl.util.serializable

internal class DownloadMapService : Service() {

    private val binder = LocalBinder()

    private var prefs: PreferencesDataSource? = null

    override fun onBind(p0: Intent?): IBinder = binder

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    inner class LocalBinder : Binder() {
        fun getService(): DownloadMapService = this@DownloadMapService
    }

    fun cancelDownloading() {
        coroutineScope.launch {
            prefs?.clear(
                context = this@DownloadMapService,
            )
        }.invokeOnCompletion {
            coroutineScope.cancel()
        }
        stopSelf()
        stopForeground(true)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(DOWNLOAD_MAP_NOTIFICATION_ID)
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesDataSource.create()
    }

    private fun downloadMap(
        mapList: List<MapItem>,
        bound: BoundingBox,
        zoom: Int,
        objects: Map<MapObjectStyle, List<MapObject>>,
        quality: Int
    ) {
        coroutineScope.launch {
            val tiles = GeoCalculator().calculateTotalTilesCount(bound, zoom).successDataOrNull()
                ?: return@launch
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
            val tileManager = DownloadTilesManager.create(this@DownloadMapService)

            val downloadedTiles = tileManager.getTiles(visibleMaps, tiles, quality) {
                updateNotification("Загрузка тайлов", fullSize, it)
                prefs?.setProgress(
                    context = this@DownloadMapService,
                    progress = (it.toFloat() / fullSize * 100).toInt()
                )
            }
            when (downloadedTiles) {
                is OperationResult.Error -> {
                    prefs?.setError(this@DownloadMapService, downloadedTiles.message)
                }
                is OperationResult.Success -> {
                    MergeTiles()
                        .mergeTilesSortedByCoordinates(
                            downloadedTiles.data,
                            tiles.minOf { it.x },
                            tiles.maxOf { it.x },
                            tiles.minOf { it.y },
                            tiles.maxOf { it.y },
                            zoom
                        ).onSuccess {
                            //50.38030022353232, 30.226485489123323
                            //49.00163585767624, 34.47819411725312

                            val currentBound = GeoCalculator().tilesToBoundingBox(tiles, zoom)
                            val bitmapWithDraw = if (objects.isNotEmpty()) DrawInBitmap().draw(
                                bitmap = it!!,
                                currentBound
                                /*bound*/,
                                objects = objects,
                                zoom = zoom
                            ) else it!!
                            saveBitmapToExternalStorage(
                                context = this@DownloadMapService,
                                bitmap = bitmapWithDraw,
                                fileName = "${System.currentTimeMillis()}"
                            )
                                ?.let {
                                    prefs?.setDownloaded(
                                        context = this@DownloadMapService,
                                        path = it
                                    )
                                }
                        }
                        .onFailure {
                            prefs?.setError(
                                this@DownloadMapService,
                                "Ошибка при формировании конечного файла: ${it.message}"
                            )
                        }
                    tileManager.deleteTiles(downloadedTiles.data.values.flatten())
                }
            }
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
            val quality = intent.getIntExtra(ZOOM_ARG, 0)
            val mapList = intent.serializable(MAP_LIST_ARG) as? ArrayList<MapItem>
                ?: throw TypeCastException("Fail cast to MapItem")
            val objects = intent.serializable(OBJECTS_ARG) as? HashMap<MapObjectStyle, List<MapObject>>
                ?: throw TypeCastException("Fail cast to MapItem")
            val bound = intent.serializable(BOUND_ARG) as? BoundingBox
                ?: throw TypeCastException("fail cast to BoundingBox")
            val zoom = intent.getIntExtra(ZOOM_ARG, 0)
            downloadMap(mapList.toList(), bound, zoom, objects, quality)
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
        const val OBJECTS_ARG = "objects"
        const val DOWNLOAD_MAP_NOTIFICATION_ID = 788843
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.coroutineContext.cancelChildren()
    }
}