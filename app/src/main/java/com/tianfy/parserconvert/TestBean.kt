package com.tianfy.parserconvert

import com.tianfy.convertlibrary.anno.BaseFieldAnnotation
import com.tianfy.convertlibrary.anno.ConvertArray

data class TestBean(
    @BaseFieldAnnotation(order = 1)
    val startByte: Byte = 0x5a.toByte(),
    @BaseFieldAnnotation(order = 2)
    val length: Int = 10,
    @BaseFieldAnnotation(order = 3)
    @ConvertArray(size = 4)
    val states: ArrayList<Byte> = arrayListOf(1,2,3,4),
    @BaseFieldAnnotation(order = 4)
    val crc: Byte = 0
)