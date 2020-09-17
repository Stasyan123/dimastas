package com.sm.stasversion.classes;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;

import org.wysaid.nativePort.CGEFFmpegNativeLibrary;
import org.wysaid.nativePort.CGENativeLibrary;


public class CGEFFmpegAcyncTask extends AsyncTask<Void, String, Boolean> {

    String outputFilename;
    String path;
    String rules;
    Float intensity;
    Integer id;
    FFmpegExecuteResponseHandler ffmpegExecuteResponseHandler;


    public CGEFFmpegAcyncTask (String _outputFilename, String _path, String _rules, Float _intensity, FFmpegExecuteResponseHandler ffmpegExecuteResponseHandler, Integer id) {
        outputFilename = _outputFilename;
        path = _path;
        rules = _rules;
        intensity = _intensity;
        this.ffmpegExecuteResponseHandler = ffmpegExecuteResponseHandler;
        this.id = id;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            CGEFFmpegNativeLibrary.generateVideoWithFilter(
                    outputFilename,
                    path,
                    rules,
                    intensity,
                    null,
                    CGENativeLibrary.TextureBlendMode.CGE_BLEND_ADDREV,
                    1.0f,
                    false,
                    id
            );
        } catch (Exception e) {

        } finally {
            this.ffmpegExecuteResponseHandler.onFinish();
        }

        return true;
    }
}
