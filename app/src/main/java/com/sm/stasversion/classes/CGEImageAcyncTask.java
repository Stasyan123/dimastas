package com.sm.stasversion.classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.google.gson.Gson;
import com.sm.stasversion.imagepicker.model.Asset;
import com.sm.stasversion.interfaces.CGEImageResponseHandler;

import org.wysaid.myUtils.ImageUtil;
import org.wysaid.nativePort.CGEFFmpegNativeLibrary;
import org.wysaid.nativePort.CGENativeLibrary;
import org.wysaid.texUtils.CropInfo;

import java.util.List;

public class CGEImageAcyncTask extends AsyncTask<Void, String, Boolean> {
    Context ctx;
    Bitmap resultBitmap;
    Asset el;
    String path;
    Float intensity;
    Integer id;
    CGEImageResponseHandler responseHandler;

    public CGEImageAcyncTask (Context ctx, String _path, Asset _el, CGEImageResponseHandler responseHandler, Integer id) {
        this.ctx = ctx;
        path = _path;
        el = _el;
        this.responseHandler = responseHandler;
        this.id = id;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            responseHandler.onProgress(el.getId(), 1);

            FutureTarget<Bitmap> futureBitmap = Glide.with(ctx)
                    .asBitmap()
                    .load(el.getPath())
                    .submit();

            Bitmap original = futureBitmap.get();

            responseHandler.onProgress(el.getId(), 2);

            if(!el.getCrop().isEmpty()) {
                CropInfo cropInfo = new Gson().fromJson(el.getCrop(), CropInfo.class);
                original = serializedConfigs.cropImage(original, cropInfo);
            }

            responseHandler.onProgress(el.getId(), 3);

            if(!el.getCorrection().isEmpty()) {
                List<AdjustConfig> _configs = serializedConfigs.decryptConfigsStatic(el.getCorrection());
                String rules = serializedConfigs.calculateRules(_configs);

                original = CGENativeLibrary.filterImage_MultipleEffects(original, rules, _configs.get(0).intensity, (int)el.getId());
            }

            responseHandler.onProgress(el.getId(), 4);

            responseHandler.onSuccess(ImageUtil.saveBitmap(original));
        } catch (Exception e) {

        }

        return true;
    }
}
