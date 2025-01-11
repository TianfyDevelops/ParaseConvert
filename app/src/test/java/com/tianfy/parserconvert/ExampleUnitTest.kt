package com.tianfy.parserconvert

import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ReflectUtils
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.ParameterizedType


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@Suppress("UNUSED_EXPRESSION")
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }


    data class TestBean(
        val startByte: Byte = 0x5a.toByte(),
        val protocolLength: Short = 0,

        @ConvertArray(size = 4)
        val states: ArrayList<Byte> = arrayListOf(1, 2, 3, 4),
        val crc: Byte = 0,
    )

    @Test
    fun test_bean2Bytes() = try {
        val testBean = TestBean()
//            val java = testBean::class.java
//            val field = java.getDeclaredField("states")
//            field.isAccessible = true
//            val genericType = field.genericType
//            if (genericType is ParameterizedType){
//                val type = genericType.actualTypeArguments[0]
//                println(type)
//            }
//            println(genericType)

//        val java = testBean::class.java
//        val declaredField = java.getDeclaredField("states")
//        declaredField.isAccessible = true
//        val get = declaredField.get(testBean)
//        println(get)

        val parserBean2Bytes = ParserConvert.Instance.parserBean2Bytes(testBean)
        val parseBytes2Bean = ParserConvert.Instance.parseBytes2Bean(parserBean2Bytes, TestBean::class.java)
        println(parseBytes2Bean.toString())
        val bytes2HexString = ConvertUtils.bytes2HexString(parserBean2Bytes)
        println(bytes2HexString)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}