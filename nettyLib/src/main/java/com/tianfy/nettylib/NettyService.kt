package com.tianfy.nettylib

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioSocketChannel


class NettyService : Service() {

    companion object {
        const val PORTS = "portArray"
    }


    private val remoteCallbackList = RemoteCallbackList<INettyServiceCallback>()

    private val binder = object : INettyInterface.Stub() {
        override fun addCallback(callback: INettyServiceCallback?) {
            remoteCallbackList.register(callback)
        }

        override fun removeCallback(callback: INettyServiceCallback?) {
            remoteCallbackList.unregister(callback)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        initNetty(intent)
        return binder
    }

    private fun initNetty(intent: Intent?) {
        val intArrayExtra = intent?.getIntArrayExtra(PORTS)
        val workerGroup: EventLoopGroup = NioEventLoopGroup()
        val b = Bootstrap() // (1)
        b.group(workerGroup) // (2)
        b.channel(NioSocketChannel::class.java) // (3)
        b.option(ChannelOption.SO_BROADCAST, true) // (4)
        b.handler(object : ChannelInitializer<NioDatagramChannel>() {
            override fun initChannel(ch: NioDatagramChannel) {
                ch.pipeline().addLast(NettyLocalChannelHandler(remoteCallbackList))
            }

        })
        intArrayExtra?.distinct()?.forEach { port ->
            val channelFuture = b.bind(port).sync()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}