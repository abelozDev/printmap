package ru.maplyb.printmap.impl.data.local

import ru.maplyb.printmap.impl.domain.repo.DataSource

class LocalDataSource: DataSource {
    override suspend fun getTile(path: String, x: Int, y: Int, z: Int): ByteArray {
        return ByteArray(0)
    }
}