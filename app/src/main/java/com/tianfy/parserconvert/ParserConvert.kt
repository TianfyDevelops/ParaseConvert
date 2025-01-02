package com.tianfy.parserconvert

import android.util.Log
import com.blankj.utilcode.util.ConvertUtils
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and

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

    private val parserMap = mutableMapOf<String, ParserObserver<*>>()

    fun <T> addParserBean(parserObserver: ParserObserver<T>) {
        parserMap["${parserObserver.startByte.toInt()}-${parserObserver.protocolLength}"] = parserObserver
    }

    /**
     * 将对象转换为字节数组
     * 长度和CRC需要自己计算填充
     * @param parserBean 要解析的对象
     * @return 字节数组
     */
    fun <T> parserBean2Bytes(parserBean: Any): ByteArray {
        // 先计算要转换bytes的长度
        val clazz = parserBean::class.java
        try {
            val byteSize = calculateSize(clazz)
            Log.d(TAG, "parserBean2Bytes byteSize:$byteSize")
            // 计算完长度后，创建ByteBuffer，然后填充数据
            val byteBuffer = convertBytes(byteSize, clazz)
            val bytes = byteBuffer.array()
            Log.d(TAG, "parserBean2Bytes convertBytesHex:${ConvertUtils.bytes2HexString(bytes)}")
            return bytes
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun convertBytes(
        byteSize: Int,
        calzz: Class<*>
    ): ByteBuffer {
        val byteBuffer = ByteBuffer.allocate(byteSize).order(ByteOrder.BIG_ENDIAN)
        calzz.declaredFields.forEach {
            when (it.type) {
                Byte::class.javaPrimitiveType -> {
                    // 一个字节
                    byteBuffer.put(it.getByte(calzz))
                }

                Short::class.javaPrimitiveType -> {
                    // 两个字节
                    byteBuffer.putShort(it.getShort(calzz))
                }

                Int::class.javaPrimitiveType -> {
                    // 四个字节
                    byteBuffer.putInt(it.getInt(calzz))
                }

                Long::class.javaPrimitiveType -> {
                    // 八个字节
                    byteBuffer.putLong(it.getLong(calzz))
                }

                Double::class.javaPrimitiveType -> {
                    // 八个字节
                    byteBuffer.putDouble(it.getDouble(calzz))
                }

                ArrayList::class.java -> {
                    it.isAccessible = true
                    val listClazz = it.type as Class<*>
                    val field = listClazz.getField("size")
                    field.isAccessible = true
                    val size = field.get(listClazz) as Int
                    for (i in 0 until size) {
                        when (it.genericType as Class<*>) {
                            Byte::class.javaPrimitiveType -> {
                                // 一个字节
                                byteBuffer.put(it.getByte(listClazz))
                            }

                            Short::class.javaPrimitiveType -> {
                                // 两个字节
                                byteBuffer.putShort(it.getShort(listClazz))
                            }

                            Int::class.javaPrimitiveType -> {
                                // 四个字节
                                byteBuffer.putInt(it.getInt(listClazz))
                            }

                            Long::class.javaPrimitiveType -> {
                                // 八个字节
                                byteBuffer.putLong(it.getLong(listClazz))
                            }

                            Double::class.javaPrimitiveType -> {
                                // 八个字节
                                byteBuffer.putDouble(it.getDouble(listClazz))
                            }

                            else -> {
                                Log.d(TAG, "暂不支持的类型:${it.genericType}")
                            }
                        }
                    }
                }
            }

        }
        return byteBuffer
    }

    // 计算字节长度
    private fun calculateSize(genericTypeClazz: Class<*>): Int {
        var byteSize = 0
        genericTypeClazz.declaredFields.forEach {
            when (it.type) {
                Byte::class.javaPrimitiveType -> {
                    // 一个字节
                    byteSize += 1
                }

                Short::class.javaPrimitiveType -> {
                    // 两个字节
                    byteSize += 2
                }

                Int::class.javaPrimitiveType -> {
                    // 四个字节
                    byteSize += 4
                }

                Long::class.javaPrimitiveType -> {
                    // 八个字节
                    byteSize += 8
                }

                Double::class.javaPrimitiveType -> {
                    // 八个字节
                    byteSize += 8
                }

                ArrayList::class.java -> {
                    it.isAccessible = true
                    val listClazz = it.type as Class<*>
                    val field = listClazz.getField("size")
                    field.isAccessible = true
                    val size = field.get(listClazz) as Int
                    for (i in 0 until size) {
                        when (it.genericType as Class<*>) {
                            Byte::class.javaPrimitiveType -> {
                                // 一个字节
                                byteSize += 1
                            }

                            Short::class.javaPrimitiveType -> {
                                // 两个字节
                                byteSize += 2
                            }

                            Int::class.javaPrimitiveType -> {
                                // 四个字节
                                byteSize += 4
                            }

                            Long::class.javaPrimitiveType -> {
                                // 八个字节
                                byteSize += 8
                            }

                            Double::class.javaPrimitiveType -> {
                                // 八个字节
                                byteSize += 8
                            }
                            else -> {
                                throw IllegalArgumentException("${genericTypeClazz.simpleName}:${it.type.simpleName} 不支持的类型")
                            }
                        }
                    }
                }
            }
        }
        return byteSize
    }

    /**
     * 解析字节数组为对象
     * 对象中的集合要使用ArrayList类型,并初始化长度,否则无法解析
     * @param bytes 字节数组
     */
    fun parseBytes2Bean(
        bytes: ByteArray,
    ) {
        val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
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
                val parserObject = parserObject(genericTypeClazz, byteBuffer)
                parserObserver.onParseSuccess(parserObject)
            } catch (e: Exception) {
                e.printStackTrace()
                parserObserver.onParseError(e)
            }
        }
    }

    private fun parserObject(
        clazz: Class<*>,
        byteBuffer: ByteBuffer,
    ): Any {
        val parserBean = clazz.getConstructor().newInstance()
        clazz.declaredFields.forEach {
            when (it.type) {
                Byte::class.javaPrimitiveType -> {
                    val byte = byteBuffer.get() and 0xff.toByte()
                    it.isAccessible = true
                    it.set(parserBean, byte)
                }

                Short::class.javaPrimitiveType -> {
                    val short = byteBuffer.short
                    it.isAccessible = true
                    it.set(parserBean, short)
                }

                Int::class.javaPrimitiveType -> {
                    val int = byteBuffer.int
                    it.isAccessible = true
                    it.set(parserBean, int)
                }

                Long::class.javaPrimitiveType -> {
                    val long = byteBuffer.long
                    it.isAccessible = true
                    it.set(parserBean, long)
                }

                Double::class.javaPrimitiveType -> {
                    val double = byteBuffer.double
                    it.isAccessible = true
                    it.set(parserBean, double)
                }

                ArrayList::class.java -> {
                    parserList(it, byteBuffer)
                }

                else -> {
                    throw IllegalArgumentException("${clazz.simpleName}:${it.type.simpleName} 不支持的类型")
                }
            }
        }
        return parserBean
    }

    private fun parserList(it: Field, byteBuffer: ByteBuffer) {
        it.isAccessible = true
        val listClazz = it.type as Class<*>
        val field = listClazz.getField("size")
        field.isAccessible = true
        val size = field.get(listClazz) as Int
        it.genericType.let { type ->
            for (i in 0 until size) {
                when (type) {
                    Byte::class.javaPrimitiveType -> {
                        listClazz.getMethod("add", Byte::class.javaPrimitiveType)
                            .invoke(listClazz, byteBuffer.get() and 0xff.toByte())
                    }

                    Short::class.javaPrimitiveType -> {
                        listClazz.getMethod("add", Short::class.javaPrimitiveType)
                            .invoke(listClazz, byteBuffer.short)
                    }

                    Int::class.javaPrimitiveType -> {
                        listClazz.getMethod("add", Int::class.javaPrimitiveType)
                            .invoke(listClazz, byteBuffer.int)
                    }

                    Long::class.javaPrimitiveType -> {
                        listClazz.getMethod("add", Long::class.javaPrimitiveType)
                            .invoke(listClazz, byteBuffer.long)
                    }

                    Double::class.javaPrimitiveType -> {
                        listClazz.getMethod("add", Double::class.javaPrimitiveType)
                            .invoke(listClazz, byteBuffer.double)
                    }

                    else -> {
                        throw IllegalArgumentException("${listClazz.simpleName}:${it.type.simpleName} 不支持的类型")
                    }
                }
            }
        }
    }
}


