package com.tianfy.convertlibrary.observer

import com.tianfy.convertlibrary.core.ParserConvert
import java.lang.reflect.ParameterizedType

class ParserObserverManager private constructor() {
    companion object {
        val Instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ParserObserverManager()
        }
    }

    private val parserMap = mutableMapOf<String, ParserObserver<*>>()
    fun <T> addObserver(parserObserver: ParserObserver<T>) {
        parserMap["${parserObserver.startByte.toInt()}-${parserObserver.protocolLength}"] = parserObserver
    }

    fun handle(byteArray: ByteArray) {
        val key = "${byteArray[0].toInt()}-${byteArray.size}"
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