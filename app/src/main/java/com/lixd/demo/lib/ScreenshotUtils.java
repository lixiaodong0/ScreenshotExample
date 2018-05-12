package com.lixd.demo.lib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 截图工具类
 */

public class ScreenshotUtils {
    private ScreenshotUtils() {
    }

    /**
     * 获取View在手机屏幕渲染的图片
     *
     * @param view
     * @return 图片
     */
    public static final Bitmap getScreenshot(@NonNull View view) {
        if (view == null) {
            return null;
        }
        //1:打开缓存开关
        view.setDrawingCacheEnabled(true);
        //2:获取缓存  此方法需要在主线程调用
        Bitmap drawingCache = view.getDrawingCache();
        //3:拷贝图片
        Bitmap newBitmap = Bitmap.createBitmap(drawingCache);
        //4:关闭缓存开关
        view.setDrawingCacheEnabled(false);
        return newBitmap;
    }

    /**
     * 压缩bitmap
     *
     * @param bitmap 图片
     * @return
     */
    public static final Bitmap compressBitmap(@NonNull Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        //质量压缩方法,这里100表示不压缩,把压缩后的数据存放到os中
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
        int options = 100;
        //循环判断如果压缩后图片是否大于100kb,大于继续压缩
        while (os.toByteArray().length / 1024 > 100) {
            //这里压缩options%,把压缩后的数据存放到baos中
            options -= 10;
            if (options < 10) {
                //最高只能压缩90% options值不能=0,否则会出现异常
                break;
            }
        }
        //将压缩后数据放入ByteArrayInputStream中
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        //把ByteArrayInputStream数据生成图片
        Bitmap compressBitamp = BitmapFactory.decodeStream(is);
        return compressBitamp;
    }

    /**
     * 合并图片
     *
     * @param datas             数据源
     * @param totalBitmapHeight 总的图片高度
     * @return
     */
    public static final Bitmap mergeBitmap(@NonNull List<Bitmap> datas, int totalBitmapHeight) {
        return mergeBitmap(datas, totalBitmapHeight, 0);
    }

    public static final Bitmap mergeBitmap(@NonNull List<Bitmap> datas, int totalBitmapHeight, int remainScrollHeight) {
        if (datas == null || datas.size() <= 0) {
            return null;
        }
        //图纸宽度(因为是截图,图片宽度大小都是一样的)
        int bitmapWidth = datas.get(0).getWidth();
        //图纸高度
        int bitmapHeight = totalBitmapHeight;
        //1:创建图纸
        Bitmap bimap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.RGB_565);
        //2:创建画布,并绑定图纸
        Canvas canvas = new Canvas(bimap);
        //3:创建画笔
        Paint paint = new Paint();
        for (int count = datas.size(), i = 0; i < count; i++) {
            Bitmap data = datas.get(i);
            float left = 0;
            float top = i * data.getHeight();
            Rect src = null;
            RectF des = null;
            /**
             * Rect src = new Rect(); 代表图片矩形范围
             * RectF des = new RectF(); 代表Canvas的矩形范围(显示位置)
             */
            if (i == count - 1 && remainScrollHeight > 0) {
                int srcRectTop = data.getHeight() - remainScrollHeight;
                src = new Rect(0, srcRectTop, data.getWidth(), data.getHeight());
                des = new RectF(left, top, data.getWidth(), top + remainScrollHeight);
            } else {
                src = new Rect(0, 0, data.getWidth(), data.getHeight());
                des = new RectF(left, top, data.getWidth(), top + data.getHeight());
            }
            canvas.drawBitmap(data, src, des, paint);
        }
        return bimap;
    }


    /**
     * 保存图片
     *
     * @param bitmap    图片
     * @param localPath 本地路径
     */
    public static final void saveBitmap(@NonNull Bitmap bitmap, @NonNull String localPath) {
        if (bitmap == null || TextUtils.isEmpty(localPath)) {
            return;
        }
        File file = new File(localPath);
        //拷贝图片,防止破坏原有图片结构
        Bitmap newBitmap = Bitmap.createBitmap(bitmap);
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
