package com.tianfy.parserconvert

import android.util.Log
import com.blankj.utilcode.util.ConvertUtils
import java.lang.reflect.ParameterizedType

/**
 * @author tianfy
 * 解析转换
 * bytes转对象
 * 对象转bytes
 */
class ParserConvert private constructor() {
    companion object {
        private const val TAG = "ParserConvert"
        val Instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ParserConvert()
        }
    }

    private val bytes2Bean = Bytes2Bean()
    private val bean2Bytes = Bean2Bytes()

    private val parserMap = mutableMapOf<String, ParserObserver<*>>()
    fun <T> addParserBean(parserObserver: ParserObserver<T>) {
        parserMap["${parserObserver.startByte.toInt()}-${parserObserver.protocolLength}"] = parserObserver
    }

    /**
     * 解析字节数组为对象
     * 对象中的集合要使用ArrayList类型,并初始化长度,否则无法解析
     * @param bytes 字节数组
     */
    fun parseBytes2Bean(
        bytes: ByteArray,
    ) {
        val key = "${bytes[0].toInt()}-${bytes.size}"
        if (parserMap.containsKey(key)) {
            val parserObserver = parserMap[key]!!
            // passerBean
            val clazz = parserObserver::class.java
            val genericInterfaces = clazz.genericInterfaces

            val parameterizedType = genericInterfaces[0] as ParameterizedType
            val genericTypeClazz = parameterizedType.rawType as Class<*>

            Log.d(TAG, "parseBytes2Bean clazz:${genericTypeClazz.simpleName}")
            Log.d(TAG, "parseBytes2Bean rawBytes:${ConvertUtils.bytes2HexString(bytes)}")
            try {
                val parserObject = parseBytes2Bean(bytes, genericTypeClazz)
                if (parserObject != null) {
                    parserObserver.onParseSuccess(parserObject)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                parserObserver.onParseError(e)
            }
        }
    }

    fun <T> parseBytes2Bean(
        bytes: ByteArray,
        clazz: Class<T>
    ): T? {
        return bytes2Bean.parseBytes2Bean(bytes, clazz)
    }

    fun parseBean2Bytes(parserBean: Any): ByteArray {
        return bean2Bytes.parserBean2Bytes(parserBean)
    }

}


