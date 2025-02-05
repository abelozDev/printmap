package ru.maplyb.printmap.impl.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import ru.maplyb.printmap.impl.domain.local.MapPath
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource.Companion.MAP_PATH_KEY
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource.Companion.MAP_PREF_KEY
import ru.maplyb.printmap.impl.domain.local.PrefsListener

internal object PreferencesDataSourceImpl: PreferencesDataSource {

    private var prefsListener: PrefsListener? = null

    fun init(context: Context) {
        if (preferences == null) {
            preferences = context.getSharedPreferences(MAP_PREF_KEY, Context.MODE_PRIVATE)
        }
    }
    override fun onUpdate(path: (MapPath?) -> Unit) {
        prefsListener = object : PrefsListener {
            override fun onMapReady(mapPath: MapPath?) {
                path(mapPath)
            }
        }
        val mapPath = getMapPath(MAP_PATH_KEY)
        prefsListener?.onMapReady(mapPath)
    }
    private var preferences: SharedPreferences? = null


    override fun saveMapPath(key: String, value: MapPath) {
        preferences?.edit {
            putString(key, value)
        }
        prefsListener?.onMapReady(value)
    }

    override fun getMapPath(key: String): MapPath? {
        return preferences?.getString(key, null)
    }

    override fun removeExistedMap() {
        preferences?.edit {
            remove(MAP_PATH_KEY)
        }
        prefsListener?.onMapReady(null)
    }
}