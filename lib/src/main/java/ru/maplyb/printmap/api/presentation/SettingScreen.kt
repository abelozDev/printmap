package ru.maplyb.printmap.api.presentation

import android.app.Activity
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.maplyb.printmap.R
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.api.utils.ScreenState
import ru.maplyb.printmap.api.utils.formatSize
import kotlin.math.roundToInt

@Composable
fun DownloadMapSettingsScreen(
    activity: Activity,
    boundingBox: BoundingBox,
    maps: List<MapItem>,
    zoom: Int
) {
    if (zoom !in 0..28) throw IllegalArgumentException("zoom must be in 0..28")
    val mapPrint = remember { MapPrint.create(activity) }
    var localMaps by remember(maps) {
        mutableStateOf(
            maps.map {
                it.copy(selected = zoom in it.zoomMin..it.zoomMax)
            }
        )
    }
    var tilesSize by rememberSaveable {
        mutableIntStateOf(
            when (
                val result = mapPrint.getTilesCount(
                    boundingBox,
                    zoom
                )
            ) {
                is OperationResult.Error -> 0
                is OperationResult.Success -> result.data.count()
            }
        )
    }

    var screenState by rememberSaveable() { mutableStateOf<ScreenState>(ScreenState.Initial()) }

    var fileSize by rememberSaveable { mutableStateOf<Int?>(null) }
    var currentZoom by rememberSaveable { mutableIntStateOf(zoom) }
    var currentQuality by rememberSaveable { mutableFloatStateOf(100f) }

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
                text = stringResource(R.string.tiles_count, tilesSize)
            )
            Spacer(Modifier.height(16.dp))
            fileSize?.let {
                Text(
                    text = stringResource(R.string.file_size, formatSize(it))
                )
                Spacer(Modifier.height(16.dp))
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                text = currentZoom.toString()
            )
            Slider(
                value = currentZoom.toFloat(),
                onValueChange = {
                    currentZoom = it.roundToInt()
                },
                steps = 27,
                valueRange = 0f..28f,
                onValueChangeFinished = {
                    tilesSize = when (val result = mapPrint.getTilesCount(
                        boundingBox,
                        currentZoom
                    )
                    ) {
                        is OperationResult.Error -> {
                            screenState = ScreenState.Failure(result.message)
                            0
                        }
                        is OperationResult.Success -> result.data.count()
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                text = currentQuality.roundToInt().toString()
            )
            Slider(
                value = currentQuality,
                onValueChange = {
                    currentQuality = it
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
                    localMaps.forEach { map ->
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
                                    localMaps = localMaps.map {
                                        if (it == map) it.copy(selected = !it.selected) else it
                                    }
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
                enabled = localMaps.any { it.selected },
                onClick = {
                    //todo
                }
            )
        }
    }
}