package com.tianfy.convertlibrary.observer

interface ReceiveObserver {
    fun onParseSuccess(data: Any?,bytes:ByteArray)
    fun onParseError(e: Exception)
}