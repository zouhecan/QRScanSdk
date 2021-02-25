package com.example.qrscansdk

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qiushan.scansdk.QRScanManager
import com.qiushan.scansdk.QRScanResultCallback

/**
 * desc: 扫一扫
 * date: 2020/12/29
 */
class MainActivity : AppCompatActivity(),QRScanResultCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main_activity)
    }

    fun startScan(view: View) {
        QRScanManager.startScan(this, this)
    }

    override fun onResult(result: String?) {
        Toast.makeText(this, "扫码成功：$result", Toast.LENGTH_LONG).show()
    }
}