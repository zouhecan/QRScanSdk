package com.qiushan.scansdk;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

final class PreviewCallback implements Camera.PreviewCallback {
    private static final String TAG = PreviewCallback.class.getSimpleName();
    private final CameraConfigurationManager configManager;
    private Handler previewHandler;
    private int previewMessage;

    PreviewCallback(CameraConfigurationManager configManager) {
        this.configManager = configManager;
    }

    void setHandler(Handler previewHandler, int previewMessage) {
        this.previewHandler = previewHandler;
        this.previewMessage = previewMessage;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Point cameraResolution = this.configManager.getCameraResolution();
        if (this.previewHandler != null) {
            Message message = this.previewHandler.obtainMessage(this.previewMessage, cameraResolution.x, cameraResolution.y, data);
            message.sendToTarget();
            this.previewHandler = null;
        } else {
            Log.d(TAG, "Got preview callback, but no handler for it");
        }

    }
}
