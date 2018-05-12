package com.lixd.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.lixd.demo.lib.Screenshot;
import com.lixd.demo.lib.callback.ScreenshotListener;
import com.lixd.demo.lib.callback.SimpleScreenshotListener;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private Screenshot screenshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.web_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.btn, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Intent intent = new Intent();
        intent.setClass(MainActivity.this, PreviewActivity.class);
        if (item.getItemId() == R.id.menu_btn_one) {
            new Screenshot.Builder(this)
                    .setTarget(webView)
                    .setScreenshotListener(new SimpleScreenshotListener() {
                        @Override
                        public void onSuccess(Bitmap bitmap, boolean isLongScreenshot) {
                            BitmapUtils.sBitmap = bitmap;
                            startActivity(intent);
                        }
                    })
                    .build()
                    .start();
            return true;
        }

        if (item.getItemId() == R.id.menu_btn_two) {
            screenshot = new Screenshot.Builder(this)
                    .setTarget(webView)
                    .setScreenshotType(true)
                    .setFilePath(new File(getCacheDir(), "bitmap.jpg").getAbsolutePath())
                    .setScreenshotListener(new ScreenshotListener() {
                        @Override
                        public void onSuccess(Bitmap bitmap, boolean isLongScreenshot) {
                            Log.e("MainActivity", "onSuccess");
                            BitmapUtils.sBitmap = bitmap;
                            startActivity(intent);
                        }

                        @Override
                        public void onFail(int code, String errorInfo) {
                            Log.e("MainActivity", "onFail = " + errorInfo);
                        }

                        @Override
                        public void onPreStart() {
                            Log.e("MainActivity", "onPreStart");
                        }
                    })
                    .build();
            screenshot.start();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (screenshot != null) {
            screenshot.destroy();
        }
    }

    void init() {
        webView.setWebViewClient(new WebViewClient(){

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        String url = "file:///android_asset/test.html";
        webView.loadUrl(url);
    }
}
