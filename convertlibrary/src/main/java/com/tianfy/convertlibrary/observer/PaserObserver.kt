package com.tianfy.convertlibrary.observer

abstract class ParserObserver<T>(val startByte: Byte, val protocolLength: Short) :
    ReceiveObserver {
    override fun onParseSuccess(data: Any?,bytes:ByteArray) {
        onChanged(Result.success(data as T))
    }

    override fun onParseError(e: Exception) {
        onChanged(Result.failure(e))
    }

    abstract fun onChanged(value: Result<T>)

}