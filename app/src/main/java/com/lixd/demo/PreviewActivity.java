package com.lixd.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

public class PreviewActivity extends AppCompatActivity {
    public static final String TAG = PreviewActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ImageView imgPreview = (ImageView) findViewById(R.id.img_preview);

        if (BitmapUtils.sBitmap != null) {
            imgPreview.setImageBitmap(BitmapUtils.sBitmap);
            Log.d(TAG, "bitmap success");
        } else {
            Log.d(TAG, "bitmap is null");
        }
    }
}
