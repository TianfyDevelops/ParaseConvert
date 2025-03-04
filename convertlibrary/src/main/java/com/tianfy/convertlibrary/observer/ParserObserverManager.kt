package com.tianfy.convertlibrary.observer

import android.os.Looper
import com.tianfy.convertlibrary.core.ParserConvert
import java.lang.reflect.ParameterizedType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class ParserObserverManager private constructor() {
    companion object {
        val Instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ParserObserverManager()
        }
    }

    private val singleThread = Executors.newSingleThreadExecutor()
    private val parserMap = ConcurrentHashMap<String, MutableList<ParserObserver<*>>>()
    fun <T> addObserver(parserObserver: ParserObserver<T>) {
        val parserObservers = parserMap["${parserObserver.startByte.toInt()}-${parserObserver.protocolLength}"]
        if (parserObservers.isNullOrEmpty()) {
            parserMap["${parserObserver.startByte.toInt()}-${parserObserver.protocolLength}"] = mutableListOf(parserObserver)
        } else {
            parserObservers.add(parserObserver)
        }
    }

    fun handle(byteArray: ByteArray) {
        val key = "${byteArray[0].toInt()}-${byteArray.size}"
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            singleThread.execute {
                parseRawBytes(key, byteArray)
            }
        } else {
            parseRawBytes(key, byteArray)
        }
    }

    private fun parseRawBytes(key: String, byteArray: ByteArray) {
        if (parserMap.containsKey(key)) {
            val parserObservers = parserMap[key]!!
            parserObservers.forEach { parserObserver ->
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
    }

    fun removeAllObserver() {
        parserMap.clear()
    }


}