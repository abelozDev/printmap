package ru.mapolib.printmap.gui.presentation.downloaded

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.toColor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.maplib.palette.PalletScreen
import ru.maplib.printmap.core.theme.colors.PrintMapCheckbox
import ru.maplib.printmap.core.theme.colors.PrintMapColorSchema
import ru.maplib.printmap.core.theme.colors.PrintMapProgressIndicator
import ru.maplyb.printmap.api.model.DownloadedImage
import ru.maplyb.printmap.impl.domain.model.PageFormat
import ru.mapolib.printmap.gui.core.ui.PrintMapTextFieldColors
import ru.mapolib.printmap.gui.presentation.components.ErrorDialog
import ru.mapolib.printmap.gui.presentation.components.PrintmapPopup
import ru.mapolib.printmap.gui.presentation.downloaded.expandable.CoordinateGrid
import ru.mapolib.printmap.gui.presentation.downloaded.expandable.LayersExpandable
import ru.mapolib.printmap.gui.presentation.downloaded.expandable.MapObjectsSettingExpandable

import ru.mapolib.printmap.gui.utils.createBitmaps
import ru.mapolib.printmap.gui.utils.formatSize

@Composable
internal fun MapDownloadedScreen(
    viewModel: MapDownloadedViewModel,
    dispose: () -> Unit,
    onDeleteMap: (String) -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        onDispose {
            dispose()
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel
            .effect
            .onEach {
                when (it) {
                    is MapDownloadedEffect.DeleteMap -> onDeleteMap(it.path)
                }
            }
            .launchIn(this)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Image(
                modifier = Modifier.clickable {
                    viewModel.sendEvent(MapDownloadedEvent.DeleteImage)
                },
                imageVector = Icons.Default.Delete,
                contentDescription = null
            )
            Spacer(Modifier.width(16.dp))
            Image(
                modifier = Modifier.clickable {
                    viewModel.sendEvent(MapDownloadedEvent.Share)
                },
                imageVector = Icons.Default.Share,
                contentDescription = null
            )
        }
        Spacer(Modifier.height(8.dp))
        var name by remember {
            mutableStateOf(state.name)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                modifier = Modifier.weight(1f),
                value = name,
                colors = PrintMapTextFieldColors(),
                onValueChange = {
                    name = it
                },
                placeholder = {
                    Text(
                        text = "Название карты",
                        color = Color(0xffdfdede),
                    )
                },
            )
            IconButton(
                onClick = {
                    viewModel.sendEvent(MapDownloadedEvent.UpdateName(name = name))
                },
                content = {
                    Icon(
                        imageVector = Icons.Default.Done,
                        tint = Color.White,
                        contentDescription = null
                    )
                }

            )
        }

        Spacer(Modifier.height(16.dp))
        PrintmapPopup(
            name = "Формат экспорта",
            items = ExportTypes.entries,
            selected = state.exportType,
            update = {
                viewModel.sendEvent(MapDownloadedEvent.UpdateExportType(it))
            }
        )
        if (state.exportType is ExportTypes.PDF) {
            Spacer(Modifier.height(8.dp))
            FormatPopup(
                selectedExportType = state.exportType as ExportTypes.PDF,
                updateExportType = {
                    viewModel.sendEvent(MapDownloadedEvent.UpdateExportType(it))
                }
            )
            PrintmapPopup(
                name = "DPI",
                items = dpiVariants,
                selected = state.dpi,
                update = {
                    viewModel.sendEvent(MapDownloadedEvent.SelectDpi(it))
                }
            )
        }

        ImageItem(
            progress = state.updateMapProgress,
            image = state.bitmap,
            context = context,
        )
        CoordinateGrid(
            sliderInfo = state.coordinatesGridSliderInfo,
            selectedCoordinateGrid = state.coordinateGrid,
            checked = state.showCoordinateGrid,
            selectedColor = state.coordinateGridColor.color,
            onCheckedChanged = {
                viewModel.sendEvent(MapDownloadedEvent.ChangeCheckCoordinateGrid())
            },
            onSliderValueChangedFinished = {
                viewModel.sendEvent(MapDownloadedEvent.CoordinateGridSliderValueChangeFinished(it))
            },
            onColorChangeClicked = {
                viewModel.sendEvent(
                    MapDownloadedEvent.UpdateState(MapDownloadedState.ChangeCoordinatesGridColor())
                )
            },
            selectCoordinateGrid = {
                viewModel.sendEvent(MapDownloadedEvent.SelectCoordinateGrid(it))
            },
            selectedCoordSystem = state.coordinateSystem,
            selectCoordinateSystem = {
                viewModel.sendEvent(MapDownloadedEvent.SelectCoordinateSystem(it))
            }
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                color = PrintMapColorSchema.colors.textColor,
                text = state.orientation.description
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    viewModel.sendEvent(MapDownloadedEvent.ChangeOrientation)
                },
                content = {
                    Icon(
                        Icons.Default.Refresh,
                        null
                    )
                }
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                color = PrintMapColorSchema.colors.textColor,
                text = stringResource(ru.mapolib.printmap.gui.R.string.printmap_show_polyline)
            )
            Spacer(Modifier.weight(1f))
            PrintMapCheckbox(
                checked = state.showLayers,
                onCheckedChange = {
                    viewModel.sendEvent(MapDownloadedEvent.ShowPolylineChanged)
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        LayersExpandable(
            layers = state.layers,
            updateLayer = {
                viewModel.sendEvent(MapDownloadedEvent.UpdateLayer(it))
            },
            editLayer = {

            }
        )
        MapObjectsSettingExpandable(
            layersObjects = state.layers.flatMap { it.objects },
            changeObjectStyle = {
                viewModel.sendEvent(MapDownloadedEvent.UpdateMapObjectStyle(it))
            },
            changeStyleFinished = {
                viewModel.sendEvent(MapDownloadedEvent.UpdateLayers)
            },
            layerObjectsColors = state.layerObjectsColor,
            onColorChangeClicked = {
                viewModel.sendEvent(
                    MapDownloadedEvent.UpdateState(
                        MapDownloadedState.ChangeLayerColor(
                            it
                        )
                    )
                )
            },
        )
    }
    when (state.state) {
        is MapDownloadedState.Failure -> {
            ErrorDialog(
                message = (state.state as MapDownloadedState.Failure).message,
                onDismiss = {
                    viewModel.sendEvent(MapDownloadedEvent.UpdateState(MapDownloadedState.Initial))
                }
            )
        }

        MapDownloadedState.Initial -> Unit
        MapDownloadedState.Progress -> {
            Dialog(
                onDismissRequest = {
                },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PrintMapProgressIndicator()
                }
            }
        }

        is MapDownloadedState.ChangeLayerColor -> {
            val layerObject = (state.state as MapDownloadedState.ChangeLayerColor).layerObject
            val currentColor = state.layerObjectsColor[layerObject::class.simpleName] ?: android.graphics.Color.GRAY
            Dialog(
                onDismissRequest = {},
                content = {
                    PalletScreen(
                        initialColor = currentColor.toColor(),
                        dismiss = {
                            viewModel.sendEvent(
                                MapDownloadedEvent.UpdateColorToObjects(
                                    color = it,
                                    layerObject = layerObject
                                )
                            )
                        }
                    )
                }
            )
        }
        is MapDownloadedState.ChangeCoordinatesGridColor -> {
            Dialog(
                onDismissRequest = {

                },
                content = {
                    PalletScreen(
                        initialColor = state.coordinateGridColor.color.toColor(),
                        dismiss = { color ->
                            val selectedColor = if (color != null) CoordinateGridColor(color.toArgb()) else CoordinateGridColor.default
                            viewModel.sendEvent(
                                MapDownloadedEvent.UpdateCoordinateGridColor(
                                    color = selectedColor
                                )
                            )
                        }
                    )
                }
            )
        }
    }
}
@Composable
private fun ColumnScope.FormatPopup(
    selectedExportType: ExportTypes.PDF,
    updateExportType: (ExportTypes.PDF) -> Unit
) {
    var formatsVisibility by remember {
        mutableStateOf(false)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable {
            formatsVisibility = !formatsVisibility
        }
    ) {
        Text(
            color = PrintMapColorSchema.colors.textColor,
            text = "Формат листа. \nКоличество листов: ${selectedExportType.pagesSize}"
        )
        Spacer(Modifier.weight(1f))
        Text(
            color = PrintMapColorSchema.colors.textColor,
            text = selectedExportType.format.name
        )
        Icon(
            imageVector = if (formatsVisibility) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null
        )
    }
    Spacer(Modifier.height(8.dp))
    if (formatsVisibility) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = {
                    formatsVisibility = !formatsVisibility
                },
                properties = PopupProperties()
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.DarkGray)
                        .padding(8.dp)
                ) {
                    Column {
                        PageFormat.entries.forEach {
                            Text(
                                modifier = Modifier
                                    .clickable {
                                        updateExportType(
                                            selectedExportType.copy(
                                                format = it
                                            )
                                        )
                                        formatsVisibility = false
                                    }
                                    .padding(8.dp),
                                text = it.name,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageItem(
    progress: Boolean,
    image: Bitmap,
    context: Context,
) {
    val images = remember(image) {
        createBitmaps(
            resultBitmap = image,
            context = context
        ).toList()
    }
    val maxHeight = remember {
        images.maxHeight()
    }
    var selectedImage by remember {
        mutableIntStateOf(images.first().id)
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(vertical = 16.dp)
                .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImageType(
                selected = selectedImage == images[0].id,
                description = images[0].description,
                onClick = {
                    selectedImage = images[0].id
                }
            )
            ImageType(
                selected = selectedImage == images[1].id,
                description = images[1].description,
                onClick = {
                    selectedImage = images[1].id
                }
            )
        }
        Text(
            color = PrintMapColorSchema.colors.textColor,
            text = "Размер файла: ${formatSize(image.byteCount.toLong())}"
        )
        Spacer(Modifier.height(16.dp))
        Box(
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = images[selectedImage].bitmap,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .height(maxHeight.dp),
                contentDescription = null
            )
            if (progress) {
                PrintMapProgressIndicator()
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

fun List<DownloadedImage>.maxHeight(): Int {
    return this.maxOf { it.bitmap?.height ?: 0 }
}

@Composable
private fun RowScope.ImageType(
    selected: Boolean,
    description: String,
    onClick: () -> Unit
) {
    val textColor by remember(selected) {
        mutableStateOf(
            if (selected) Color.White else Color.Black
        )
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(if (selected) Color.Gray else Color.Transparent)
            .clickable {
                onClick()
            }
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            color = PrintMapColorSchema.colors.textColor,
            textAlign = TextAlign.Center,
            text = description,
        )
    }
}

@Preview
@Composable
private fun PreviewMapDownloadedScreen() {
    MapDownloadedScreen(
        viewModel = viewModel(),
        onDeleteMap = {},
        dispose = {}
    )
}