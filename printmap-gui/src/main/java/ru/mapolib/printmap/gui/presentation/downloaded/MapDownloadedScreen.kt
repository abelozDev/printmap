package ru.mapolib.printmap.gui.presentation.downloaded

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import ru.mapolib.printmap.gui.halpers.share.sendImageAsFile

import ru.mapolib.printmap.gui.utils.createBitmaps
import ru.mapolib.printmap.gui.utils.formatSize

@Composable
fun MapDownloadedScreen() {
    val context = LocalContext.current
    val activity = checkNotNull(LocalActivity.current) {"activity must not by null"}
    val viewModel = viewModel<MapDownloadedViewModel>(
        factory = MapDownloadedViewModel.create(activity)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            if (state.image != null) {
                ImageItem(
                    image = state.image!!,
                    context = context,
                    delete = {
                        viewModel.sendEvent(MapDownloadedEvent.DeleteImage)
                    }
                )
            }
        }
    }
}
@Composable
private fun ImageItem(
    image: String,
    context: Context,
    delete: () -> Unit
) {
    val imageBitmap by remember(image) {
        mutableStateOf<Bitmap>(BitmapFactory.decodeFile(image))
    }
    val images = remember {
        createBitmaps(
            resultBitmap = imageBitmap,
            context = context
        )
    }
    var selectedImage by remember {
        mutableStateOf(images.first.bitmap)
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
                selected = selectedImage == images.first.bitmap,
                description = images.first.description,
                onClick = {
                    selectedImage = images.first.bitmap
                }
            )
            ImageType(
                selected = selectedImage == images.second.bitmap,
                description = images.second.description,
                onClick = {
                    selectedImage = images.second.bitmap
                }
            )
        }
        Text(
            text = "Размер файла: ${formatSize(imageBitmap.byteCount)}"
        )
        Spacer(Modifier.height(16.dp))
        AsyncImage(
            model = selectedImage,
            modifier = Modifier.fillMaxWidth(),
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