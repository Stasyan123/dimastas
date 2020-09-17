package com.sm.stasversion.interfaces;

import android.graphics.Bitmap;

public interface CGEImageResponseHandler {
    /**
     * on Success
     */
    public void onSuccess(String s);

    /**
     * on Progress
     */
    public void onProgress(Long id, Integer progress);

    /**
     * on Failure
     * @param message complete output of the FFmpeg command
     */
    public void onFailure(String message);
}
