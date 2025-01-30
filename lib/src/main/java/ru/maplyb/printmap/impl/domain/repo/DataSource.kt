package ru.maplyb.printmap.impl.domain.repo

import ru.maplyb.printmap.impl.data.local.LocalDataSource
import ru.maplyb.printmap.impl.data.network.RemoteDataSource
import ru.maplyb.printmap.impl.domain.model.TileSchema

internal interface DataSource {
    suspend fun getTile(path: String, x: Int, y: Int, z: Int, schema: TileSchema): ByteArray?
    suspend fun getSchema(path: String): TileSchema
    companion object Factory {
        fun createRemote(): DataSource = RemoteDataSource()
        fun createLocal(): DataSource = LocalDataSource()
    }
}