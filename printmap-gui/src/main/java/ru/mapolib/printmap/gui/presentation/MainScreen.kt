package ru.mapolib.printmap.gui.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.mapolib.printmap.gui.api.DownloadMapManagerImpl
import ru.mapolib.printmap.gui.api.DownloadMapState
import ru.mapolib.printmap.gui.presentation.downloaded.MapDownloadedScreen
import ru.mapolib.printmap.gui.presentation.settings.DownloadMapSettingsScreen

@Composable
fun MainScreen() {
    val downloadManager = remember { DownloadMapManagerImpl }
    val state by downloadManager.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    if (state.isOpen) {
        Dialog(
            onDismissRequest = {
                downloadManager.hide()
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(
                        vertical = 16.dp,
                    )
            ) {
                when (state) {
                    is DownloadMapState.Downloading -> {
                        ProgressScreen(
                            progress = (state as DownloadMapState.Downloading).progress,
                            cancelDownloading = {
                                downloadManager.cancelDownloading()
                            }
                        )
                    }

                    is DownloadMapState.Failure -> TODO()
                    is DownloadMapState.Finished -> {
                        println("image path: ${(state as DownloadMapState.Finished).path}")
                        MapDownloadedScreen(
                            path = (state as DownloadMapState.Finished).path,
                            onDeleteMap = {
                                scope.launch {
                                    downloadManager.deleteMap(it)
                                }
                            }
                        )
                    }

                    DownloadMapState.Idle -> TODO()
                    is DownloadMapState.PrepareDownloading -> {
                        DownloadMapSettingsScreen(
                            boundingBox = (state as DownloadMapState.PrepareDownloading).boundingBox,
                            maps = (state as DownloadMapState.PrepareDownloading).maps,
                            zoom = (state as DownloadMapState.PrepareDownloading).zoom,
                            startFormingAMap = { maps, boundingBox, zoom, quality ->
                                scope.launch {
                                    downloadManager.startFormingAMap(maps, boundingBox, zoom, quality)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}