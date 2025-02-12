package ru.maplyb.printmap.impl.domain.local

import android.content.Context
import kotlinx.coroutines.flow.Flow
import ru.maplyb.printmap.impl.data.local.DataStoreSource
import ru.maplyb.printmap.impl.data.local.DownloadStatus

internal typealias MapPath =  String
interface PreferencesDataSource {

    suspend fun updateDownloadStatus(context: Context, status: DownloadStatus)
    suspend fun setDownloaded(context: Context, path: String)
    suspend fun setProgress(context: Context, progress: Int)
    suspend fun setError(context: Context, message: String)
    suspend fun remove(context: Context, path: String)
    suspend fun clear(context: Context)
    fun getDownloadStatus(context: Context): Flow<DownloadStatus>

    companion object {
        fun create(): PreferencesDataSource {
            return DataStoreSource
        }
    }
}
internal interface PrefsListener {
    fun onMapReady(mapPath: MapPath?)
}