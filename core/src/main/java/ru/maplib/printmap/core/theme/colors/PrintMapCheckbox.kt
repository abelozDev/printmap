package ru.maplib.printmap.core.theme.colors

import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable

@Composable
fun PrintMapCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = PrintMapCheckboxColors()
    )
}

@Composable
internal fun PrintMapCheckboxColors(): CheckboxColors {
    return CheckboxDefaults.colors().copy(
        checkedBoxColor = PrintMapColorSchema.colors.primary,
        checkedBorderColor = PrintMapColorSchema.colors.primary
    )
}
