package com.sm.stasversion.imagepicker.ui.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.sm.stasversion.R;
import com.sm.stasversion.imagepicker.model.Asset;
import com.sm.stasversion.imagepicker.model.Config;
import com.sm.stasversion.imagepicker.ui.common.BasePresenter;

import java.util.List;

/**
 * Created by hoanglam on 8/22/17.
 */

public class CameraPresenter extends BasePresenter<CameraView> {

    private CameraModule cameraModule = new DefaultCameraModule();

    public CameraPresenter() {
    }


    void captureImage(Activity activity, Config config, int requestCode) {
        Context context = activity.getApplicationContext();
        Intent intent = cameraModule.getCameraIntent(activity, config);
        if (intent == null) {
            Toast.makeText(context, context.getString(R.string.imagepicker_error_create_image_file), Toast.LENGTH_LONG).show();
            return;
        }
        activity.startActivityForResult(intent, requestCode);
    }

    public void finishCaptureAsset(Context context, Intent data, final Config config) {
        cameraModule.getImage(context, data, new OnAssetReadyListener() {
            @Override
            public void onAssetReady(List<Asset> assets) {
                getView().finishPickAssets(assets);
            }
        });
    }
}
