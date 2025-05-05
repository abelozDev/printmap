package ru.maplyb.printmap.impl.data.network

import ru.maplyb.printmap.impl.domain.model.TileParams
import ru.maplyb.printmap.impl.domain.model.TileSchema
import ru.maplyb.printmap.impl.domain.repo.DataSource
import ru.maplyb.printmap.impl.util.getTileSize

internal class RemoteDataSource: DataSource {

    private val api = Networking().httpClient()

    override suspend fun getTilesApproximateSize(
        path: String,
        tiles: List<TileParams>,
        alpha: Int,
        schema: TileSchema
    ): Long {
        if (tiles.isEmpty()) return 0L
        val tileSize = tiles[0].let {
            getTile(path, it.x, it.y, it.z, schema).getTileSize(alpha)
        }
        return tiles.count() * tileSize
    }
    override suspend fun getTile(path: String, x: Int, y: Int, z: Int, schema: TileSchema): ByteArray? {
        return try {
            var currentPath = path
            val coordinatesMap = mapOf(
                "x" to x,
                "y" to y,
                "z" to z
            )
            coordinatesMap.forEach { (key, value) ->
                currentPath = currentPath.replace("{$key}", "$value")
            }
            println("currentPath: $currentPath")
            val tile = api.getMap(currentPath)
            return tile.bytes()
        } catch (e: Exception) {
            println("FATAL EXCEPTION = ${e.stackTrace.joinToString("\n")}")
            byteArrayOf()
        }
    }
    override suspend fun getSchema(path: String): TileSchema = TileSchema.GOOGLE
}