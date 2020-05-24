/*
 * Created by Nguyen Hoang Lam
 * Date: ${DATE}
 */

package com.sm.stasversion;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.app.AppCompatActivity;


import com.sm.stasversion.imagepicker.model.Asset;
import com.sm.stasversion.imagepicker.model.Config;
import com.sm.stasversion.imagepicker.model.Image;
import com.sm.stasversion.imagepicker.model.Video;
import com.sm.stasversion.imagepicker.ui.imagepicker.AssetPicker;
import com.sm.stasversion.imagepicker.util.Extensions_FileKt;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by hoanglam on 8/4/16.
 */
public class MainActivityPicker extends AppCompatActivity {

    private AssetAdapter adapter;
    private ArrayList<Asset> assets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_picker);

        ConstraintLayout root = findViewById(R.id.rootView);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                start();
            }
        }, 1200);

        // Register the onClick listener with the implementation above
        root.setOnClickListener(mCorkyListener);
    }

    private View.OnClickListener mCorkyListener = new View.OnClickListener() {
        public void onClick(View v) {
            start();
        }
    };

    private void start() {
        AssetPicker.with(this)
                .setFolderMode(true)
                .setIncludeVideos(true)
                .setVideoOrImagePickerTitle("Capture image or video")
                .setCameraOnly(false)
                .setFolderTitle(getResources().getString(R.string.all))
                .setMultipleMode(false)
                .setSelectedImages(assets)
                .setMaxSize(10)
                .setBackgroundColor("#000000")
                .setToolbarColor("#ffffff")
                .setToolbarTextColor("#000000")
                .setAlwaysShowDoneButton(false)
                .setRequestCode(100)
                .setKeepScreenOn(true)
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Config.RC_PICK_ASSETS && resultCode == RESULT_OK && data != null) {
            Intent intent = new Intent(MainActivityPicker.this, EditImageActivity.class);
            assets = data.getParcelableArrayListExtra(Config.EXTRA_ASSETS);

            Uri uri = Uri.fromFile(new File(assets.get(0).getPath()));

            File f = new File(uri.getPath());
            if (Extensions_FileKt.isImageFile(f)) { // If file is an Image

            } else if (Extensions_FileKt.isVideoFile(f)) { // If file is a Video
                intent = new Intent(MainActivityPicker.this, NewVideoOverviewActivity.class);
            }

            intent.putExtra("file", uri);

            startActivity(intent);
        }
        //super.onActivityResult(requestCode, resultCode, data);
    }
}
