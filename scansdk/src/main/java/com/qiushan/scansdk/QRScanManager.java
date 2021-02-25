package com.qiushan.scansdk;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class QRScanManager {

    private static QRScanResultCallback callback;

    /**
     * 执行扫码
     */
    public static void startScan(Context context, QRScanResultCallback callback) {
        if (context == null) {
            return;
        }
        FoundationContext.context = context;
        QRScanManager.callback = callback;
        context.startActivity(new Intent(context, QRScanActivity.class));
    }

    static void sendScanResult(String result) {
        if (!TextUtils.isEmpty(result)) {
            if (callback != null) {
                callback.onResult(result);
            }
        }
        removeCallback();
    }

    static void removeCallback() {
        callback = null;
    }
}
