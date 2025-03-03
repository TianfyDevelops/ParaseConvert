package com.tianfy.parserconvert

import android.app.Application
import com.tianfy.convertlibrary.observer.ParserObserverManager
import com.tianfy.nettylib.NettyActivityLifecycleCallback

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NettyActivityLifecycleCallback.Instance.init(this, intArrayOf(6666)) { bytes ->
            ParserObserverManager.Instance.handle(bytes)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        NettyActivityLifecycleCallback.Instance.unInit(this)
    }
}