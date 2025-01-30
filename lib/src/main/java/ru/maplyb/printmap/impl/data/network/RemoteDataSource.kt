package ru.maplyb.printmap.impl.data.network

import ru.maplyb.printmap.impl.domain.model.TileSchema
import ru.maplyb.printmap.impl.domain.repo.DataSource

internal class RemoteDataSource: DataSource {

    private val api = Networking().httpClient()

    override suspend fun getTile(path: String, x: Int, y: Int, z: Int, schema: TileSchema): ByteArray? {
        return try {
            val tile = api.getMap(path, x, y, z)
            return tile.bytes()
        } catch (e: Exception) {
            println("FATAL EXCEPTION = ${e.message}")
            byteArrayOf()
        }
    }
    override suspend fun getSchema(path: String): TileSchema = TileSchema.GOOGLE
}