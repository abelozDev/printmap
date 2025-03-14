package ru.mapolib.printmap.gui.presentation.downloaded

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.DownloadedImage
import ru.maplyb.printmap.api.model.Layer
import ru.mapolib.printmap.gui.R
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
        LaunchedEffect(name) {
            delay(300)
            viewModel.sendEvent(MapDownloadedEvent.UpdateName(name = name))
        }
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = name,
            onValueChange = {
                name = it
            },
            label = {
                Text(
                    text = stringResource(R.string.map_name)
                )
            }
        )
        ImageItem(
            progress = state.updateMapProgress,
            image = state.bitmap,
            context = context,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
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
                text = stringResource(ru.mapolib.printmap.gui.R.string.show_polyline)
            )
            Spacer(Modifier.weight(1f))
            Checkbox(
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
            }
        )
        MapObjectsSettingExpandable(
            layersObjects = state.layers.flatMap { it.objects },
            changeObjectStyle = {
                viewModel.sendEvent(MapDownloadedEvent.UpdateMapObjectStyle(it))
            },
            changeStyleFinished = {
                viewModel.sendEvent(MapDownloadedEvent.UpdateLayers)
            }
        )
    }
    if (state.state is MapDownloadedState.Progress) {
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
                CircularProgressIndicator()
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
                CircularProgressIndicator()
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
            color = textColor,
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