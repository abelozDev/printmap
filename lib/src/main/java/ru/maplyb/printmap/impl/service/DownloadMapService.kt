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
import ru.maplyb.printmap.api.model.FormingMapArgs
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.impl.data.local.DownloadedState
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource
import ru.maplyb.printmap.impl.domain.repo.DownloadTilesManager
import ru.maplyb.printmap.impl.files.FileUtil
import ru.maplyb.printmap.impl.util.DrawInBitmap
import ru.maplyb.printmap.impl.util.GeoCalculator
import ru.maplyb.printmap.impl.util.MergeTiles
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
        args: FormingMapArgs
    ) {
        coroutineScope.launch {
            val tiles =
                GeoCalculator.calculateTotalTilesCount(args.bound, args.zoom).successDataOrNull()
                    ?: return@launch
            val fullSize = args.mapList.size * tiles.size
            startForeground(
                DOWNLOAD_MAP_NOTIFICATION_ID,
                createNotification(
                    progressText = "Начало загрузки",
                    maxProgress = fullSize,
                    progress = 0
                )
            )
            val tileManager = DownloadTilesManager.create(this@DownloadMapService)

            val downloadedTiles = tileManager.getTiles(args.mapList, tiles, args.quality) {
                updateNotification("Загрузка тайлов", fullSize, it)
                prefs?.setProgress(
                    context = this@DownloadMapService,
                    progress = (it.toFloat() / fullSize * 100).toInt(),
                    message = "Скачивание карт"
                )
            }
            when (downloadedTiles) {
                is OperationResult.Error -> {
                    prefs?.setError(this@DownloadMapService, downloadedTiles.message)
                }

                is OperationResult.Success -> {
                    prefs?.setProgress(
                        context = this@DownloadMapService,
                        progress = 100,
                        message = "Формирование файла"
                    )
                    MergeTiles()
                        .mergeTilesSortedByCoordinates(
                            args.author,
                            boundingBox = args.bound,
                            tiles = tiles,
                            downloadedTiles.data,
                            args.zoom
                        ).onSuccess { bitmap ->
                    prefs?.setProgress(
                        context = this@DownloadMapService,
                        progress = 100,
                        message = "Сохранение файла"
                    )
                    FileUtil(this@DownloadMapService).saveBitmapToExternalStorage(
                        bitmap = bitmap!!,
                        fileName = "${System.currentTimeMillis()}"
                    )
                        ?.let {
                            prefs?.setDownloaded(
                                context = this@DownloadMapService,
                                path = DownloadedState(
                                    path = it,
                                    layers = args.layers,
                                    boundingBox = args.bound
                                )
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
        val args = intent.serializable(FORMING_MAP_ARGS) as? FormingMapArgs
            ?: throw NullPointerException("Args is null")
        downloadMap(args)
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
    const val AUTHOR_ARG = "author"
    const val FORMING_MAP_ARGS = "FORMING_MAP_ARGS"
    const val DOWNLOAD_MAP_NOTIFICATION_ID = 788843
}

override fun onDestroy() {
    super.onDestroy()
    coroutineScope.coroutineContext.cancelChildren()
}
}