package com.tianfy.parserconvert

interface ReceiveObserver {
    fun onParseSuccess(data: Any)
    fun onParseError(e: Exception)
}