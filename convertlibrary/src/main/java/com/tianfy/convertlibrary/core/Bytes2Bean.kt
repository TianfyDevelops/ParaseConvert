package com.tianfy.convertlibrary.core

import com.tianfy.convertlibrary.anno.BaseFieldAnnotation
import com.tianfy.convertlibrary.anno.ConvertArray
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Comparator
import kotlin.experimental.and

class Bytes2Bean {

    companion object {
        private const val TAG = "Bytes2Bean"
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
        val fields = clazz.declaredFields.asList()
            .filter { it.getAnnotation(BaseFieldAnnotation::class.java) != null }
            .stream()
            .sorted(Comparator.comparingInt {
                it.getAnnotation(BaseFieldAnnotation::class.java)!!.order
            })
        fields.forEach {
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
                    parserList(parserBean, it, byteBuffer)
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
        it.set(parserBean, newInstance)
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