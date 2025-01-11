package com.tianfy.convertlibrary.core

import android.util.Log
import com.blankj.utilcode.util.ConvertUtils
import com.tianfy.convertlibrary.anno.BaseFieldAnnotation
import com.tianfy.convertlibrary.anno.ConvertArray
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Bean2Bytes {
    companion object {
        private const val TAG = "Bean2Bytes"
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
            Log.d(TAG, "parserBean2Bytes byteSize:$byteSize")
            // 计算完长度后，创建ByteBuffer，然后填充数据
            val byteBuffer = convertBytes(byteSize, clazz, parserBean)
            val bytes = byteBuffer.array()
            Log.d(TAG, "parserBean2Bytes convertBytesHex:${ConvertUtils.bytes2HexString(bytes)}")
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
        val fields = clazz.declaredFields.asList()
            .filter { it.getAnnotation(BaseFieldAnnotation::class.java) != null }
            .stream()
            .sorted(Comparator.comparingInt {
                it.getAnnotation(BaseFieldAnnotation::class.java)!!.order
            })
        fields.forEach {
            Log.d(TAG, it.name)
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