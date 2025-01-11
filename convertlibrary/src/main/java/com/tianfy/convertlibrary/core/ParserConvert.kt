package com.tianfy.convertlibrary.core

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


