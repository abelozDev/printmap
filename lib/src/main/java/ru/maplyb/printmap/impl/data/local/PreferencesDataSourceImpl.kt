package ru.maplyb.printmap.impl.data.local

import android.content.Context
import ru.maplyb.printmap.impl.domain.local.MapPath
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource.Companion.MAP_PATH_KEY
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource.Companion.MAP_PREF_KEY
import ru.maplyb.printmap.impl.domain.local.PrefsListener

internal class PreferencesDataSourceImpl(context: Context): PreferencesDataSource {

    private var prefsListener: PrefsListener? = null

    override fun onUpdate(path: (MapPath?) -> Unit) {
        prefsListener = object : PrefsListener {
            override fun onMapReady(mapPath: MapPath?) {
                path(mapPath)
            }
        }
        val mapPath = getMapPath(MAP_PATH_KEY)
        prefsListener?.onMapReady(mapPath)
    }
    private val preferences = context.getSharedPreferences(MAP_PREF_KEY, Context.MODE_PRIVATE)

    override fun saveMapPath(key: String, value: MapPath) {
        preferences.edit().putString(key, value).apply()
        prefsListener?.onMapReady(value)
    }

    override fun getMapPath(key: String): MapPath? {
        return preferences.getString(key, null)
    }
}