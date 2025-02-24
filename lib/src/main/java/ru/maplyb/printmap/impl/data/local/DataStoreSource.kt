package ru.maplyb.printmap.impl.data.local

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource
import ru.maplyb.printmap.impl.domain.repo.DownloadTilesManager

internal object DataStoreSource: PreferencesDataSource {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "print_map_prefs")
    private val PROGRESS_KEY = intPreferencesKey("progress")
    private val PROGRESS_MESSAGE_KEY = stringPreferencesKey("progress_message")
    private val FILE_PATH_KEY = stringPreferencesKey("file_path")
    private val ERROR_KEY = stringPreferencesKey("error")
    private val IS_FINISHED_KEY = booleanPreferencesKey("is_finished")

    override fun getDownloadStatus(context: Context): Flow<DownloadStatus> {
        return context.dataStore.data.map { prefs ->
            DownloadStatus(
                progress = prefs[PROGRESS_KEY],
                progressMessage = prefs[PROGRESS_MESSAGE_KEY],
                filePath = prefs[FILE_PATH_KEY],
                isFinished = prefs[IS_FINISHED_KEY] ?: false,
                errorMessage = prefs[ERROR_KEY]
            )
        }
    }

    override suspend fun updateDownloadStatus(context: Context, status: DownloadStatus) {
        context.dataStore.edit { prefs ->
            if (status.progress == null) {
                prefs.remove(PROGRESS_KEY)
                prefs.remove(PROGRESS_MESSAGE_KEY)
            } else {
                prefs[PROGRESS_KEY] = status.progress
                prefs[PROGRESS_MESSAGE_KEY] = status.progressMessage ?: ""
            }
            if (status.filePath == null) {
                prefs.remove(FILE_PATH_KEY)
            } else {
                prefs[FILE_PATH_KEY] = status.filePath
            }
            prefs[IS_FINISHED_KEY] = status.isFinished
            if (status.errorMessage == null) {
                prefs.remove(ERROR_KEY)
            } else {
                prefs[ERROR_KEY] = status.errorMessage
            }
        }
    }

    override suspend fun setDownloaded(context: Context, path: String) {
        updateDownloadStatus(
            context = context,
            status = DownloadStatus(
                progress = 100,
                filePath = path,
                isFinished = true,
                errorMessage = null
            )
        )
    }

    override suspend fun setProgress(context: Context, progress: Int, message: String) {
        updateDownloadStatus(
            context = context,
            status = DownloadStatus(
                progress = progress,
                progressMessage = message,
                filePath = null,
                isFinished = false,
                errorMessage = null
            )
        )
    }

    override suspend fun setError(context: Context, message: String) {
        updateDownloadStatus(
            context = context,
            status = DownloadStatus(
                progress = null,
                filePath = null,
                isFinished = false,
                errorMessage = message
            )
        )
    }

    override suspend fun clear(context: Context) {
        updateDownloadStatus(
            context = context,
            status = DownloadStatus(
                progress = null,
                filePath = null,
                isFinished = false,
                errorMessage = null
            )
        )
    }

    override suspend fun remove(context: Context, path: String) {
        DownloadTilesManager.create(context).deleteTiles(listOf(path))
        updateDownloadStatus(
            context = context,
            status = DownloadStatus(
                progress = null,
                filePath = null,
                isFinished = false,
                errorMessage = null
            )
        )
    }
}