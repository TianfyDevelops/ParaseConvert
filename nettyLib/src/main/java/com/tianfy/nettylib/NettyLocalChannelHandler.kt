package com.tianfy.nettylib

import android.os.RemoteCallbackList
import androidx.core.util.rangeTo
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.DatagramPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ArrayBlockingQueue

class NettyLocalChannelHandler(private val remoteCallbackList: RemoteCallbackList<INettyServiceCallback>) :
    SimpleChannelInboundHandler<DatagramPacket>() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val blockingQueue = ArrayBlockingQueue<ByteArray>(10, true)

    init {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                while (isActive) {
                    delay(100L)
                    val bytes = blockingQueue.take()
                    try {
                        remoteCallbackList.beginBroadcast()
                        val registeredCallbackCount = remoteCallbackList.registeredCallbackCount
                        for (i in 0..<registeredCallbackCount) {
                            val broadcastItem = remoteCallbackList.getBroadcastItem(i)
                            broadcastItem.receiveBytes(bytes)
                        }
                    } finally {
                        remoteCallbackList.finishBroadcast()
                    }
                }
            }
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: DatagramPacket?) {
        msg?.let {
            val byteBuf = it.content()
            val array = byteBuf.array()
            if (!blockingQueue.offer(array)) {
                blockingQueue.poll()
            }
        }
    }
}