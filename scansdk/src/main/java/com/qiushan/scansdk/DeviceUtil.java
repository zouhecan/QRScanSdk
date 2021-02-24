package com.qiushan.scansdk;

import android.util.DisplayMetrics;
import android.util.TypedValue;

class DeviceUtil {

    /**
     * Dip转换为实际屏幕的像素值
     *
     * @param dm  设备显示对象描述
     * @param dip dip值
     * @return 匹配当前屏幕的像素值
     */
    public static int getPixelFromDip(DisplayMetrics dm, float dip) {
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, dm) + 0.5f);
    }

    public static int getPixelFromDip(float dip) {
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, FoundationContext.context.getResources().getDisplayMetrics()) + 0.5f);
    }
}
