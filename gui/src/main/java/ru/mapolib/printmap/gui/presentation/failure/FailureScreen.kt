package ru.mapolib.printmap.gui.presentation.failure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.maplib.printmap.core.theme.colors.PrintMapButtonColors
import ru.maplib.printmap.core.theme.colors.PrintMapColorSchema
import ru.mapolib.printmap.gui.R

@Composable
internal fun FailureScreen(
    message: String,
    dismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            color = PrintMapColorSchema.colors.textColor,
            text = "Ошибка",
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Text(
            color = PrintMapColorSchema.colors.textColor,
            text = message
        )
        Spacer(Modifier.height(16.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = PrintMapButtonColors(),
            content = {
                Text(
                    color = PrintMapColorSchema.colors.textColor,
                    text = stringResource(R.string.printmap_cancel)
                )
            },
            onClick = {
                dismiss()
            }
        )
    }
}

@Composable
@Preview
private fun PreviewFailureScreen() {
    FailureScreen(
        "Ошибка получения файлов", {}
    )
}