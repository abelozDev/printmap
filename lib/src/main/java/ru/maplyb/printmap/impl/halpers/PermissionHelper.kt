package ru.maplyb.printmap.impl.halpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

internal class PermissionHelper(private val context: Context) {
    //Manifest.permission.POST_NOTIFICATIONS
    fun checkPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkPostNotificationsPermission(): Boolean {
        return checkPermission(Manifest.permission.POST_NOTIFICATIONS)
    }
}