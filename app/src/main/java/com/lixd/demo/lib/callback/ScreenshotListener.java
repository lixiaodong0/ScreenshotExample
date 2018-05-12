package com.lixd.demo.lib.callback;

import android.graphics.Bitmap;

/**
 * 截图监听器
 */
public interface ScreenshotListener {
    /**
     * 截图成功
     *
     * @param bitmap           图片
     * @param isLongScreenshot 是否是长截图
     */
    void onSuccess(Bitmap bitmap, boolean isLongScreenshot);

    /**
     * 截图失败
     *
     * @param code      失败码
     * @param errorInfo 错误信息
     */
    void onFail(int code, String errorInfo);

    /**
     * 当开始截图的回调,可以显示进度弹窗
     */
    void onPreStart();
}
