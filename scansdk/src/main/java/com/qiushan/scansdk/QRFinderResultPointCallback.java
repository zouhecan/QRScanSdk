package com.qiushan.scansdk;

import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

public final class QRFinderResultPointCallback implements ResultPointCallback {
    private final QRFinderView viewfinderView;

    public QRFinderResultPointCallback(QRFinderView viewfinderView) {
        this.viewfinderView = viewfinderView;
    }

    public void foundPossibleResultPoint(ResultPoint point) {
        this.viewfinderView.addPossibleResultPoint(point);
    }
}
