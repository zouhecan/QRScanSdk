package com.qiushan.scansdk;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.Hashtable;

final class DecodeHandler extends Handler {
    private static final String TAG = DecodeHandler.class.getSimpleName();
    private final QRScanActivity activity;
    private final MultiFormatReader multiFormatReader = new MultiFormatReader();

    DecodeHandler(QRScanActivity activity, Hashtable<DecodeHintType, Object> hints) {
        this.multiFormatReader.setHints(hints);
        this.activity = activity;
    }

    public void handleMessage(Message message) {
        int id = message.what;
        if (id == R.id.decode) {
            this.decode((byte[]) ((byte[]) message.obj), message.arg1, message.arg2);
        } else if (id == R.id.quit) {
            Looper.myLooper().quit();
        }
    }

    private void decode(byte[] data, int width, int height) {
        long start = System.currentTimeMillis();
        Result rawResult = null;
        byte[] rotatedData = new byte[data.length];

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                rotatedData[x * height + height - y - 1] = data[x + y * width];
            }
        }

        PlanarYUVLuminanceSource source = QRCameraManager.getInstance(this.activity).buildLuminanceSource(rotatedData, height, width);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            rawResult = this.multiFormatReader.decodeWithState(bitmap);
        } catch (ReaderException var17) {
        } finally {
            this.multiFormatReader.reset();
        }

        if (rawResult != null) {
            long end = System.currentTimeMillis();
            Log.d(TAG, "Found barcode (" + (end - start) + " ms):\n" + rawResult.toString());
            Message message = Message.obtain(this.activity.getHandler(), R.id.decode_succeeded, rawResult);
            Bundle bundle = new Bundle();
            bundle.putParcelable("barcode_bitmap", renderCroppedGreyscaleBitmap(source));
            message.setData(bundle);
            message.sendToTarget();
        } else {
            Message message = Message.obtain(this.activity.getHandler(), R.id.decode_failed);
            message.sendToTarget();
        }

    }

    public Bitmap renderCroppedGreyscaleBitmap(PlanarYUVLuminanceSource source) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        return Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
    }
}
