package ru.mapolib.printmap.gui.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.maplib.printmap.core.theme.colors.ColorSchema
import ru.maplib.printmap.core.theme.colors.LocalColorScheme
import ru.maplib.printmap.core.theme.colors.PrintMapColorSchema
import ru.maplib.printmap.core.theme.colors.darkColorSchema
import ru.maplib.printmap.core.theme.colors.lightColorSchema
import ru.maplyb.printmap.api.model.FormingMapArgs
import ru.mapolib.printmap.gui.api.DownloadMapManagerImpl
import ru.mapolib.printmap.gui.api.DownloadMapState
import ru.mapolib.printmap.gui.presentation.downloaded.MapDownloadedScreen
import ru.mapolib.printmap.gui.presentation.downloaded.MapDownloadedViewModel
import ru.mapolib.printmap.gui.presentation.failure.FailureScreen
import ru.mapolib.printmap.gui.presentation.progress.ProgressScreen
import ru.mapolib.printmap.gui.presentation.settings.DownloadMapSettingsScreen

@Composable
fun MainScreen() {
    val downloadManager = remember { DownloadMapManagerImpl }
    val state by downloadManager.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = if(!isSystemInDarkTheme()) lightColorSchema() else darkColorSchema()
    if (state.isOpen) {
        CompositionLocalProvider(LocalColorScheme provides colors) {
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
                        .background(PrintMapColorSchema.colors.backgroundColor)
                        .padding(
                            vertical = 16.dp,
                        )
                ) {
                    when (state) {
                        is DownloadMapState.Downloading -> {
                            ProgressScreen(
                                progress = (state as DownloadMapState.Downloading).progress,
                                progressMessage = (state as DownloadMapState.Downloading).message,
                                cancelDownloading = {
                                    downloadManager.cancelDownloading()
                                }
                            )
                        }

                        is DownloadMapState.Failure -> {
                            FailureScreen(
                                message = (state as DownloadMapState.Failure).message,
                                dismiss = {
                                    downloadManager.dismissFailure(context)
                                }
                            )
                        }

                        is DownloadMapState.Finished -> {
                            val downloadedViewModelStore = remember { ViewModelStore() }
                            val downloadedViewModelStoreOwner = remember {
                                object : ViewModelStoreOwner {
                                    override val viewModelStore: ViewModelStore
                                        get() = downloadedViewModelStore
                                }
                            }
                            val viewModel = ViewModelProvider(
                                owner = downloadedViewModelStoreOwner,
                                factory = MapDownloadedViewModel.create(
                                    path = (state as DownloadMapState.Finished).path,
                                    boundingBox = (state as DownloadMapState.Finished).boundingBox,
                                    layers = (state as DownloadMapState.Finished).layers,
                                    context = context,
                                    author = (state as DownloadMapState.Finished).author,
                                    appName = (state as DownloadMapState.Finished).appName
                                )
                            )[MapDownloadedViewModel::class.java]

                            MapDownloadedScreen(
                                viewModel = viewModel,
                                dispose = {
                                    downloadedViewModelStore.clear()
                                },
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
                                objects = (state as DownloadMapState.PrepareDownloading).objects,
                                startFormingAMap = { maps, boundingBox, zoom, quality, objects ->
                                    scope.launch {
                                        downloadManager.startFormingAMap(
                                            args = FormingMapArgs(
                                                mapList = maps,
                                                bound = boundingBox,
                                                layers = objects,
                                                zoom = zoom,
                                                quality = quality,
                                                author = (state as DownloadMapState.PrepareDownloading).author ?: "",
                                                appName = (state as DownloadMapState.PrepareDownloading).appName
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
