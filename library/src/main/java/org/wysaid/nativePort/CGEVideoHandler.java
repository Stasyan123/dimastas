package org.wysaid.nativePort;

import android.graphics.Bitmap;

public class CGEVideoHandler {
    static {
        NativeLibraryLoader.load();
    }

    public boolean generateVideoWithFilter(String outputFilename, String inputFilename, String filterConfig, float filterIntensity,
                                                  Bitmap blendImage, CGENativeLibrary.TextureBlendMode blendMode, float blendIntensity,
                                                  boolean mute) {

        return nativeGenerateVideoWithFilter(outputFilename, inputFilename, filterConfig, filterIntensity, blendImage, blendMode == null ? 0 : blendMode.ordinal(), blendIntensity, mute);

    }

    //////////////////////////////////////////

    private native boolean nativeGenerateVideoWithFilter(String outputFilename, String inputFilename, String filterConfig, float filterIntensity, Bitmap blendImage, int blendMode, float blendIntensity, boolean mute);

    public native void avRegisterAll();
}
