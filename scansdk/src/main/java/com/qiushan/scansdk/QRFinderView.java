package com.qiushan.scansdk;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;

import java.util.Collection;
import java.util.HashSet;

public final class QRFinderView extends View {
    private static final int OPAQUE = 255;
    private static final int CORNER_TALL = DeviceUtil.getPixelFromDip(26.0F);
    private static final int CORNER_FAT = DeviceUtil.getPixelFromDip(1.0F);
    private static final int CORNER_MARGIN = DeviceUtil.getPixelFromDip(0.0F);
    private static final long ANIMATION_DELAY = 14L;
    private static final int SPEEN_DISTANCE = 3;
    private static final int MIDDLE_LINE_WIDTH = 2;
    private static final int MIDDLE_LINE_PADDING = 4;
    private Paint paint = new Paint();
    private final int maskColor;
    Bitmap scanline;
    private int slideTop;
    boolean isFirst;
    private final int resultPointColor;
    private Collection<ResultPoint> possibleResultPoints = new HashSet(5);
    private Collection<ResultPoint> lastPossibleResultPoints;

    public QRFinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources resources = this.getResources();
        this.maskColor = resources.getColor(R.color.baseqrcode_viewfinder_mask);
        this.resultPointColor = resources.getColor(R.color.baseqrcode_possible_result_points);
    }

    public void onDraw(Canvas canvas) {
        Rect frame = QRCameraManager.getInstance(this.getContext()).getFramingRect();
        if (frame != null) {
            if (this.scanline == null) {
                Bitmap bm = BitmapFactory.decodeResource(this.getResources(), R.drawable.scan_line_icon);
                if (bm != null) {
                    this.scanline = Bitmap.createScaledBitmap(bm, frame.width(), DeviceUtil.getPixelFromDip(7.0F), false);
                    if (bm != this.scanline) {
                        bm.recycle();
                    }
                }
            }

            if (!this.isFirst) {
                this.isFirst = true;
                this.slideTop = frame.top;
            }

            int width = canvas.getWidth();
            int height = canvas.getHeight();
            this.paint.setColor(this.maskColor);
            canvas.drawRect(0.0F, 0.0F, (float)width, (float)frame.top, this.paint);
            canvas.drawRect(0.0F, (float)frame.top, (float)frame.left, (float)(frame.bottom + 1), this.paint);
            canvas.drawRect((float)(frame.right + 1), (float)frame.top, (float)width, (float)(frame.bottom + 1), this.paint);
            canvas.drawRect(0.0F, (float)(frame.bottom + 1), (float)width, (float)height, this.paint);
            this.paint.setColor(-1);
            int t = DeviceUtil.getPixelFromDip(1.0F);
            canvas.drawRect((float)frame.left, (float)frame.top, (float)(frame.left + t), (float)frame.bottom, this.paint);
            canvas.drawRect((float)frame.left, (float)frame.top, (float)frame.right, (float)(frame.top + t), this.paint);
            canvas.drawRect((float)(frame.right - t), (float)frame.top, (float)frame.right, (float)frame.bottom, this.paint);
            canvas.drawRect((float)frame.left, (float)(frame.bottom - t), (float)frame.right, (float)frame.bottom, this.paint);
            this.paint.setColor(-10100478);
            canvas.drawRect((float)(frame.left - CORNER_MARGIN - CORNER_FAT), (float)(frame.top - CORNER_MARGIN), (float)(frame.left - CORNER_MARGIN), (float)(frame.top - CORNER_MARGIN + CORNER_TALL), this.paint);
            canvas.drawRect((float)(frame.left - CORNER_MARGIN - CORNER_FAT), (float)(frame.top - CORNER_MARGIN - CORNER_FAT), (float)(frame.left - CORNER_MARGIN - CORNER_FAT + CORNER_TALL), (float)(frame.top - CORNER_MARGIN), this.paint);
            canvas.drawRect((float)(frame.right + CORNER_MARGIN), (float)(frame.top - CORNER_MARGIN), (float)(frame.right + CORNER_MARGIN + CORNER_FAT), (float)(frame.top - CORNER_MARGIN + CORNER_TALL), this.paint);
            canvas.drawRect((float)(frame.right + CORNER_MARGIN + CORNER_FAT - CORNER_TALL), (float)(frame.top - CORNER_MARGIN - CORNER_FAT), (float)(frame.right + CORNER_MARGIN + CORNER_FAT), (float)(frame.top - CORNER_MARGIN), this.paint);
            canvas.drawRect((float)(frame.left - CORNER_MARGIN - CORNER_FAT), (float)(frame.bottom + CORNER_MARGIN - CORNER_TALL), (float)(frame.left - CORNER_MARGIN), (float)(frame.bottom + CORNER_MARGIN), this.paint);
            canvas.drawRect((float)(frame.left - CORNER_MARGIN - CORNER_FAT), (float)(frame.bottom + CORNER_MARGIN), (float)(frame.left - CORNER_MARGIN - CORNER_FAT + CORNER_TALL), (float)(frame.bottom + CORNER_MARGIN + CORNER_FAT), this.paint);
            canvas.drawRect((float)(frame.right + CORNER_MARGIN), (float)(frame.bottom + CORNER_MARGIN - CORNER_TALL), (float)(frame.right + CORNER_MARGIN + CORNER_FAT), (float)(frame.bottom + CORNER_MARGIN), this.paint);
            canvas.drawRect((float)(frame.right + CORNER_MARGIN + CORNER_FAT - CORNER_TALL), (float)(frame.bottom + CORNER_MARGIN), (float)(frame.right + CORNER_MARGIN + CORNER_FAT), (float)(frame.bottom + CORNER_MARGIN + CORNER_FAT), this.paint);
            this.slideTop += 3;
            if (this.slideTop >= frame.bottom - 20) {
                this.slideTop = frame.top;
            }

            if (this.scanline != null) {
                canvas.drawBitmap(this.scanline, (float)frame.left, (float)this.slideTop, (Paint)null);
            } else {
                canvas.drawRect((float)(frame.left + 4), (float)this.slideTop, (float)(frame.right - 4), (float)(this.slideTop + 2), this.paint);
            }

            this.paint.setColor(-1);
            this.paint.setTextSize((float)DeviceUtil.getPixelFromDip(15.0F));
            String desText = this.getResources().getString(R.string.baseqrcode_scan_guide);
            float desTextWidth = this.paint.measureText(desText);
            canvas.drawText(desText, ((float)width - desTextWidth) / 2.0F, (float)(frame.top - DeviceUtil.getPixelFromDip(20.0F)), this.paint);
            Collection<ResultPoint> currentPossible = this.possibleResultPoints;
            Collection<ResultPoint> currentLast = this.lastPossibleResultPoints;
            if (currentPossible.isEmpty()) {
                this.lastPossibleResultPoints = null;
            } else {
                this.possibleResultPoints = new HashSet(5);
                this.lastPossibleResultPoints = currentPossible;
                this.paint.setAlpha(255);
                this.paint.setColor(this.resultPointColor);
            }

            if (currentLast != null) {
                this.paint.setAlpha(127);
                this.paint.setColor(this.resultPointColor);
            }

            this.postInvalidateDelayed(14L, frame.left, frame.top, frame.right, frame.bottom);
        }
    }

    public void drawViewfinder() {
        this.invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        this.possibleResultPoints.add(point);
    }
}

