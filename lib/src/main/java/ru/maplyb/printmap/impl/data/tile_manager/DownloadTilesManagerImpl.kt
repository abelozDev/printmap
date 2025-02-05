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
import java.util.concurrent.atomic.AtomicInteger

internal class DownloadTilesManagerImpl(
    private val fileSaveUtil: FileSaveUtil,
    private val localDataSource: DataSource,
    private val remoteDataSource: DataSource
) : DownloadTilesManager {


    override suspend fun getApproximateImageSize(
        maps: List<MapItem>,
        tiles: List<TileParams>
    ): Long {
        val local = maps.filter { it.type is MapType.Offline }
        val remote = maps.filter { it.type is MapType.Online }

        return coroutineScope {
            withContext(Dispatchers.IO) {
                val localSize = async {
                    local
                        .map {
                            it.copy(mapType = localDataSource.getSchema(it.type.path))
                        }
                        .sumOf { map ->
                            localDataSource.getTilesApproximateSize(map.type.path, tiles, map.alpha.toInt(), map.mapType)
                        }
                }.await()
                val remoteSize =  async {
                    remote
                        .map {
                            it.copy(mapType = remoteDataSource.getSchema(it.type.path))
                        }
                        .sumOf { map ->
                            remoteDataSource.getTilesApproximateSize(map.type.path, tiles, map.alpha.toInt(), map.mapType)
                        }
                }.await()
                localSize + remoteSize
            }
        }
    }

    override suspend fun getTiles(
        maps: List<MapItem>,
        tiles: List<TileParams>,
        onProgress: (Int) -> Unit
    ): Map<MapItem, List<String?>> {
        val local = maps.filter { it.type is MapType.Offline }
        val remote = maps.filter { it.type is MapType.Online }
        val downloadedSize = AtomicInteger(0)
        return coroutineScope {
            withContext(Dispatchers.IO) {
                val chunkSize = 10
                val localTiles = async {
                    local
                        .map {
                            it.copy(mapType = localDataSource.getSchema(it.type.path))
                        }
                        .associateWith { map ->
                            tiles
                                .chunked(10)
                                .flatMap { tileChunk ->
                                    val result = tileChunk.map { tile ->
                                        async {
                                            try {
                                                localDataSource.getTile(map.type.path, tile.x, tile.y, tile.z, map.mapType)
                                                    ?.let {
                                                        fileSaveUtil.saveTileToPNG(
                                                            it,
                                                            generateName(
                                                                map.type.path,
                                                                tile.x,
                                                                tile.y,
                                                                tile.z
                                                            ),
                                                            map.alpha.toInt()
                                                        )
                                                    }
                                            } catch (e: Exception) { null }
                                        }
                                    }.awaitAll()
                                    val progress = downloadedSize.addAndGet(10)
                                    onProgress(progress)
                                    result
                                }
                        }
                }
                val remoteTiles = async {
                    remote
                        .map {
                            it.copy(mapType = remoteDataSource.getSchema(it.type.path))
                        }
                        .associateWith { map ->
                            tiles
                                .chunked(chunkSize)
                                .flatMap { tileChunk ->
                                    val result = tileChunk.map { tile ->
                                        async {
                                            try {
                                                remoteDataSource.getTile(
                                                    map.type.path,
                                                    tile.x,
                                                    tile.y,
                                                    tile.z,
                                                    map.mapType
                                                )?.let {
                                                    fileSaveUtil.saveTileToPNG(
                                                        it,
                                                        generateName(
                                                            map.type.path,
                                                            tile.x,
                                                            tile.y,
                                                            tile.z
                                                        ),
                                                        map.alpha.toInt()
                                                    )
                                                }
                                            } catch (e: Exception) { null }
                                        }
                                    }.awaitAll()
                                    val progress = downloadedSize.addAndGet(10)
                                    onProgress(progress)
                                    result
                                }
                        }
                }
                return@withContext localTiles.await() + remoteTiles.await()
            }
        }
    }


    private fun generateName(path: String, x: Int, y: Int, z: Int): String {
        val formattedPath = path.replace("https:/", "").replace("/", "_")
        return "${formattedPath}_x=${x}_y=${y}_z=$z.jpg"
    }
}