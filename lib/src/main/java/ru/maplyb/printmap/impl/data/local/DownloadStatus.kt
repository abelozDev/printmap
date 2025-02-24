package ru.maplyb.printmap.impl.data.local

data class DownloadStatus(
    val progress: Int? = null, // Прогресс в процентах
    val progressMessage: String? = null,
    val filePath: String? = null, // Путь к загруженному файлу (если скачивание завершено)
    val isFinished: Boolean = false, // Флаг завершения загрузки
    val errorMessage: String? = null // Ошибка (если произошла)
)
