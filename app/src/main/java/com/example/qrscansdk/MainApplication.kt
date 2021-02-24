package com.example.qrscansdk

import android.app.Application
import com.qiushan.scansdk.QRScanInit

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        QRScanInit.initSdk(this)
    }
}