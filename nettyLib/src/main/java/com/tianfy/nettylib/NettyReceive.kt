package com.tianfy.nettylib

data class NettyReceive (

    val bytes: ByteArray,
    val ip: String,
    val port: Int,

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NettyReceive

        if (!bytes.contentEquals(other.bytes)) return false
        if (ip != other.ip) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + ip.hashCode()
        result = 31 * result + port
        return result
    }
}