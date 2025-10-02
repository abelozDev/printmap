package ru.mapolib.printmap.gui.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.maplib.printmap.core.theme.colors.PrintMapColorSchema
import ru.mapolib.printmap.gui.R

@Composable
fun RepostSection(
    reportFilePath: String?,
    onReportClick: () -> Unit,
    onDeleteReportClick: () -> Unit,
    onShareRepostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasFile = reportFilePath != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onReportClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_report),
            tint = PrintMapColorSchema.colors.textColor,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
        )
        Text(
            text = if (hasFile) "Отчет" else "Сформировать отчет",
            color = PrintMapColorSchema.colors.textColor,
            modifier = Modifier.weight(1f)
        )
        if (hasFile) {
            Icon(
                modifier = Modifier.clickable(onClick = onDeleteReportClick),
                tint = PrintMapColorSchema.colors.textColor,
                imageVector = Icons.Default.Delete,
                contentDescription = null
            )
            Spacer(Modifier.width(16.dp))
            Icon(
                modifier = Modifier.clickable(onClick = onShareRepostClick),
                tint = PrintMapColorSchema.colors.textColor,
                imageVector = Icons.Default.Share,
                contentDescription = null
            )
        }
    }
}