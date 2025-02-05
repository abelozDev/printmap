package ru.maplyb.printmap.impl.domain.local

import android.content.Context
import ru.maplyb.printmap.impl.data.local.PreferencesDataSourceImpl

internal typealias MapPath =  String
internal interface PreferencesDataSource {

    fun onUpdate(path: (MapPath?) -> Unit)
    fun saveMapPath(key: String, value: MapPath)
    fun getMapPath(key: String): MapPath?

    companion object {
        const val MAP_PREF_KEY = "MAP_PREF_KEY"
        const val MAP_PATH_KEY = "MAP_PATH_KEY"
        fun create(context: Context): PreferencesDataSource {
            return PreferencesDataSourceImpl(context)
        }
    }
}
internal interface PrefsListener {
    fun onMapReady(mapPath: MapPath?)
}