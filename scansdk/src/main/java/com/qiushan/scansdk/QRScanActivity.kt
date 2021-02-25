package com.qiushan.scansdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_scan.*
import java.io.IOException

/*

                       .::::.
                     .::::::::.
                    :::::::::::
                 ..:::::::::::'
              '::::::::::::'
                .::::::::::
           '::::::::::::::..
                ..::::::::::::.
              ``::::::::::::::::
               ::::``:::::::::'        .:::.
              ::::'   ':::::'       .::::::::.
            .::::'      ::::     .:::::::'::::.
           .:::'       :::::  .:::::::::' ':::::.
          .::'        :::::.:::::::::'      ':::::.
         .::'         ::::::::::::::'         ``::::.
     ...:::           ::::::::::::'              ``::.
    ````':.           :::::::::::                  ::::..
                       '.:::::'                    ':'````..
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
                        扫码app
*/
class QRScanActivity : AppCompatActivity(), QRScanResultCallback {
    //扫码提示音播放器
    private var soundPlayer: MediaPlayer? = null
    private val qrScanFragment = QRScanFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("QRScanActivity", "Welcome to QRScan2Sdk！")
        setContentView(R.layout.activity_scan)
        initView()
    }

    /**
     * 初始化UI
     */
    private fun initView() {
        //提示音
        initSoundPlayer()
        //返回
        backBtn.setOnClickListener {
            finish()
        }
    }

    /**
     * 初始化音频播放器
     */
    private fun initSoundPlayer() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            return
        }

        //铃声在外放模式下，需要播放扫码提示音
        if (soundPlayer == null) {
            volumeControlStream = 1
            soundPlayer = MediaPlayer().apply {
                setAudioStreamType(AudioManager.VIBRATE_SETTING_ON)
                setOnCompletionListener { player -> player.seekTo(0) }
            }

            try {
                val uri =
                    Uri.parse("android.resource://" + FoundationContext.context!!.packageName + "/" + R.raw.common_voice_1)
                soundPlayer!!.setDataSource(FoundationContext.context!!, uri)
                soundPlayer!!.setVolume(1.0f, 1.0f)
                soundPlayer!!.prepare()
            } catch (var3: IOException) {
                soundPlayer = null
            }
        }
    }

    @SuppressLint("NewApi")
    override fun onResume() {
        super.onResume()
        if (checkCameraPermission()) {
            startCamera()
        } else {
            requestPermissions(arrayOf("android.permission.CAMERA"), 1)
        }
    }

    private fun checkCameraPermission(): Boolean {
        return Build.VERSION.SDK_INT >= 23 && checkSelfPermission("android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 1) {
            return
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "请先同意相机使用权限", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCamera() {
        //手电筒
        lightCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                qrScanFragment.openFlashLight()
            } else {
                qrScanFragment.closeFlashLight()
            }
        }
        qrScanFragment.setQrScanResultCallback(this)
        supportFragmentManager.beginTransaction().replace(R.id.cameraContent, qrScanFragment)
            .commitAllowingStateLoss()
    }

    override fun onResult(result: String?) {
        soundPlayer?.start()
        QRScanManager.sendScanResult(result)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        QRScanManager.removeCallback()
    }
}