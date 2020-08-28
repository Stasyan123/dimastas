package org.wysaid.texUtils;

import android.graphics.Rect;
import android.graphics.RectF;

public class CropInfo {
    public boolean isCropped = false;

    public int width = 0;
    public int height = 0;
    public int x = 0;
    public int y = 0;
    public float scaleX = 1;
    public float scaleY = 1;

    public float originalScaleX = 1;
    public float originalScaleY = 1;
    public int originalH = 1;
    public int originalW = 1;
    public int currentPercent = 0;
    public float currentPercentF = 0f;

    public int videoWidth = 0;
    public int videoHeight = 0;

    public int rotation = 0;
    public boolean flipHor = false;
    public boolean flipVert = false;
    public float postRotate = 0.0f;
    public float scale = 1.0f;

    public float percent = 0f;
    public boolean instaMode = false;
    public float[] points;
    public RectF overlay;

    public CropInfo() {

    }

    public CropInfo(int _w, int _h, int _x, int _y, int _sX, int _sY) {
        width = _w;
        height = _h;
        x = _x;
        y = _y;
        scaleX = _sX;
        scaleY = _sY;
    }

    public boolean isCropped() {
        return scale == 1.0f && percent == 0f && postRotate == 0.0f && flipVert == false && flipHor == false && rotation == 0 &&
                scaleY == 1 && scaleX == 1 && x == 0 && y == 0 && width == 0 && height == 0 && originalScaleX == 1 && originalScaleY == 1;
    }
}
