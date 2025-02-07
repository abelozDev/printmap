package ru.mapolib.printmap.gui.presentation.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.maplyb.printmap.R
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.mapolib.printmap.gui.utils.formatSize
import kotlin.math.roundToInt

@Composable
fun DownloadMapSettingsScreen(
    /*activity: Activity,*/
    boundingBox: BoundingBox,
    maps: List<MapItem>,
    zoom: Int
) {
    if (zoom !in 0..28) throw IllegalArgumentException("zoom must be in 0..28")
    val activity = checkNotNull(LocalActivity.current) { "Activity is null" }
    val viewModel = viewModel<SettingViewModel>(
        factory = SettingViewModel.create(
            activity = activity,
            zoom = zoom,
            boundingBox = boundingBox,
            maps = maps.map {
                it.copy(selected = zoom in it.zoomMin..it.zoomMax)
            }
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isOpen by remember {
        mutableStateOf(false)
    }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_header),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.tiles_count, state.tilesCount)
            )
            Spacer(Modifier.height(16.dp))
            state.fileSize?.let {
                Text(
                    text = stringResource(R.string.file_size, formatSize(it))
                )
                Spacer(Modifier.height(16.dp))
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                text = state.zoom.toString()
            )
            Slider(
                value = state.zoom.toFloat(),
                onValueChange = {
                    viewModel.sendEvent(SettingEvent.UpdateZoom(it.roundToInt()))
                },
                steps = 27,
                valueRange = 0f..28f,
                onValueChangeFinished = {
                    viewModel.sendEvent(SettingEvent.GetTilesCount)
                }
            )
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                text = state.quality.toString()
            )
            Slider(
                value = state.quality.toFloat(),
                onValueChange = {
                    viewModel.sendEvent(SettingEvent.UpdateQuality(it.roundToInt()))
                },
                steps = 100,
                valueRange = 0f..100f,
                onValueChangeFinished = {
                    //todo
                }
            )
            Row(
                modifier = Modifier
                    .clickable {
                        isOpen = !isOpen
                    }
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.selected_maps)
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            Spacer(Modifier.height(16.dp))
            AnimatedVisibility(
                visible = isOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    state.maps.forEach { map ->
                        Row {
                            Text(
                                modifier = Modifier
                                    .padding(8.dp),
                                text = map.name
                            )
                            Spacer(Modifier.weight(1f))
                            Checkbox(
                                checked = map.selected,
                                onCheckedChange = {
                                    viewModel.sendEvent(
                                        SettingEvent.UpdateMap(
                                            map.copy(
                                                selected = !map.selected
                                            )
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Text(text = stringResource(R.string.download))
                },
                enabled = state.maps.any { it.selected },
                onClick = {
                    viewModel.sendEvent(SettingEvent.StartDownloadingMap)
                }
            )
        }
    }
}