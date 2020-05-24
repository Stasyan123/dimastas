package com.sm.stasversion.customFilters;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class StasOverlayBlendSample extends StasScreenBlendFilter {
    private Bitmap bitmap;

    public StasOverlayBlendSample() {

    }

    public void setBitmap(Bitmap bm) {
        this.bitmap = bm;
    }

    public Bitmap getBitmap() {
        return this.bitmap;
    }

    @Override
    protected void drawCanvas(Canvas canvas) {
        if (bitmap != null && !bitmap.isRecycled()) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
    }
}
