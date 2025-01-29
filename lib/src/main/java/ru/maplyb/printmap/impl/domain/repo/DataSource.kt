package ru.maplyb.printmap.impl.domain.repo

import ru.maplyb.printmap.impl.data.local.LocalDataSource
import ru.maplyb.printmap.impl.data.network.RemoteDataSource

internal interface DataSource {
    suspend fun getTile(path: String, x: Int, y: Int, z: Int): ByteArray

    companion object Factory {
        fun createRemote(): DataSource = RemoteDataSource()
        fun createLocal(): DataSource = LocalDataSource()
    }
}