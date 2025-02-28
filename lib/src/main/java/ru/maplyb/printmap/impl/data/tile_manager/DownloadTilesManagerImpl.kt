package ru.maplyb.printmap.impl.data.tile_manager

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapType
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.impl.domain.model.TileParams
import ru.maplyb.printmap.impl.domain.repo.DataSource
import ru.maplyb.printmap.impl.domain.repo.DownloadTilesManager
import ru.maplyb.printmap.impl.files.FileUtil
import java.util.concurrent.atomic.AtomicInteger

internal class DownloadTilesManagerImpl(
    private val fileSaveUtil: FileUtil,
    private val localDataSource: DataSource,
    private val remoteDataSource: DataSource
) : DownloadTilesManager {


    override suspend fun getApproximateImageSize(
        maps: List<MapItem>,
        tiles: List<TileParams>,
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
                            localDataSource.getTilesApproximateSize(
                                map.type.path,
                                tiles,
                                map.alpha.toInt(),
                                map.mapType
                            )
                        }
                }.await()
                val remoteSize = async {
                    remote
                        .map {
                            it.copy(mapType = remoteDataSource.getSchema(it.type.path))
                        }
                        .sumOf { map ->
                            remoteDataSource.getTilesApproximateSize(
                                map.type.path,
                                tiles,
                                map.alpha.toInt(),
                                map.mapType
                            )
                        }
                }.await()
                localSize + remoteSize
            }
        }
    }

    override suspend fun deleteTiles(
        tiles: List<String?>
    ) {
        fileSaveUtil.deleteTiles(tiles.filterNotNull())
    }

    override suspend fun getTiles(
        maps: List<MapItem>,
        tiles: List<TileParams>,
        quality: Int,
        onProgress: suspend (Int) -> Unit
    ): OperationResult<Map<MapItem, List<String?>>> {
        val local = maps.filter { it.type is MapType.Offline }
        val remote = maps.filter { it.type is MapType.Online }
        val downloadedSize = AtomicInteger(0)
        val downloadedFiles = mutableListOf<String?>()
        return coroutineScope {
            withContext(Dispatchers.IO) {
                try {
                    val chunkSize = 10
                    val localTiles =  getTilesForMaps(
                        dataSource = localDataSource,
                        items = local,
                        tiles = tiles,
                        chunkSize = chunkSize,
                        quality = quality,
                        addToDownloaded = {
                            downloadedFiles.add(it)
                        },
                        onDownload = {
                            val progress = downloadedSize.addAndGet(it)
                            onProgress(progress)
                        }
                    )
                    val remoteTiles = getTilesForMaps(
                        dataSource = remoteDataSource,
                        items = remote,
                        tiles = tiles,
                        chunkSize = chunkSize,
                        quality = quality,
                        addToDownloaded = {
                            downloadedFiles.add(it)
                        },
                        onDownload = {
                            val progress = downloadedSize.addAndGet(it)
                            onProgress(progress)
                        }
                    )
                    val result = localTiles.await() + remoteTiles.await()
                    return@withContext if (result.flatMap { it.value }.filterNotNull().isEmpty()) {
                        OperationResult.Error("Не удалось получить ни одно файла")
                    } else {
                        OperationResult.Success(result)
                    }
                } catch (e: CancellationException) {
                    withContext(NonCancellable) {
                        fileSaveUtil.deleteTiles(downloadedFiles.filterNotNull())
                    }
                    throw e
                }
                catch (e: Exception) {
                    withContext(NonCancellable) {
                        fileSaveUtil.deleteTiles(downloadedFiles.filterNotNull())
                    }
                    return@withContext OperationResult.Error(e.message ?: "Ошибка при получении файлов.")
                }
            }
        }
    }

    private suspend fun getTilesForMaps(
        dataSource: DataSource,
        items: List<MapItem>,
        tiles: List<TileParams>,
        chunkSize: Int,
        quality: Int,
        addToDownloaded: (String?) -> Unit,
        onDownload: suspend (Int) -> Unit
    ): Deferred<Map<MapItem, List<String?>>> {
        return coroutineScope {
            async {
                items
                    .map {
                        it.copy(mapType = dataSource.getSchema(it.type.path))
                    }
                    .associateWith { map ->
                        tiles
                            .chunked(chunkSize)
                            .flatMap { tileChunk ->
                                val result = tileChunk.map { tile ->
                                    async {
                                        ensureActive()
                                        downloadAndSave(
                                            dataSource = dataSource,
                                            map = map,
                                            x = tile.x,
                                            y = tile.y,
                                            z = tile.z,
                                            quality = quality
                                        ).apply { addToDownloaded(this) }
                                    }
                                }.awaitAll()
                                onDownload(tileChunk.size)
                                result
                            }
                    }
            }
        }
    }

    private suspend fun downloadAndSave(
        dataSource: DataSource,
        map: MapItem,
        x: Int,
        y: Int,
        z: Int,
        quality: Int
    ): String? {
        return try {
            dataSource.getTile(map.type.path, x, y, z, map.mapType)
                ?.let {
                    fileSaveUtil.saveTileToPNG(
                        it,
                        generateName(map.type.path, x, y, z),
                        map.alpha.toInt(),
                        quality
                    )
                }
        } catch (e: Exception) {
            null
        }
    }


    private fun generateName(path: String, x: Int, y: Int, z: Int): String {
        val formattedPath = path.replace("https:/", "").replace("/", "_")
        return "${formattedPath}_x=${x}_y=${y}_z=$z.jpg"
    }
}