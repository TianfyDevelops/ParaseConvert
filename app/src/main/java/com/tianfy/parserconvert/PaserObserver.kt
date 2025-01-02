package com.tianfy.parserconvert

abstract class ParserObserver<T> constructor(val startByte: Byte, val protocolLength: Short) :
    ReceiveObserver {
    override fun onParseSuccess(data: Any) {
        onChanged(Result.success(data as T))
    }

    override fun onParseError(e: Exception) {
        onChanged(Result.failure(e))
    }

    abstract fun onChanged(value: Result<T>)

}