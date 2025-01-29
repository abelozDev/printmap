package ru.maplyb.printmap.impl.data.network

import ru.maplyb.printmap.impl.domain.repo.DataSource

internal class RemoteDataSource: DataSource {

    private val api = Networking().httpClient()

    override suspend fun getTile(path: String, x: Int, y: Int, z: Int): ByteArray {
        return try {
            val tile = api.getMap(path, x, y, z)
            return tile.bytes()
        } catch (e: Exception) {
            println("FATAL EXCEPTION = ${e.message}")
            byteArrayOf()
        }
    }
}