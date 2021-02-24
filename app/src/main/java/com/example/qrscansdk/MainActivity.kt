package com.example.qrscansdk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.qiushan.scansdk.QRScanActivity

/**
 * desc: 扫一扫
 * date: 2020/12/29
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main_activity)
    }

    fun startScan(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, QRScanActivity::class.java))
        } else {
            val permissions = mutableListOf(Manifest.permission.CAMERA)
            this.requestPermissions(permissions.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, QRScanActivity::class.java))
        } else {
            Toast.makeText(this, "请先打开相机权限", Toast.LENGTH_LONG).show()
        }
    }
}