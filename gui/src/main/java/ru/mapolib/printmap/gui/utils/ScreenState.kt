package ru.mapolib.printmap.gui.utils

import java.io.Serializable

internal sealed interface ScreenState: Serializable {
    class Initial: ScreenState
    data class Failure(val message: String): ScreenState
}