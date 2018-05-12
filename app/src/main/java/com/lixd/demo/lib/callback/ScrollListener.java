package com.lixd.demo.lib.callback;

/**
 * 滚动监听,用于监听滚动View是否滚动指定距离
 */

public interface ScrollListener {
    /**
     * 滚动成功

     */
    void onSuccess();

    /**
     * 当开始滚动
     */
    void onPreScroll();

    /**
     * 滚动失败
     */
    void onFail();


    abstract class Adapter implements ScrollListener {

        @Override
        public void onPreScroll() {

        }

        @Override
        public void onFail() {

        }
    }
}

