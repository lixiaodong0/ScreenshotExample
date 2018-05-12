package com.lixd.demo.lib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.webkit.WebView;

import com.lixd.demo.lib.callback.ScreenshotListener;
import com.lixd.demo.lib.callback.ScrollListener;

import java.util.ArrayList;
import java.util.List;


public class Screenshot {
    private static final String TAG = Screenshot.class.getCanonicalName();
    private static final Object LOCK = new Object();
    //错误码
    public static final int PARAMS_ERROR = 1001; //参数错误

    //状态码
    private static final int SUCCESS_STATE = 100;       //成功状态
    private static final int FAIL_STATE = 200;          //失败状态
    private static final int SCROLL_STATE = 300;        //滚动状态
    private static final int START_STATE = 400;         //开始截图状态

    //上下文
    private Context context;
    //截图的View
    private View view;
    //截图保存的本地路径
    private String filePath = "";
    //截图的方式(true=长截图 fasle=截图) 默认为false
    private boolean isLongScreenshot = false;
    //WebView内容的高度
    private int contentHeight;
    //WebView控件的高度
    private int height;
    //滚动截屏的次数(长截图才会使用)
    private int totalScrollCount;
    //剩余滚动的高度(长截图才会使用)
    private int remainScrollHeight;
    //临时的存放图片变量
    private Bitmap tempBitmap;
    //截图handler
    private ScreenshotHandler handler;
    //截图事件监听
    private ScreenshotListener listener;
    //长截图任务
    private LongScreenshotRunabable runabable;
    //滚动事件监听
    private ScrollListener scrollListener = new ScrollListener.Adapter() {
        @Override
        public void onSuccess() {
            //滚动成功之后,获取截图
            tempBitmap = ScreenshotUtils.getScreenshot(view);
        }
    };

    private Screenshot(Builder builder) {
        context = builder.context;
        view = builder.view;
        filePath = builder.filePath;
        isLongScreenshot = builder.isLongScreenshot;
        listener = builder.listener;

        handler = new ScreenshotHandler(context.getMainLooper());
    }

    private String checkParams() {
        if (context == null) {
            return "context not null";
        }

        if (view == null) {
            return "target view not null";
        }
        return "";
    }

    public void start() {
        String failInfo = checkParams();
        if (!TextUtils.isEmpty(failInfo)) {
            handler.sendFailMessage(PARAMS_ERROR, failInfo);
            return;
        }
        Log.d(TAG, "------------ start screenshot ------------");
        if (!isLongScreenshot) {
            //截图
            screenshot();
        } else {
            //长截图
            preLongScreenshot();
        }
    }

    /**
     * 截图
     */
    private void screenshot() {
        //获取截图
        Bitmap bitmap = ScreenshotUtils.getScreenshot(view);
        //压缩截图
        Bitmap compressBitmap = ScreenshotUtils.compressBitmap(bitmap);
        savaBitmap(compressBitmap);
        //回调
        handler.sendSuccessMessage(compressBitmap);
    }

    /**
     * 在长截图之前的准备工作
     */
    private void preLongScreenshot() {
        handler.sendStartMessage();
        //1:发起测量
        view.measure(0, 0);
        //2:获取测量后高度 == Webview内容的高度
        contentHeight = view.getMeasuredHeight();
        //3:获取Webview控件的高度
        height = view.getHeight();
        //4:计算滚动次数
        totalScrollCount = contentHeight / height;
        //5:有余数(剩余高度)的情况下
        remainScrollHeight = contentHeight - (totalScrollCount * height);

        Log.d(TAG, "WebView内容高度: " + contentHeight);
        Log.d(TAG, "WebView控件高度: " + height);
        Log.d(TAG, "WebView滚动次数: " + totalScrollCount);
        Log.d(TAG, "WebView剩余高度: " + remainScrollHeight);
        longScreenshot();
    }

    /**
     * 长截图
     */
    private void longScreenshot() {
        runabable = new LongScreenshotRunabable();
        Thread thread = new Thread(runabable);
        thread.start();
    }


    /**
     * 保存图片到本地
     *
     * @param bitmap
     */
    private void savaBitmap(Bitmap bitmap) {
        if (!TextUtils.isEmpty(filePath)) {
            //保存图片
            ScreenshotUtils.saveBitmap(bitmap, filePath);
            Log.d(TAG, "filePath: " + filePath);
        }
    }

    /**
     * 防止WebView滚动条影响效果
     *
     * @param state 当前状态
     */
    private void switchWebViewScrollBar(int state) {
        if (view instanceof WebView) {
            WebView webView = (WebView) view;
            boolean isEnable = webView.isVerticalScrollBarEnabled();
            if (state == START_STATE && isEnable) {
                webView.setVerticalScrollBarEnabled(false);
            } else {
                webView.setVerticalScrollBarEnabled(true);
            }
        }
    }

    /**
     * 销毁,防止内存泄漏
     */
    public void destroy() {
        tempBitmap = null;

        if (runabable != null) {
            //更改标志位,让线程安全退出
            runabable.isDestory = true;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

    }

    /**
     * 执行滚动的高度动画
     *
     * @param scrollHeight
     */
    private void startScrollAnimat(final int scrollHeight) {
        final int lastScrollHeight = view.getScrollY();
        //scrollHeight = 0,证明是第一次截图,那就无需滚动控件
        if (scrollHeight <= 0) {
            synchronized (LOCK) {
                scrollListener.onSuccess();
                //唤醒LongScreenshotRunabable线程继续工作
                Log.d(TAG, "主线程滚动截图完毕,环境LongScreenshotRunabable继续工作");
                LOCK.notify();
            }
            return;
        }
        ValueAnimator scrollAnimator = ValueAnimator.ofInt(0, scrollHeight);
        scrollAnimator.setInterpolator(new LinearInterpolator());
        scrollAnimator.setDuration(1000);
        scrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                view.scrollTo(0, value + lastScrollHeight);
            }
        });
        scrollAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                synchronized (LOCK) {
                    scrollListener.onSuccess();
                    //唤醒LongScreenshotRunabable线程继续工作
                    Log.d(TAG, "主线程滚动截图完毕,环境LongScreenshotRunabable继续工作");
                    LOCK.notify();
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                scrollListener.onPreScroll();
            }
        });
        scrollAnimator.start();
    }

    class LongScreenshotRunabable implements Runnable {
        //控制线程退出的标志位.
        private boolean isDestory = false;

        @Override
        public void run() {
            synchronized (LOCK) {
                //保存图片的集合
                List<Bitmap> cacheBitmaps = new ArrayList<>();
                int count = totalScrollCount;
                //如果有剩余高度,+1次滚动截屏
                if (remainScrollHeight > 0) {
                    count++;
                }

                for (int i = 0; i < count; i++) {
                    if (isDestory) {
                        break;
                    }

                    if (i == 0) {
                        //通知Webview滚动
                        handler.sendScrollMessage(0);
                    } else {
                        //通知Webview滚动
                        handler.sendScrollMessage(height);
                    }


                    try {
                        Log.d(TAG, "当前线程阻塞,等待主(UI)线程滚动截图");
                        /**
                         *  这里暂时没有想到更完美的解决办法
                         *  主线程于子线程通信是 wait,notify来实现的.
                         *  如果你有更好想法,请告知我!
                         */
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //压缩图片
                    Bitmap bitmap = ScreenshotUtils.compressBitmap(tempBitmap);
                    cacheBitmaps.add(bitmap);
                }

                if (!isDestory) {
                    //合并图片
                    Bitmap bitmap = ScreenshotUtils.mergeBitmap(cacheBitmaps, contentHeight, remainScrollHeight);
                    Log.d(TAG, "合并图片成功");
                    savaBitmap(bitmap);
                    //回调成功
                    handler.sendSuccessMessage(bitmap);
                }
            }
        }
    }

    public static class Builder {

        private Context context;
        private View view;
        private String filePath;
        private boolean isLongScreenshot;
        private ScreenshotListener listener;

        public Builder(@NonNull Context context) {
            this.context = context;
        }

        public Builder setTarget(View view) {
            this.view = view;
            return this;
        }

        public Builder setScreenshotType(boolean isLongScreenshot) {
            this.isLongScreenshot = isLongScreenshot;
            return this;
        }

        public Builder setFilePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder setScreenshotListener(ScreenshotListener listener) {
            this.listener = listener;
            return this;
        }

        public Screenshot build() {
            return new Screenshot(this);
        }
    }

    class ScreenshotHandler extends android.os.Handler {
        public ScreenshotHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START_STATE:
                    if (listener != null) {
                        listener.onPreStart();
                    }
                    switchWebViewScrollBar(msg.what);
                    break;
                case SUCCESS_STATE:
                    Bitmap bitmap = (Bitmap) msg.obj;
                    if (listener != null) {
                        listener.onSuccess(bitmap, isLongScreenshot);
                    }
                    switchWebViewScrollBar(msg.what);
                    Log.d(TAG, "------------ finish screenshot ------------");
                    break;
                case FAIL_STATE:
                    int code = msg.arg1;
                    String errorInfo = (String) msg.obj;
                    if (listener != null) {
                        listener.onFail(code, errorInfo);
                    }
                    switchWebViewScrollBar(msg.what);
                    break;
                case SCROLL_STATE:
                    int scrollHeight = msg.arg1;
                    startScrollAnimat(scrollHeight);
                    break;
            }
        }

        /**
         * 发送滚动消息
         *
         * @param scrollHeight 滚动的高度
         */
        public void sendScrollMessage(int scrollHeight) {
            Message msg = this.obtainMessage(SCROLL_STATE);
            msg.arg1 = scrollHeight;
            this.sendMessage(msg);
        }

        /**
         * 发送成功消息
         *
         * @param bitmap 图片
         */
        public void sendSuccessMessage(Bitmap bitmap) {
            Message msg = this.obtainMessage(SUCCESS_STATE);
            msg.obj = bitmap;
            this.sendMessage(msg);
        }

        /**
         * 发送失败消息
         *
         * @param code      失败码
         * @param errorInfo 失败信息
         */
        public void sendFailMessage(int code, String errorInfo) {
            Message msg = this.obtainMessage(SUCCESS_STATE);
            msg.arg1 = code;
            msg.obj = errorInfo;
            this.sendMessage(msg);
        }

        /**
         * 发送开始截图消息
         */
        public void sendStartMessage() {
            this.obtainMessage(START_STATE).sendToTarget();
        }
    }
}
