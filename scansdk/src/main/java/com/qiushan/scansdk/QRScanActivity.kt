package com.qiushan.scansdk

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.Result
import kotlinx.android.synthetic.main.activity_qr_scan_layout.*
import java.io.IOException

class QRScanActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private var hasSurface = false
    private var handler: QRScanHandler? = null

    //定时任务
    private var inactivityTimer = InactivityTimer(this)

    //扫码提示音播放器
    private var soundPlayer: MediaPlayer? = null

    private var isFirstIn = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("QRScanActivity", "Welcome to QRScanSdk！")
        setContentView(R.layout.activity_qr_scan_layout)
        initView()
        initSoundPlayer()
        hasSurface = false
    }

    /**
     * 初始化UI
     */
    private fun initView() {
        previewSurfaceView.visibility = View.GONE
        //返回
        backBtn.setOnClickListener {
            finish()
        }
        //手电筒
        lightCheckBox.setOnCheckedChangeListener { _, isChecked ->
            val cameraManager = QRCameraManager.getInstance(this)
            if (isChecked) {
                cameraManager.enableFlashlight()
            } else {
                cameraManager.disableFlashlight()
            }
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
                    Uri.parse("android.resource://" + packageName + "/" + R.raw.common_voice_1)
                soundPlayer!!.setDataSource(this, uri)
                soundPlayer!!.setVolume(1.0f, 1.0f)
                soundPlayer!!.prepare()
            } catch (var3: IOException) {
                soundPlayer = null
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFirstIn) {
            isFirstIn = false
            if (checkCameraPermission()) {
                showPreview()
            }
        } else {
            showPreview()
        }
    }

    private fun showPreview() {
        previewSurfaceView.visibility = View.VISIBLE
        val surfaceHolder = previewSurfaceView!!.holder
        if (hasSurface) {
            prepareCamera(surfaceHolder)
        } else {
            surfaceHolder.addCallback(this)
            surfaceHolder.setType(3)
        }
    }

    private fun checkCameraPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission("android.permission.CAMERA") != PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf("android.permission.CAMERA"), 1)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            previewSurfaceView!!.visibility = View.VISIBLE
            val surfaceHolder: SurfaceHolder
            if ("android.permission.CAMERA" == permissions[0] && grantResults[0] == 0) {
                surfaceHolder = previewSurfaceView!!.holder
                if (hasSurface) {
                    prepareCamera(surfaceHolder)
                } else {
                    surfaceHolder.addCallback(this)
                    surfaceHolder.setType(3)
                }
            } else {
                surfaceHolder = previewSurfaceView!!.holder
                prepareCamera(surfaceHolder)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inactivityTimer.shutdown()
        QRScanManager.removeCallback()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!hasSurface) {
            hasSurface = true
            prepareCamera(holder)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        hasSurface = false
        lightCheckBox!!.isChecked = false
        if (handler != null) {
            handler!!.quitSynchronously()
            handler = null
        }
        try {
            QRCameraManager.getInstance(this).closeDriver(previewSurfaceView, this)
        } catch (var3: Exception) {
            Log.e("QRCameraManager", "surfaceDestroyed", var3)
        }
    }

    private fun prepareCamera(holder: SurfaceHolder) {
        try {
            QRCameraManager.getInstance(this).openDriver(holder)
        } catch (var3: Exception) {
            QRCameraManager.getInstance(this).setCameraPermissionDeny()
            Toast.makeText(this, "no permission", Toast.LENGTH_SHORT).show()
            return
        }
        if (handler != null) {
            handler!!.quitSynchronously()
        }
        handler = QRScanHandler(this, null, null)
    }

    fun handleDecoded(result: Result, barCode: Bitmap?) {
        playBeepSoundAndVibrate()
        inactivityTimer.onActivity()
        val resultString = result.text
        handleScanResult(resultString)
    }

    private fun handleScanResult(result: String) {
        QRScanManager.sendScanResult(result)
        finish()
    }

    fun getViewfinderView(): QRFinderView? {
        return finderView
    }

    private fun playBeepSoundAndVibrate() {
        soundPlayer?.start()
    }

    fun getHandler(): Handler? {
        return handler
    }

    fun drawViewfinder() {
        finderView!!.drawViewfinder()
    }
}