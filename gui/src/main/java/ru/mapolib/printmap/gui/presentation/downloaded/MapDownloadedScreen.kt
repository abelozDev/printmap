package ru.mapolib.printmap.gui.presentation.downloaded

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.DownloadedImage
import ru.mapolib.printmap.gui.halpers.share.sendImageAsFile

import ru.mapolib.printmap.gui.utils.createBitmaps
import ru.mapolib.printmap.gui.utils.formatSize

@Composable
internal fun MapDownloadedScreen(
    path: String,
    onDeleteMap: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel = viewModel<MapDownloadedViewModel>(
        factory = MapDownloadedViewModel.create(path)
    )
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
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        ImageItem(
            image = path,
            context = context,
            delete = {
                viewModel.onEffect(MapDownloadedEffect.DeleteMap(path))
            }
        )
    }
}

@Composable
private fun ImageItem(
    image: String,
    context: Context,
    delete: () -> Unit
) {
    LaunchedEffect(image) {
        println("imageBitmapPath = $image")
    }
    val imageBitmap by remember(image) {
        mutableStateOf<Bitmap>(BitmapFactory.decodeFile(image))
    }
    val images = remember {
        createBitmaps(
            resultBitmap = imageBitmap,
            context = context
        ).toList()
    }
    val maxHeight = remember {
        images.maxHeight()
    }
    var selectedImage by remember {
        mutableIntStateOf(images.first().id)
    }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
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
            text = "Размер файла: ${formatSize(imageBitmap.byteCount.toLong())}"
        )
        Spacer(Modifier.height(16.dp))
        AsyncImage(
            model = images[selectedImage].bitmap,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
                .height(maxHeight.dp)
                .onGloballyPositioned { layoutCoordinates ->
                    println("onGloballyPositioned ${layoutCoordinates.size}")
                },
            contentDescription = null
        )
        Spacer(Modifier.height(16.dp))
        Row {
            Image(
                modifier = Modifier.clickable {
                    delete()
                },
                imageVector = Icons.Default.Delete,
                contentDescription = null
            )
            Spacer(Modifier.width(16.dp))
            Image(
                modifier = Modifier.clickable {
                    sendImageAsFile(context, image)
                },
                imageVector = Icons.Default.Share,
                contentDescription = null
            )
        }
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