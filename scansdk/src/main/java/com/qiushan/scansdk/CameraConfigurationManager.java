package com.qiushan.scansdk;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.regex.Pattern;

final class CameraConfigurationManager {
    private static final String TAG = CameraConfigurationManager.class.getSimpleName();
    private static final int TEN_DESIRED_ZOOM = 27;
    private static final int DESIRED_SHARPNESS = 30;
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    private final Context context;
    private Point screenResolution;
    private Point cameraResolution;
    private int previewFormat;
    private String previewFormatString;

    CameraConfigurationManager(Context context) {
        this.context = context;
    }

    void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        this.previewFormat = parameters.getPreviewFormat();
        this.previewFormatString = parameters.get("preview-format");
        Log.d(TAG, "Default preview format: " + this.previewFormat + '/' + this.previewFormatString);
        WindowManager manager = (WindowManager)this.context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        this.screenResolution = new Point(display.getWidth(), display.getHeight());
        Log.d(TAG, "Screen resolution: " + this.screenResolution);
        Point screenResolutionForCamera = new Point();
        screenResolutionForCamera.x = this.screenResolution.x;
        screenResolutionForCamera.y = this.screenResolution.y;
        if (this.screenResolution.x < this.screenResolution.y) {
            screenResolutionForCamera.x = this.screenResolution.y;
            screenResolutionForCamera.y = this.screenResolution.x;
        }

        this.cameraResolution = getCameraResolution(parameters, screenResolutionForCamera);
        Log.d(TAG, "Camera resolution: " + this.screenResolution);
    }

    void setDesiredCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Log.d(TAG, "Setting preview size: " + this.cameraResolution);
        parameters.setPreviewSize(this.cameraResolution.x, this.cameraResolution.y);
        this.setFlashLight(parameters);
        this.setZoom(parameters);
        this.setDisplayOrientation(camera, 90);
        camera.setParameters(parameters);
    }

    Point getCameraResolution() {
        return this.cameraResolution;
    }

    Point getScreenResolution() {
        return this.screenResolution;
    }

    int getPreviewFormat() {
        return this.previewFormat;
    }

    String getPreviewFormatString() {
        return this.previewFormatString;
    }

    private static Point getCameraResolution(Camera.Parameters parameters, Point screenResolution) {
        String previewSizeValueString = parameters.get("preview-size-values");
        if (previewSizeValueString == null) {
            previewSizeValueString = parameters.get("preview-size-value");
        }

        Point cameraResolution = null;
        if (previewSizeValueString != null) {
            Log.d(TAG, "preview-size-values parameter: " + previewSizeValueString);
            cameraResolution = findBestPreviewSizeValue(previewSizeValueString, screenResolution);
        }

        if (cameraResolution == null) {
            cameraResolution = new Point(screenResolution.x >> 3 << 3, screenResolution.y >> 3 << 3);
        }

        return cameraResolution;
    }

    private static Point findBestPreviewSizeValue(CharSequence previewSizeValueString, Point screenResolution) {
        int bestX = 0;
        int bestY = 0;
        int diff = 2147483647;
        String[] var5 = COMMA_PATTERN.split(previewSizeValueString);
        int var6 = var5.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            String previewSize = var5[var7];
            previewSize = previewSize.trim();
            int dimPosition = previewSize.indexOf(120);
            if (dimPosition < 0) {
                Log.e(TAG, "Bad preview-size: " + previewSize);
            } else {
                int newX;
                int newY;
                try {
                    newX = Integer.parseInt(previewSize.substring(0, dimPosition));
                    newY = Integer.parseInt(previewSize.substring(dimPosition + 1));
                } catch (NumberFormatException var13) {
                    Log.e(TAG, "Bad preview-size: " + previewSize);
                    continue;
                }

                int newDiff = Math.abs(newX - screenResolution.x) + Math.abs(newY - screenResolution.y);
                if (newDiff == 0) {
                    bestX = newX;
                    bestY = newY;
                    break;
                }

                if (newDiff < diff) {
                    bestX = newX;
                    bestY = newY;
                    diff = newDiff;
                }
            }
        }

        return bestX > 0 && bestY > 0 ? new Point(bestX, bestY) : null;
    }

    private static int findBestMotZoomValue(CharSequence stringValues, int tenDesiredZoom) {
        int tenBestValue = 0;
        String[] var3 = COMMA_PATTERN.split(stringValues);
        int var4 = var3.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String stringValue = var3[var5];
            stringValue = stringValue.trim();

            double value;
            try {
                value = Double.parseDouble(stringValue);
            } catch (NumberFormatException var10) {
                return tenDesiredZoom;
            }

            int tenValue = (int)(10.0D * value);
            if (Math.abs((double)tenDesiredZoom - value) < (double)Math.abs(tenDesiredZoom - tenBestValue)) {
                tenBestValue = tenValue;
            }
        }

        return tenBestValue;
    }

    private void setZoom(Camera.Parameters parameters) {
        String zoomSupportedString = parameters.get("zoom-supported");
        if (zoomSupportedString == null || Boolean.parseBoolean(zoomSupportedString)) {
            int tenDesiredZoom = 27;
            String maxZoomString = parameters.get("max-zoom");
            if (maxZoomString != null) {
                try {
                    int tenMaxZoom = (int)(10.0D * Double.parseDouble(maxZoomString));
                    if (tenDesiredZoom > tenMaxZoom) {
                        tenDesiredZoom = tenMaxZoom;
                    }
                } catch (NumberFormatException var13) {
                    Log.e(TAG, "Bad max-zoom: " + maxZoomString);
                }
            }

            String takingPictureZoomMaxString = parameters.get("taking-picture-zoom-max");
            if (takingPictureZoomMaxString != null) {
                try {
                    int tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString);
                    if (tenDesiredZoom > tenMaxZoom) {
                        tenDesiredZoom = tenMaxZoom;
                    }
                } catch (NumberFormatException var12) {
                    Log.e(TAG, "Bad taking-picture-zoom-max: " + takingPictureZoomMaxString);
                }
            }

            String motZoomValuesString = parameters.get("mot-zoom-values");
            if (motZoomValuesString != null) {
                tenDesiredZoom = findBestMotZoomValue(motZoomValuesString, tenDesiredZoom);
            }

            String motZoomStepString = parameters.get("mot-zoom-step");
            if (motZoomStepString != null) {
                try {
                    double motZoomStep = Double.parseDouble(motZoomStepString.trim());
                    int tenZoomStep = (int)(10.0D * motZoomStep);
                    if (tenZoomStep > 1) {
                        tenDesiredZoom -= tenDesiredZoom % tenZoomStep;
                    }
                } catch (NumberFormatException var11) {
                }
            }

            if (maxZoomString != null || motZoomValuesString != null) {
                parameters.set("zoom", String.valueOf((double)tenDesiredZoom / 10.0D));
            }

            if (takingPictureZoomMaxString != null) {
                parameters.set("taking-picture-zoom", tenDesiredZoom);
            }

        }
    }

    public static int getDesiredSharpness() {
        return 30;
    }

    protected void setDisplayOrientation(Camera camera, int angle) {
        try {
            Method downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", Integer.TYPE);
            if (downPolymorphic != null) {
                downPolymorphic.invoke(camera, angle);
            }
        } catch (Exception var5) {
        }

    }

    synchronized FlashLightStatus getFlashLightStatus(Camera camera) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters != null) {
                String flashMode = camera.getParameters().getFlashMode();
                return flashMode == null || !"on".equals(flashMode) && !"torch".equals(flashMode) ? FlashLightStatus.OFF : FlashLightStatus.ON;
            }
        }

        return FlashLightStatus.OFF;
    }

    synchronized void setFlashLight(Camera camera, boolean OnOrOff) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            String flashMode;
            if (OnOrOff) {
                flashMode = this.findSettableValue(parameters.getSupportedFlashModes(), "torch", "on");
            } else {
                flashMode = this.findSettableValue(parameters.getSupportedFlashModes(), "off");
            }

            if (flashMode != null) {
                parameters.setFlashMode(flashMode);
            }

            camera.setParameters(parameters);
        }

    }

    private void setFlashLight(Camera.Parameters parameters) {
        if (Build.MODEL.contains("Behold II") && QRCameraManager.SDK_INT == 3) {
            parameters.set("flash-value", 1);
        } else {
            parameters.set("flash-value", 2);
        }

        parameters.set("flash-mode", "off");
    }

    private String findSettableValue(Collection<String> supportedValues, String... desiredValues) {
        String result = null;
        if (supportedValues != null) {
            String[] var4 = desiredValues;
            int var5 = desiredValues.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                String desiredValue = var4[var6];
                if (supportedValues.contains(desiredValue)) {
                    result = desiredValue;
                    break;
                }
            }
        }

        return result;
    }

    public static enum FlashLightStatus {
        ON,
        OFF;

        private FlashLightStatus() {
        }
    }
}

