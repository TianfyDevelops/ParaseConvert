package com.tianfy.parserconvert

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ConvertUtils
import com.tianfy.convertlibrary.core.ParserConvert
import com.tianfy.convertlibrary.observer.ParserObserver
import com.tianfy.convertlibrary.observer.ParserObserverManager
import com.tianfy.parserconvert.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var bytes: ByteArray? = null


    inner class CustomParserObserver : ParserObserver<TestBean>(0x5a.toByte(), 10) {
        override fun onChanged(value: Result<TestBean>) {
            value.onSuccess {
                binding.tvConvertBean.text = it.toString()
            }
            value.onFailure {
                binding.tvConvertBean.text = it.message
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ParserObserverManager.Instance.addObserver(CustomParserObserver())

        binding.btnBean2bytes.setOnClickListener {
            Toast.makeText(this, "hello", Toast.LENGTH_SHORT).show()
            bytes = ParserConvert.Instance.parseBean2Bytes(TestBean())
            binding.tvConvertBytes.text = ConvertUtils.bytes2HexString(bytes)
        }
        binding.btnBytes2Bean.setOnClickListener {
            bytes?.let {
                ParserObserverManager.Instance.handle(it)
            }
        }
    }
}