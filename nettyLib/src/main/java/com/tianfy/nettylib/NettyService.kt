package com.tianfy.nettylib

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress


class NettyService : Service() {

    companion object {
        const val PORTS = "portArray"
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val remoteCallbackList = RemoteCallbackList<INettyServiceCallback>()

    private val writeBytesFlow =
        MutableSharedFlow<NettyReceive>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val binder = object : INettyInterface.Stub() {
        override fun addCallback(callback: INettyServiceCallback?) {
            remoteCallbackList.register(callback)
        }

        override fun removeCallback(callback: INettyServiceCallback?) {
            remoteCallbackList.unregister(callback)
        }

        override fun writeBytes(bytes: ByteArray, ip: String, port: Int) {
            writeBytesFlow.tryEmit(NettyReceive(bytes, ip, port))
        }
    }

    override fun onCreate() {
        super.onCreate()
        observerWriteBytes()
    }

    private fun observerWriteBytes() {
        coroutineScope.launch {
            writeBytesFlow.collect {
                withContext(Dispatchers.IO) {
                    val byteBuf = ByteBufAllocator.DEFAULT.buffer().writeBytes(it.bytes)
                    val datagramPacket = DatagramPacket(byteBuf, InetSocketAddress(it.ip, it.port))
                    channelFuture?.channel()?.write(datagramPacket)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        initNetty(intent)
        return binder
    }

    private var channelFuture: ChannelFuture? = null

    private fun initNetty(intent: Intent?) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val intArrayExtra = intent?.getIntArrayExtra(PORTS)
                val workerGroup: EventLoopGroup = NioEventLoopGroup()
                val b = Bootstrap() // (1)
                b.group(workerGroup) // (2)
                b.channel(NioDatagramChannel::class.java) // (3)
                b.option(ChannelOption.SO_BROADCAST, true) // (4)
                b.handler(object : ChannelInitializer<NioDatagramChannel>() {
                    override fun initChannel(ch: NioDatagramChannel) {
                        ch.pipeline().addLast(NettyLocalChannelHandler(remoteCallbackList))
                    }

                })
                intArrayExtra?.distinct()?.forEach { port ->
                    channelFuture = b.bind(port).sync()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel("Service destroyed")
    }
}