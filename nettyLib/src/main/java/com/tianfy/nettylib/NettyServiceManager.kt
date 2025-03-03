package com.tianfy.nettylib

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException

class NettyServiceManager private constructor() {
    companion object {
        val Instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            NettyServiceManager()
        }
    }

    private var listener: ((ByteArray) -> Unit)? = null

    private val iNettyServiceCallback = object : INettyServiceCallback.Stub() {

        override fun receiveBytes(bytes: ByteArray?) {
            bytes?.let {
                listener?.invoke(it)
            }
        }

    }

    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                isBound = true
                val iNettyInterface = INettyInterface.Stub.asInterface(service)
                iNettyInterface.addCallback(iNettyServiceCallback)
            } catch (e: RemoteException) {
                isBound = false
                e.printStackTrace()
            }

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    fun setReceiveBytesListener(listener: (ByteArray) -> Unit) {
        this.listener = listener
    }


    fun startService(context: Context, port: IntArray) {
        val intent = Intent(context, NettyService::class.java)
        intent.putExtra(NettyService.PORTS, port)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }


    fun stopService(context: Context) {
        if (isBound) {
            isBound = false
            context.unbindService(serviceConnection)
        }
    }
}