package com.sm.stasversion.imagepicker.ui.camera;

import android.content.Context;
import android.content.Intent;

import com.sm.stasversion.imagepicker.model.Config;

/**
 * Created by hoanglam on 8/18/17.
 */

public interface CameraModule {
    Intent getCameraIntent(Context context, Config config);

    void getImage(Context context, Intent intent, OnAssetReadyListener imageReadyListener);
}