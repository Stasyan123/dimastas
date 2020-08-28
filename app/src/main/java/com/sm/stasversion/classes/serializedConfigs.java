package com.sm.stasversion.classes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wysaid.texUtils.CropInfo;
import org.wysaid.view.VideoPlayerGLSurfaceView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sm.stasversion.crop.BitmapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class serializedConfigs {
    private String jsonStr;
    private List<AdjustConfig> configs;
    private VideoPlayerGLSurfaceView player;

    public serializedConfigs() {

    }

    serializedConfigs(String _jsonStr) {
        jsonStr = _jsonStr;

    }

    public void setPlayer(VideoPlayerGLSurfaceView _player) {
        player = _player;
    }

    public List<AdjustConfig> decryptConfigs(String _jsonStr) {
        configs = new Gson().fromJson(_jsonStr, new TypeToken<List<AdjustConfig>>(){}.getType());

        List<AdjustConfig> newConfigs = new ArrayList<AdjustConfig>();

        for(AdjustConfig config : configs) {
            if(player != null) {
                config.setPlayerView(player);
            }

            if(config.parentId != -1) {
                newConfigs.get(config.parentId).additionaItem = config;
            } else {
                newConfigs.add(config);
            }
        }

        return newConfigs;
    }

    public static List<AdjustConfig> decryptConfigsStatic(String _jsonStr) {
        List<AdjustConfig> configs = new Gson().fromJson(_jsonStr, new TypeToken<List<AdjustConfig>>(){}.getType());

        for(int i = 0; i < 2; i++) {
            configs.get(configs.get(configs.size() - 1).parentId).additionaItem = configs.get(configs.size() - 1);
            configs.remove(configs.size() - 1);
        }

        return configs;
    }

    public static String encryptConfigs(List<AdjustConfig> _configs) {
        _configs.add(_configs.get(4).additionaItem);
        _configs.add(_configs.get(6).additionaItem);

        return new Gson().toJson(_configs);
    }

    public static String calculateRules(List<AdjustConfig> _configs) {
        String rule = "";

        for(AdjustConfig config : _configs) {
            if(config.active) {
                rule += " " + config.getRule();
            }
        }

        return rule;
    }

    public static Bitmap cropImage(Bitmap resource, CropInfo crop) {
        float w = resource.getWidth();
        float h = resource.getHeight();

        Rect rect =
                BitmapUtils.getRectFromPoints(
                        crop.points,
                        crop.originalW,
                        crop.originalH,
                        false,
                        1,
                        1
                );

        float left = rect.left * (w / (float)crop.originalW);
        float top = rect.top * (h / (float)crop.originalH);
        float width = rect.width() * (w / (float)crop.originalW);
        float height = rect.height() * (h / (float)crop.originalH);

        Bitmap dstBmp = Bitmap.createBitmap(
                resource,
                (int)left,
                (int)top,
                (int)width,
                (int)height
        );

        Matrix m = new Matrix();

        m.postScale(crop.flipHor ? -crop.scale : crop.scale, crop.flipVert ? -crop.scale : crop.scale, dstBmp.getWidth() / 2, dstBmp.getHeight() / 2);
        m.postRotate(crop.postRotate, dstBmp.getWidth() / 2, dstBmp.getHeight() / 2);

        Canvas canvas = new Canvas(dstBmp);
        canvas.drawBitmap(resource, m, new Paint());

        Matrix matrixNew = new Matrix();
        matrixNew.setRotate(crop.rotation, dstBmp.getWidth() / 2, dstBmp.getHeight() / 2);

        return Bitmap.createBitmap(dstBmp, 0, 0, (int)width, (int)height, matrixNew, true);
        //return dstBmp;
    }
}
