package com.qiushan.scansdk;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.util.Vector;

public final class QRScanHandler extends Handler {
    private static final String TAG = QRScanHandler.class.getSimpleName();
    private final QRScanActivity activity;
    private final DecodeThread decodeThread;
    private State state;

    public QRScanHandler(QRScanActivity activity, Vector<BarcodeFormat> decodeFormats, String characterSet) {
        this.activity = activity;
        //开启解码线程
        this.decodeThread = new DecodeThread(activity, decodeFormats, characterSet, new QRFinderResultPointCallback(activity.getViewfinderView()));
        this.decodeThread.start();
        this.state = State.SUCCESS;
        //启动摄像头预览
        QRCameraManager.getInstance(activity).startPreview();
        //开始预览并解码
        restartPreviewAndDecode();
    }

    /**
     * 预览并解码
     */
    private void restartPreviewAndDecode() {
        if (this.state == State.SUCCESS) {
            this.state = State.PREVIEW;
            //请求摄像头的一帧图像数据
            QRCameraManager.getInstance(this.activity).requestPreviewFrame(this.decodeThread.getHandler(), R.id.decode);
            //请求摄像头聚焦
            QRCameraManager.getInstance(this.activity).requestAutoFocus(this, R.id.auto_focus);
            //重绘扫码框控件
            this.activity.drawViewfinder();
        }
    }

    /**
     * 处理各种扫码状态
     */
    @SuppressLint("NonConstantResourceId")
    public void handleMessage(Message message) {
        if (message.what == R.id.auto_focus) {//聚焦完成
            if (this.state == State.PREVIEW) {
                QRCameraManager.getInstance(this.activity).requestAutoFocus(this, R.id.auto_focus);
            }
        } else if (message.what == R.id.decode_succeeded) {//扫码成功，进入解码流程
            this.state = State.SUCCESS;
            Bundle bundle = message.getData();
            Bitmap barcode = bundle == null ? null : (Bitmap) bundle.getParcelable("barcode_bitmap");
            this.activity.handleDecoded((Result) message.obj, barcode);
            Log.d(TAG, "Got decode succeeded message");
        } else if (message.what == R.id.decode_failed) {//扫码失败，继续扫下一帧
            this.state = State.PREVIEW;
            QRCameraManager.getInstance(this.activity).requestPreviewFrame(this.decodeThread.getHandler(), R.id.decode);
        }
    }

    /**
     * 终止扫码
     */
    public void quitSynchronously() {
        this.state = State.DONE;
        QRCameraManager.getInstance(this.activity).stopPreview();
        Message quit = Message.obtain(this.decodeThread.getHandler(), R.id.quit);
        quit.sendToTarget();
        try {
            this.decodeThread.join();
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }
        this.removeMessages(R.id.decode_succeeded);
        this.removeMessages(R.id.decode_failed);
    }

    private enum State {
        PREVIEW,
        SUCCESS,
        DONE;

        State() {
        }
    }
}
