package ru.mapolib.printmap.gui.presentation.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.mapolib.printmap.gui.R

@Composable
fun ProgressScreen(
    progress: Int,
    progressMessage: String,
    cancelDownloading: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(ru.maplyb.printmap.R.string.progress, progress.toString())
        )
        if (progressMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = progressMessage
            )
        }

        CircularProgressIndicator(
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(
            content = {
                Text(
                    text = stringResource(R.string.printmap_cancel)
                )
            },
            onClick = {
                cancelDownloading()
            }
        )
    }
}