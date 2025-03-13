package com.tianfy.nettylib

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.RemoteCallbackList
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.DatagramPacket

class NettyLocalChannelHandler(private val remoteCallbackList: RemoteCallbackList<INettyServiceCallback>) :
    SimpleChannelInboundHandler<DatagramPacket>(), Handler.Callback {
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    override fun channelActive(ctx: ChannelHandlerContext?) {
        super.channelActive(ctx)
        handlerThread = HandlerThread("NettyLocalChannelHandlerThread").apply {
            start()
        }
        handler = handlerThread?.looper?.let { Handler(it) }
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: DatagramPacket?) {
        msg?.let {
            val sender = it.sender()
            val port = sender.port
            val hostString = sender.hostString
            val byteBuf = it.content()
            val array = byteBuf.array()
            val message = Message.obtain().apply {
                obj = NettyReceive(array, hostString, port)
            }
            handler?.sendMessage(message)
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        val nettyReceive = msg.obj as NettyReceive
        try {
            remoteCallbackList.beginBroadcast()
            val registeredCallbackCount = remoteCallbackList.registeredCallbackCount
            for (i in 0..<registeredCallbackCount) {
                val broadcastItem = remoteCallbackList.getBroadcastItem(i)
                broadcastItem.receiveBytes(nettyReceive.bytes, nettyReceive.ip, nettyReceive.port)
            }
        } finally {
            remoteCallbackList.finishBroadcast()
        }
        return true
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        super.channelInactive(ctx)
        handlerThread?.quitSafely()
        handler?.removeCallbacksAndMessages(null)
        handlerThread = null
        handler = null
    }
}