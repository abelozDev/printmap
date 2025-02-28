package ru.maplyb.printmap.impl.domain.repo

import android.content.Context
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.impl.data.tile_manager.DownloadTilesManagerImpl
import ru.maplyb.printmap.impl.domain.model.TileParams
import ru.maplyb.printmap.impl.files.FileUtil

internal interface DownloadTilesManager {

    /**Скичивает тайлы
     * @return Map где key - карта, value - пути к файлам в локальной памяти*/
    suspend fun getTiles(
        maps: List<MapItem>,
        tiles: List<TileParams>,
        quality: Int,
        onProgress: suspend (Int) -> Unit
    ): OperationResult<Map<MapItem, List<String?>>>

    suspend fun getApproximateImageSize(maps: List<MapItem>, tiles: List<TileParams>): Long
    suspend fun deleteTiles(
        tiles: List<String?>
    )
    companion object Factory {
        fun create(context: Context): DownloadTilesManager {
            val local = DataSource.createLocal()
            val remote = DataSource.createRemote()
            val fileSaveUtil = FileUtil(context)
            return DownloadTilesManagerImpl(fileSaveUtil, local, remote)
        }
    }
}