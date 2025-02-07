package ru.mapolib.printmap.gui.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.mapolib.printmap.gui.api.DownloadMapManagerImpl
import ru.mapolib.printmap.gui.api.DownloadMapState

@Composable
fun MainScreen() {
    val state by DownloadMapManagerImpl.state.collectAsStateWithLifecycle()
    when(state) {
        DownloadMapState.Downloading -> {

        }
        is DownloadMapState.Failure -> TODO()
        is DownloadMapState.Finished -> TODO()
        DownloadMapState.Idle -> {

        }
        is DownloadMapState.PrepareDownloading -> TODO()
    }
}