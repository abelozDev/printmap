package ru.maplyb.printmap.impl.data.local

import kotlinx.serialization.Serializable
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.model.TileParams

data class DownloadStatus(
    val progress: Int? = null, // Прогресс в процентах
    val progressMessage: String? = null,
    val filePath: DownloadedState? = null, // Путь к загруженному файлу (если скачивание завершено)
    val isFinished: Boolean = false, // Флаг завершения загрузки
    val errorMessage: String? = null // Ошибка (если произошла)
)

@Serializable
data class DownloadedState(
    val path: String,
    val boundingBox: BoundingBox,
    val layers: List<Layer>,
)
