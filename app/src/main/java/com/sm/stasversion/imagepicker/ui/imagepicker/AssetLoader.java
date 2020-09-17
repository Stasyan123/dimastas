package com.sm.stasversion.imagepicker.ui.imagepicker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.sm.stasversion.R;
import com.sm.stasversion.classes.AdjustConfig;
import com.sm.stasversion.classes.DBConfig;
import com.sm.stasversion.imagepicker.model.Asset;
import com.sm.stasversion.imagepicker.model.Image;
import com.sm.stasversion.imagepicker.model.Video;
import com.sm.stasversion.classes.serializedConfigs;

import org.wysaid.nativePort.CGENativeLibrary;
import org.wysaid.texUtils.CropInfo;

import java.util.List;

/**
 * Created by hoanglam on 8/17/17.
 */

public class AssetLoader {

    private RequestOptions options;

    public AssetLoader() {
        options = new RequestOptions()
                .placeholder(R.drawable.imagepicker_image_placeholder)
                .error(R.drawable.imagepicker_image_placeholder)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE);
    }

    public void loadConfigAsset(Asset asset, ImageView imageView) {
        //if(asset.getCorrection().isEmpty() && asset.getCrop().isEmpty()) {
        CropInfo crop = new Gson().fromJson(asset.getCrop(), CropInfo.class);

        if(asset.getCorrection().isEmpty() && (asset.getCrop().isEmpty() || (crop != null && crop.isCropped()) || (crop != null && !crop.isCropped))) {
            loadAsset(asset, imageView);
        } else {
            Glide.with(imageView.getContext())
                    .asBitmap()
                    .load(asset instanceof Image ? asset.getPath() : ((Video)asset).getThumbnailUri())
                    //.override(imageView.getWidth(), imageView.getHeight())
                    .apply(options)
                    //.transition(DrawableTransitionOptions.withCrossFade())
                    .into(new CustomTarget<Bitmap>(480, 800) {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            Bitmap bmp;

                            if(crop != null && !crop.isCropped()) {
                                bmp = serializedConfigs.cropImage(resource, crop);
                                //imageView.setImageBitmap(CGENativeLibrary.filterImage_MultipleEffects(resource, rules, _configs.get(0).intensity));
                            } else {
                                bmp = resource;
                                //imageView.setImageBitmap(CGENativeLibrary.filterImage_MultipleEffects(resource, rules, _configs.get(0).intensity));
                            }

                            if(asset.getCorrection().isEmpty()) {
                                imageView.setImageBitmap(bmp);
                            } else {
                                List<AdjustConfig> _configs = serializedConfigs.decryptConfigsStatic(asset.getCorrection());
                                String rules = serializedConfigs.calculateRules(_configs);

                                imageView.setImageBitmap(CGENativeLibrary.filterImage_MultipleEffects(bmp, rules, _configs.get(0).intensity, -1));
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
        }
    }

    /*private fun bitmapCrop(): Bitmap {
        val translateBitmap = Bitmap.createBitmap(
                transImage!!.width,
                transImage!!.height, transImage!!.config
        )

        val translateCanvas = Canvas(translateBitmap)

        val translateMatrix = Matrix()

        translateMatrix.postScale(mCurrentFragment!!.scaleX, mCurrentFragment!!.scaleY, transImage!!.width / 2.0f, transImage!!.height / 2.0f)
        translateMatrix.postRotate(mCurrentFragment!!.postRotate, transImage!!.width / 2.0f, transImage!!.height / 2.0f)

        translateCanvas.drawBitmap(transImage!!, translateMatrix, Paint())

        return translateBitmap
    }*/

    public void loadAsset(Asset asset, ImageView imageView) {
        Glide.with(imageView.getContext())
                .load(asset instanceof Image ? asset.getPath() : ((Video)asset).getThumbnailUri())
                .override(imageView.getWidth(), imageView.getHeight())
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);
    }
}
