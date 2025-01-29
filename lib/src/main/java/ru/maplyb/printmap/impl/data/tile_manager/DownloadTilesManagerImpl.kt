package ru.maplyb.printmap.impl.data.tile_manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapType
import ru.maplyb.printmap.impl.domain.model.TileParams
import ru.maplyb.printmap.impl.domain.repo.DataSource
import ru.maplyb.printmap.impl.domain.repo.DownloadTilesManager
import ru.maplyb.printmap.impl.util.FileSaveUtil

internal class DownloadTilesManagerImpl(
    private val fileSaveUtil: FileSaveUtil,
    private val localDataSource: DataSource,
    private val remoteDataSource: DataSource
) : DownloadTilesManager {

    override suspend fun getTiles(maps: List<MapItem>, tiles: List<TileParams>): List<String?> {
        val local = maps.filterIsInstance<MapType.Offline>()
        val remote = maps.filter { it.type is MapType.Online }
        return coroutineScope {
            withContext(Dispatchers.IO) {
                val chunkSize = 10
                remote.flatMap { map ->
                    tiles
                        .chunked(chunkSize)
                        .flatMap { tileChunk ->
                            tileChunk.map { tile ->
                                async {
                                    try {
                                        val tileBytes = remoteDataSource.getTile(
                                            map.type.path,
                                            tile.x,
                                            tile.y,
                                            tile.z
                                        )
                                        fileSaveUtil.saveTileToPNG(
                                            tileBytes,
                                            generateName(map.type.path, tile.x, tile.y, tile.z),
                                            map.alpha.toInt()
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }.awaitAll()
                        }
                }
            }
        }
    }


    private fun generateName(path: String, x: Int, y: Int, z: Int): String {
        val formattedPath = path.replace("https:/", "").replace("/", "_")
        return "${formattedPath}_x=${x}_y=${y}_z=$z.jpg"
    }
}