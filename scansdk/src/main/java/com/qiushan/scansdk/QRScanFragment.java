package com.qiushan.scansdk;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public class QRScanFragment extends Fragment {
    private String TAG = "QRScanFragment";
    //预览画面
    private TextureView textureView;
    //扫描框
    private QRFinderView finderView;
    //相机设备
    private CameraDevice cameraDevice;
    //预览画面输出
    private ImageReader imageReader;
    //预览请求
    private CaptureRequest.Builder previewRequestBuilder;
    //扫码成功回调
    private QRScanResultCallback qrScanResultCallback;
    //扫码框区域
    private Rect mFramingRect;
    //预览画面区域
    private Rect mFramingRectInPreview;
    //预览尺寸
    private Size mPreviewSize;
    //预览抓捕进程
    private CameraCaptureSession captureSession;
    //预览画面状态
    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    //相机状态
    private CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
        }
    };
    //预览配置状态
    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            captureSession = session;
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            try {
                //重复请求预览
                session.setRepeatingRequest(previewRequestBuilder.build(), sessionCaptureCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };
    //预览捕获状态
    private CameraCaptureSession.CaptureCallback sessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            captureSession = session;
            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
            if (afState == null || afState == CaptureRequest.CONTROL_AF_STATE_PASSIVE_FOCUSED
                    || (afState == CaptureRequest.CONTROL_AF_STATE_INACTIVE && aeState == CaptureRequest.CONTROL_AE_STATE_CONVERGED)) {
                capturePicture();
            }
        }
    };
    //预览图片完成
    private ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            image.close();
            decodeImageByZxing(data, imageWidth, imageHeight);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_scan, container, false);
        textureView = view.findViewById(R.id.textureView);
        finderView = view.findViewById(R.id.finderView);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        closeCamera();
    }

    /**
     * 开启相机
     */
    private void openCamera() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "请先授予相机使用权限", Toast.LENGTH_LONG).show();
            return;
        }
        CameraManager cameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        String cameraId = getBackCameraId();
        if (TextUtils.isEmpty(cameraId)) {
            Toast.makeText(getContext(), "无法开启相机", Toast.LENGTH_LONG).show();
            return;
        }
        prepareFrame();
        try {
            cameraManager.openCamera(getBackCameraId(), deviceStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭相机
     */
    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
    }

    /**
     * 开始预览
     */
    private void startPreview() {
        try {
            //构建预览请求
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            Surface surface = new Surface(texture);
            //添加显示预览视图
            previewRequestBuilder.addTarget(surface);
            //一直发送预览请求（请求结果将输出到ImageReader）
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取后置相机id
     */
    private String getBackCameraId() {
        CameraManager cameraManager = getCameraManager();
        try {
            String[] ids = cameraManager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                int orientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (orientation == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 相机设备Manager
     */
    private CameraManager getCameraManager() {
        return (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
    }

    /**
     * 通过zxing解码图片
     */
    private void decodeImageByZxing(byte[] imageData, int imageWidth, int imageHeight) {
        Rect rect = new Rect(mFramingRectInPreview);
        MultiFormatReader multiFormatReader = getMultiFormatReader();
        Result result = null;
        PlanarYUVLuminanceSource planarYUVLuminanceSource = new PlanarYUVLuminanceSource(imageData, imageWidth, imageHeight, rect.left, rect.top, rect.width(), rect.height(), false);
        if (planarYUVLuminanceSource != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(planarYUVLuminanceSource));
            try {
                result = multiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException re) {
                Log.e(TAG, ": ", re);
            } finally {
                multiFormatReader.reset();
            }
        }
        handleScanResult(result);
    }

    /**
     * 处理zxing解码结果
     */
    private void handleScanResult(Result result) {
        if (result != null) {
            //扫码成功
            if (qrScanResultCallback != null) {
                qrScanResultCallback.onResult(result.getText());
            }
        } else {
            //扫码不成功，重新再来
            try {
                captureSession.setRepeatingRequest(previewRequestBuilder.build(), sessionCaptureCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取富文本reader
     */
    private MultiFormatReader getMultiFormatReader() {
        MultiFormatReader mMultiFormatReader = new MultiFormatReader();
        Collection<BarcodeFormat> decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
        //指定扫码数据类型
        decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);

        final Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        hints.put(DecodeHintType.CHARACTER_SET, "UTF8");
        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, new QRFinderResultPointCallback(finderView));
        mMultiFormatReader.setHints(hints);
        return mMultiFormatReader;
    }

    /**
     * 抓捕预览画面
     */
    private void capturePicture() {
        if (captureSession == null) {
            return;
        }
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            previewRequestBuilder.addTarget(imageReader.getSurface());
            captureSession.stopRepeating();
            captureSession.capture(previewRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止预览
     */
    private void unlockFocus() {
        if (captureSession == null) {
            return;
        }
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        previewRequestBuilder.removeTarget(imageReader.getSurface());
    }

    /**
     * 准备工作，调整扫码框大小
     */
    private void prepareFrame() {
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        setUpCameraOutputs(viewWidth, viewHeight);
        initFramingRect(viewWidth, viewHeight);
        initFramingRectInPreview(viewWidth, viewHeight);
        finderView.setFrame(mFramingRect);
    }

    /**
     * 扫码框区域（宽高比3：4）
     */
    private void initFramingRect(int viewWidth, int viewHeight) {
        int width = viewWidth * 3 / 4;
        int height = viewHeight * 3 / 4;
        int leftOffset = (viewWidth - width) / 2;
        int topOffset = (viewHeight - height) / 2;
        mFramingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
    }

    /**
     * 预览输出区域
     */
    private void initFramingRectInPreview(int viewWidth, int viewHeight) {
        Rect rect = new Rect(mFramingRect);
        rect.left = rect.left * mPreviewSize.getWidth() / viewWidth;
        rect.right = rect.right * mPreviewSize.getWidth() / viewWidth;
        rect.top = rect.top * mPreviewSize.getHeight() / viewHeight;
        rect.bottom = rect.bottom * mPreviewSize.getHeight() / viewHeight;
        mFramingRectInPreview = rect;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int viewWidth, int viewHeight) {
        try {
            CameraCharacteristics characteristics = getCameraManager().getCameraCharacteristics(getBackCameraId());
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //当前相机设备支持的预览尺寸
            Size[] supportedSizes = map.getOutputSizes(SurfaceTexture.class);
            //屏幕方向
            int windowRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            //相机方向
            int cameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            //是否需要根据屏幕方向调整相机旋转方向（当屏幕方向和相机方向不一致时需要）
            boolean needSwappedDimensions = false;
            switch (windowRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (cameraOrientation == 90 || cameraOrientation == 270) {
                        needSwappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (cameraOrientation == 0 || cameraOrientation == 180) {
                        needSwappedDimensions = true;
                    }
                    break;
            }
            int rotatedPreviewWidth = viewWidth;
            int rotatedPreviewHeight = viewHeight;
            if (needSwappedDimensions) {
                rotatedPreviewWidth = viewHeight;
                rotatedPreviewHeight = viewWidth;
            }

            //从相机支持的所有预览尺寸中选择，不超过view寸尺的最大尺寸，作为实际预览尺寸
            mPreviewSize = new Size(rotatedPreviewWidth, rotatedPreviewHeight);
            float ration = (float) rotatedPreviewWidth / rotatedPreviewHeight;
            for (Size option : supportedSizes) {
                if ((float) option.getWidth() / option.getHeight() == ration
                        && option.getWidth() <= rotatedPreviewWidth
                        && option.getHeight() <= rotatedPreviewHeight) {
                    mPreviewSize = option;
                    break;
                }
            }

            //预览输出到ImageReader中
            imageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, /*maxImages*/2);
            //监听预览输出
            imageReader.setOnImageAvailableListener(imageAvailableListener, null);
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开启闪光灯
     */
    public void openFlashLight() {
        previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
        try {
            captureSession.setRepeatingRequest(previewRequestBuilder.build(), sessionCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭闪光灯
     */
    public void closeFlashLight() {
        previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        try {
            captureSession.setRepeatingRequest(previewRequestBuilder.build(), sessionCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置扫码成功回调
     */
    public void setQrScanResultCallback(QRScanResultCallback qrScanResultCallback) {
        this.qrScanResultCallback = qrScanResultCallback;
    }
}
