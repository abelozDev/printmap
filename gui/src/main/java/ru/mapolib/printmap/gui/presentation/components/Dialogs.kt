package ru.mapolib.printmap.gui.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.maplib.printmap.core.theme.colors.PrintMapButtonColors
import ru.maplib.printmap.core.theme.colors.PrintMapColorSchema

@Composable
internal fun ErrorDialog(
    title: String = "Ошибка",
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(
                color = PrintMapColorSchema.colors.textColor,
                text = title,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.25.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                color = PrintMapColorSchema.colors.textColor,
                text = message,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.15.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(
                colors = PrintMapButtonColors(),
                onClick =onDismiss,
                content = {
                    Text(
                        color = PrintMapColorSchema.colors.textColor,
                        text = "ОК"
                    )
                }
            )
        }
    }
}