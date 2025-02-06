package ru.maplyb.printmap.impl.util

import android.app.Activity
import android.app.Application
import android.os.Bundle

class DestroyLifecycleCallback(
    private val onDestroy: (activity: Activity) -> Unit
): Application.ActivityLifecycleCallbacks {
    override fun onActivityDestroyed(p0: Activity) {
        onDestroy(p0)
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
    override fun onActivityStarted(p0: Activity) {}
    override fun onActivityResumed(p0: Activity) {}
    override fun onActivityPaused(p0: Activity) {}
    override fun onActivityStopped(p0: Activity) {}
    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

}