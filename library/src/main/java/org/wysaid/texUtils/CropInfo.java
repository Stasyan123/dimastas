package org.wysaid.texUtils;

public class CropInfo {
    public boolean isCrop = false;

    public int width;
    public int height;
    public int x = 0;
    public int y = 0;
    public float scaleX = 1;
    public float scaleY = 1;

    public float rotation = 0;
    public boolean flipHor = false;
    public boolean flipVert = false;
    public float postRotate = 0.0f;
    public float scale = 1.0f;

    public float percent = 0f;

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
}
