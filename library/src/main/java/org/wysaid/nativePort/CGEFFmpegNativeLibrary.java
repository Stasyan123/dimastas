package org.wysaid.nativePort;

import android.graphics.Bitmap;
import android.util.Log;

import org.wysaid.common.Common;

/**
 * Created by wangyang on 15/7/30.
 */
public class CGEFFmpegNativeLibrary {
    static {
        NativeLibraryLoader.load();
    }

    public interface SaveVideoCallback {
        void progress(Integer pr, Integer id);
    }

    static SaveVideoCallback saveVideoCallback;

    public static void printFloat(float i) {
        Log.i(Common.LOG_TAG, "Ratata Video");
    }

    public static void setSaveVideoCallback(SaveVideoCallback callback) {
        saveVideoCallback = callback;
    }

    //will be called from jni.
    public static void getProgress(int progress, int id) {
        saveVideoCallback.progress(progress, id);
    }

    //CN: 视频转换+特效可能执行较长的时间， 请置于后台线程运行.
    //EN: Convert video + Filter Effects may take some time, so you'd better put it on another thread.
    public static boolean generateVideoWithFilter(String outputFilename, String inputFilename, String filterConfig, float filterIntensity,
                                                  Bitmap blendImage, CGENativeLibrary.TextureBlendMode blendMode, float blendIntensity,
                                                  boolean mute, int id) {

        return nativeGenerateVideoWithFilter(outputFilename, inputFilename, filterConfig, filterIntensity, blendImage, blendMode == null ? 0 : blendMode.ordinal(), blendIntensity, mute, id);
    }
    //////////////////////////////////////////

    private static native boolean nativeGenerateVideoWithFilter(String outputFilename, String inputFilename, String filterConfig, float filterIntensity,
                                                                Bitmap blendImage, int blendMode, float blendIntensity, boolean mute, int id);

    public static native void avRegisterAll();

}
