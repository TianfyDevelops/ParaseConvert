package com.tianfy.parserconvert

import android.util.Log
import com.blankj.utilcode.util.ConvertUtils
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

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
    fun parserBean2Bytes(parserBean: Any): ByteArray {
        // 先计算要转换bytes的长度
        val clazz = parserBean::class.java
        try {
            val byteSize = calculateSize(clazz)
//            Log.d(TAG, "parserBean2Bytes byteSize:$byteSize")
            // 计算完长度后，创建ByteBuffer，然后填充数据
            val byteBuffer = convertBytes(byteSize, clazz, parserBean)
            val bytes = byteBuffer.array()
//            Log.d(TAG, "parserBean2Bytes convertBytesHex:${ConvertUtils.bytes2HexString(bytes)}")
            return bytes
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun convertBytes(
        byteSize: Int,
        clazz: Class<*>,
        parserBean: Any
    ): ByteBuffer {
        val byteBuffer = ByteBuffer.allocate(byteSize).order(ByteOrder.BIG_ENDIAN)
        clazz.declaredFields.forEach {
            it.isAccessible = true
            when (it.type) {
                Byte::class.javaPrimitiveType -> {
                    // 一个字节
                    byteBuffer.put(it.getByte(parserBean))
                }

                Short::class.javaPrimitiveType -> {
                    // 两个字节
                    byteBuffer.putShort(it.getShort(parserBean))
                }

                Int::class.javaPrimitiveType -> {
                    // 四个字节
                    byteBuffer.putInt(it.getInt(parserBean))
                }

                Long::class.javaPrimitiveType -> {
                    // 八个字节
                    byteBuffer.putLong(it.getLong(parserBean))
                }

                Double::class.javaPrimitiveType -> {
                    // 八个字节
                    byteBuffer.putDouble(it.getDouble(parserBean))
                }

                ArrayList::class.java -> {
                    it.isAccessible = true
                    val listClazz = it.type as Class<*>
                    val size = arrayListCheck(listClazz, it)
                    val list = it.get(parserBean) as List<*>
                    val genericType = it.genericType
                    val parameterizedType = genericType as ParameterizedType
                    val type = parameterizedType.actualTypeArguments[0]
                    for (i in 0 until size) {
                        when (type as Class<*>) {
                            Byte::class.javaObjectType -> {
                                // 一个字节
                                byteBuffer.put(list[i] as Byte)
                            }

                            Short::class.javaObjectType -> {
                                // 两个字节
                                byteBuffer.putShort(list[i] as Short)
                            }

                            Int::class.javaObjectType -> {
                                // 四个字节
                                byteBuffer.putInt(list[i] as Int)
                            }

                            Long::class.javaObjectType -> {
                                // 八个字节
                                byteBuffer.putLong(list[i] as Long)
                            }

                            Double::class.javaObjectType -> {
                                // 八个字节
                                byteBuffer.putDouble(list[i] as Double)
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
                    val size = arrayListCheck(listClazz, it)
                    val genericType = it.genericType
                    val parameterizedType = genericType as ParameterizedType
                    val type = parameterizedType.actualTypeArguments[0]
                    for (i in 0 until size) {
                        when (type as Class<*>) {
                            Byte::class.javaObjectType -> {
                                // 一个字节
                                byteSize += 1
                            }

                            Short::class.javaObjectType -> {
                                // 两个字节
                                byteSize += 2
                            }

                            Int::class.javaObjectType -> {
                                // 四个字节
                                byteSize += 4
                            }

                            Long::class.javaObjectType -> {
                                // 八个字节
                                byteSize += 8
                            }

                            Double::class.javaObjectType -> {
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

    fun <T> parseBytes2Bean(
        bytes: ByteArray,
        clazz: Class<T>
    ): T? {
        val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        var parserObject: T? = null
        try {
            parserObject = parserObject(clazz, byteBuffer) as T
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return parserObject
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
                    parserList(parserBean,it, byteBuffer)
                }

                else -> {
                    throw IllegalArgumentException("${clazz.simpleName}:${it.type.simpleName} 不支持的类型")
                }
            }

        }
        return parserBean
    }

    private fun parserList(parserBean: Any, it: Field, byteBuffer: ByteBuffer) {
        it.isAccessible = true
        val listClazz = it.type as Class<*>
        val size = arrayListCheck(listClazz, it)
        val genericType = it.genericType
        val parameterizedType = genericType as ParameterizedType
        val type = parameterizedType.actualTypeArguments[0]
        val constructor = listClazz.getConstructor(Int::class.java)
        val newInstance = constructor.newInstance(size) as ArrayList<*>
        val newListClazz = newInstance::class.java
        for (i in 0 until size) {
            when (type) {
                Byte::class.javaObjectType -> {
                    val addMethod = newListClazz.getMethod("add", Any::class.java)
                    addMethod.isAccessible = true
                    addMethod.invoke(newInstance, byteBuffer.get() and 0xff.toByte())
                }

                Short::class.javaObjectType -> {
                    val addMethod = newListClazz.getDeclaredMethod("add", Any::class.java)
                    addMethod.isAccessible = true
                    addMethod.invoke(newInstance, byteBuffer.short)
                }

                Int::class.javaObjectType -> {
                    val addMethod = newListClazz.getDeclaredMethod("add", Any::class.java)
                    addMethod.isAccessible = true
                    addMethod.invoke(newInstance, byteBuffer.int)
                }

                Long::class.javaObjectType -> {
                    val addMethod = newListClazz.getDeclaredMethod("add", Any::class.java)
                    addMethod.isAccessible = true
                    addMethod.invoke(newInstance, byteBuffer.long)
                }

                Double::class.javaObjectType -> {
                    val addMethod = newListClazz.getDeclaredMethod("add", Any::class.java)
                    addMethod.isAccessible = true
                    addMethod.invoke(newInstance, byteBuffer.double)
                }

                else -> {
                    throw IllegalArgumentException("${listClazz.simpleName}:${it.type.simpleName} 不支持的类型")
                }
            }
        }
        it.set(parserBean,newInstance)
    }

    private fun arrayListCheck(listClazz: Class<*>, it: Field): Int {
        if (listClazz != ArrayList::class.java) {
            throw ClassNotFoundException("${it.name} type must be ArrayList")
        }
        val annotation = it.getAnnotation(ConvertArray::class.java)
            ?: throw NullPointerException("${it.name} must add ConvertArray annotation")
        val size = annotation.size
        if (size == 0) {
            throw NullPointerException("${it.name} ConvertArray annotation size can not is 0")
        }
        return size
    }
}


