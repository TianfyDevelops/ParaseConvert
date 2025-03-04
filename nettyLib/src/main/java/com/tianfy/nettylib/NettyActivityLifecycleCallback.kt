package com.tianfy.nettylib

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle

class NettyActivityLifecycleCallback private constructor() : ActivityLifecycleCallbacks {
    companion object {
        val Instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            NettyActivityLifecycleCallback()
        }
    }

    private var portArray: IntArray? = null

    fun init(app: Application, portArray: IntArray,listener: (ByteArray) -> Unit) {
        this.portArray = portArray
        NettyServiceManager.Instance.setReceiveBytesListener(listener)
        app.registerActivityLifecycleCallbacks(this)
    }

    fun unInit(app: Application) {
        app.unregisterActivityLifecycleCallbacks(this)
    }


    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        portArray?.let {
            NettyServiceManager.Instance.startService(activity, it)
        }
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        NettyServiceManager.Instance.stopService(activity)
    }
}