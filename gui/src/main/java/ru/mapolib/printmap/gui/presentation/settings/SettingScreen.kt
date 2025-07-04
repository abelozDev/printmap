package ru.mapolib.printmap.gui.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.maplib.printmap.core.theme.colors.PrintMapButtonColors
import ru.maplib.printmap.core.theme.colors.PrintMapColorSchema
import ru.maplib.printmap.core.theme.colors.PrintMapProgressIndicator
import ru.maplib.printmap.core.theme.colors.PrintMapSliderColor
import ru.maplyb.printmap.R
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.MapItem
import ru.mapolib.printmap.gui.presentation.settings.expand.MapsExpandable
import ru.mapolib.printmap.gui.utils.formatSize
import kotlin.math.roundToInt

@Composable
internal fun DownloadMapSettingsScreen(
    boundingBox: BoundingBox,
    maps: List<MapItem>,
    zoom: Int,
    objects: List<Layer>,
    startFormingAMap: (
        maps: List<MapItem>,
        boundingBox: BoundingBox,
        zoom: Int,
        quality: Int,
        objects: List<Layer>,
    ) -> Unit
) {
    val settingsViewModelStore = remember { ViewModelStore() }
    val settingsViewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore
                get() = settingsViewModelStore
        }
    }
    if (zoom !in 0..28) throw IllegalArgumentException("zoom must be in 0..28")
    val viewModel = ViewModelProvider(
        owner = settingsViewModelStoreOwner,
        factory = SettingViewModel.create(
            zoom = zoom,
            boundingBox = boundingBox,
            maps = maps.map {
                it.copy(selected = zoom in it.zoomMin..it.zoomMax)
            },
            objects = objects
        )
    )[SettingViewModel::class.java]
    DisposableEffect(Unit) {
        onDispose {
            settingsViewModelStore.clear()
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effect.onEach {
            when (it) {
                is SettingEffect.StartFormingAMap -> startFormingAMap(
                    it.maps,
                    it.boundingBox,
                    it.zoom,
                    it.quality,
                    it.objects
                )
            }
        }
            .launchIn(this)
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            color = PrintMapColorSchema.colors.textColor,
            text = stringResource(R.string.settings_header),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        if (state.progress) {
            PrintMapProgressIndicator(
                modifier = Modifier
                    .height(32.dp)
                    .padding(horizontal = 16.dp),

            )
        } else {
            if (state.state is SettingState.Error) {
                Text(
                    text = stringResource(R.string.file_size_error),
                    color = Color.Red
                )
            } else {
                Text(
                    color = PrintMapColorSchema.colors.textColor,
                    text = stringResource(R.string.tiles_count, state.tilesCount)
                )
                Spacer(Modifier.height(16.dp))
                state.fileSize?.let {
                    Text(
                        color = PrintMapColorSchema.colors.textColor,
                        text = stringResource(R.string.file_size, formatSize(it))
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                color = PrintMapColorSchema.colors.textColor,
                text = stringResource(ru.mapolib.printmap.gui.R.string.printmap_zoom)
            )
            Spacer(Modifier.weight(1f))
            Text(
                color = PrintMapColorSchema.colors.textColor,
                text = state.zoom.toString()
            )
        }
        Slider(
            value = state.zoom.toFloat(),
            onValueChange = {
                viewModel.sendEvent(SettingEvent.UpdateZoom(it.roundToInt()))
            },
            steps = 27,
            colors = PrintMapSliderColor(),
            valueRange = 0f..28f,
            onValueChangeFinished = {
                viewModel.sendEvent(SettingEvent.GetTilesCount)
            }
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                color = PrintMapColorSchema.colors.textColor,
                text = stringResource(ru.mapolib.printmap.gui.R.string.printmap_quality)
            )
            Spacer(Modifier.weight(1f))
            Text(
                color = PrintMapColorSchema.colors.textColor,
                text = state.quality.toString()
            )
        }

        Slider(
            value = state.quality.toFloat(),
            onValueChange = {
                viewModel.sendEvent(SettingEvent.UpdateQuality(it.roundToInt()))
            },
            colors = PrintMapSliderColor(),
            steps = 100,
            valueRange = 0f..100f,
            onValueChangeFinished = {
                //todo
            }
        )

        MapsExpandable(
            maps = state.maps,
            updateMap = {
                viewModel.sendEvent(SettingEvent.UpdateMap(it))
            },
            updateMaps = {
                viewModel.sendEvent(SettingEvent.UpdateMaps(it))
            }
        )

        Spacer(Modifier.height(16.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = PrintMapButtonColors(),
            content = {
                Text(text = stringResource(R.string.download))
            },
            enabled = state.buttonEnabled(),
            onClick = {
                viewModel.sendEvent(SettingEvent.StartDownloadingMap)
            }
        )
    }
}
//var isLayersOpen by remember {
//            mutableStateOf(false)
//        }
//        Row(
//            modifier = Modifier
//                .clickable {
//                    isLayersOpen = !isLayersOpen
//                }
//                .padding(vertical = 16.dp)
//        ) {
//            Text(
//                text = stringResource(R.string.selected_layers)
//            )
//            Spacer(Modifier.weight(1f))
//            Icon(
//                imageVector = if (isLayersOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
//                contentDescription = null
//            )
//        }

@Preview
@Composable
private fun PreviewDownloadMapSettingsScreen() {
    DownloadMapSettingsScreen(
        boundingBox = BoundingBox(
            0.0, 0.0, 0.0, 0.0,
        ),
        maps = emptyList(),
        zoom = 10,
        objects = emptyList(),
        { _, _, _, _, _ ->

        }
    )
}