package com.qiushan.scansdk;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.zxing.PlanarYUVLuminanceSource;

import java.io.IOException;

public final class QRCameraManager {
    private static final String TAG = QRCameraManager.class.getSimpleName();
    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 480;
    private static final int MAX_FRAME_HEIGHT = 360;
    private static QRCameraManager QRCameraManager;
    static final int SDK_INT;
    private final Context context;
    private final CameraConfigurationManager configManager;
    private Camera camera;
    private Rect framingRect;
    private Rect framingRectInPreview;
    private boolean initialized;
    private boolean previewing;
    private final PreviewCallback previewCallback;
    private final AutoFocusCallback autoFocusCallback;

    public static synchronized QRCameraManager getInstance(Context context) {
        if (QRCameraManager == null) {
            QRCameraManager = new QRCameraManager(context);
        }

        return QRCameraManager;
    }

    private QRCameraManager(Context context) {
        this.context = context;
        this.configManager = new CameraConfigurationManager(context);
        this.previewCallback = new PreviewCallback(this.configManager);
        this.autoFocusCallback = new AutoFocusCallback();
    }

    public void openDriver(SurfaceHolder holder) throws IOException {
        if (this.camera == null) {
            this.camera = Camera.open();
            if (this.camera == null) {
                throw new IOException();
            }

            this.camera.setPreviewDisplay(holder);
            if (!this.initialized) {
                this.initialized = true;
                this.configManager.initFromCameraParameters(this.camera);
            }

            this.configManager.setDesiredCameraParameters(this.camera);
        }

    }

    public void closeDriver(SurfaceView view, SurfaceHolder.Callback cb) {
        synchronized (QRCameraManager) {
            if (this.camera != null) {
                if (this.isFlashLightOn()) {
                    this.disableFlashlight();
                }

                view.getHolder().removeCallback(cb);
                this.camera.stopPreview();
                this.camera.setPreviewCallback((Camera.PreviewCallback) null);
                this.camera.release();
                this.camera = null;
            }

        }
    }

    public void startPreview() {
        if (this.camera != null && !this.previewing) {
            this.camera.startPreview();
            this.previewing = true;
        }

    }

    public void stopPreview() {
        if (this.camera != null && this.previewing) {
            this.camera.stopPreview();
            this.previewCallback.setHandler((Handler) null, 0);
            this.autoFocusCallback.setHandler((Handler) null, 0);
            this.previewing = false;
        }

    }

    public void requestPreviewFrame(Handler handler, int message) {
        if (this.camera != null && this.previewing) {
            this.previewCallback.setHandler(handler, message);
            this.camera.setOneShotPreviewCallback(this.previewCallback);
        }
    }

    public void requestAutoFocus(Handler handler, int message) {
        if (this.camera != null && this.previewing) {
            this.autoFocusCallback.setHandler(handler, message);

            try {
                this.camera.autoFocus(this.autoFocusCallback);
            } catch (RuntimeException var4) {
                Toast.makeText(this.context, "相机自动对焦失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    public Rect getFramingRect() {
        Point screenResolution = this.configManager.getScreenResolution();
        if (this.framingRect == null) {
            if (this.camera == null) {
                return null;
            }

            if (screenResolution == null) {
                return null;
            }

            int expectedWidth = DeviceUtil.getPixelFromDip(240.0F);
            int width = expectedWidth;
            if (screenResolution.x - expectedWidth < DeviceUtil.getPixelFromDip(20.0F)) {
                width = screenResolution.x - DeviceUtil.getPixelFromDip(20.0F);
            }

            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - width - DeviceUtil.getPixelFromDip(105.0F)) / 2;
            this.framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + width);
        }

        return this.framingRect;
    }

    public Rect getFramingRectInPreview() {
        if (this.framingRectInPreview == null) {
            Rect rect = new Rect(this.getFramingRect());
            Point cameraResolution = this.configManager.getCameraResolution();
            Point screenResolution = this.configManager.getScreenResolution();
            rect.left = rect.left * cameraResolution.y / screenResolution.x;
            rect.right = rect.right * cameraResolution.y / screenResolution.x;
            rect.top = rect.top * cameraResolution.x / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
            this.framingRectInPreview = rect;
        }

        return this.framingRectInPreview;
    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = this.getFramingRectInPreview();
        int previewFormat = this.configManager.getPreviewFormat();
        String previewFormatString = this.configManager.getPreviewFormatString();
        switch (previewFormat) {
            case 16:
            case 17:
                return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height(), false);
            default:
                if ("yuv420p".equals(previewFormatString)) {
                    return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height(), false);
                } else {
                    throw new IllegalArgumentException("Unsupported picture format: " + previewFormat + '/' + previewFormatString);
                }
        }
    }

    public Context getContext() {
        return this.context;
    }

    public boolean isFlashLightOn() {
        try {
            if (this.camera != null) {
                return CameraConfigurationManager.FlashLightStatus.ON == this.configManager.getFlashLightStatus(this.camera);
            }
        } catch (Exception var2) {
            var2.printStackTrace();
        }

        return false;
    }

    public void enableFlashlight() {
        if (this.camera != null) {
            this.configManager.setFlashLight(this.camera, true);
        }

    }

    public void disableFlashlight() {
        if (this.camera != null) {
            this.configManager.setFlashLight(this.camera, false);
        }

    }

    public void setCameraPermissionDeny() {
        this.camera = null;
    }

    static {
        int sdkInt;
        try {
            sdkInt = Integer.parseInt(Build.VERSION.SDK);
        } catch (NumberFormatException var2) {
            sdkInt = 10000;
        }

        SDK_INT = sdkInt;
    }
}

