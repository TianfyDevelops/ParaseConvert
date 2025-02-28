package com.tianfy.convertlibrary.observer

import android.os.Looper
import androidx.annotation.MainThread
import com.tianfy.convertlibrary.core.ParserConvert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.reflect.ParameterizedType
import java.util.concurrent.ConcurrentHashMap

class ParserObserverManager private constructor() {
    companion object {
        val Instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ParserObserverManager()
        }
    }

    private val parserMap = ConcurrentHashMap<String, ParserObserver<*>>()
    fun <T> addObserver(parserObserver: ParserObserver<T>) {
        parserMap["${parserObserver.startByte.toInt()}-${parserObserver.protocolLength}"] = parserObserver
    }

    suspend fun handle(byteArray: ByteArray) {
        val key = "${byteArray[0].toInt()}-${byteArray.size}"
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            withContext(Dispatchers.IO) {
                parseRawBytes(key, byteArray)
            }
        } else {
            parseRawBytes(key, byteArray)
        }
    }

    private fun parseRawBytes(key: String, byteArray: ByteArray) {
        if (parserMap.containsKey(key)) {
            val parserObserver = parserMap[key]!!
            try {
                // passerBean
                val clazz = parserObserver::class.java
                val genericSuperclass = clazz.genericSuperclass
                if (genericSuperclass is ParameterizedType) {
                    val genericTypeClazz = genericSuperclass.actualTypeArguments[0] as Class<*>
                    val parserObject = ParserConvert.Instance.parseBytes2Bean(byteArray, genericTypeClazz)
                    parserObserver.onParseSuccess(parserObject, byteArray)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                parserObserver.onParseError(e)
            }
        }
    }

    fun removeAllObserver() {
        parserMap.clear()
    }


}