package com.sm.stasversion.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import com.sm.stasversion.R;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
public class HorizontalProgressWheelView extends View {

    private final Rect mCanvasClipBounds = new Rect();

    private ScrollingListener mScrollingListener;
    private float mLastTouchedPosition;

    private Paint mProgressLinePaint;
    private Paint mProgressMiddleLinePaint;
    private int mProgressLineWidth, mProgressLineHeight;
    private int mProgressLineMargin;
    private int lOffset = 1;
    private int linesCount = 0;

    private boolean mScrollStarted;
    private float mTotalScrollDistance;

    private int mMiddleLineColor;
    public float currentPercentF = 0f;
    public int currentPercent = 0;
    private static final int percentMax = 30;

    public boolean firstDraw = true;

    public HorizontalProgressWheelView(Context context) {
        this(context, null);
    }

    public HorizontalProgressWheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalProgressWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HorizontalProgressWheelView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setScrollingListener(ScrollingListener scrollingListener) {
        mScrollingListener = scrollingListener;
    }

    public void setMiddleLineColor(@ColorInt int middleLineColor) {
        mMiddleLineColor = middleLineColor;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchedPosition = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                if (mScrollingListener != null) {
                    mScrollStarted = false;
                    mScrollingListener.onScrollEnd();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = event.getX() - mLastTouchedPosition;

                if (distance != 0) {
                    float _totalScrollDistance = mTotalScrollDistance + distance;
                    float deltaX = (_totalScrollDistance) % (float) (mProgressLineMargin + mProgressLineWidth);
                    float startX = -deltaX + lOffset + mCanvasClipBounds.left + _totalScrollDistance;
                    float startLastX = -deltaX + lOffset + mCanvasClipBounds.left + (linesCount - 1) * (mProgressLineWidth + mProgressLineMargin) + _totalScrollDistance;


                    if(mCanvasClipBounds.centerX() + mProgressLineWidth / 2 < startX || mCanvasClipBounds.centerX() + mProgressLineWidth / 2 > startLastX) {
                        break;
                    }

                    if (!mScrollStarted) {
                        mScrollStarted = true;
                        if (mScrollingListener != null) {
                            mScrollingListener.onScrollStart();
                        }
                    }
                    onScrollEvent(event, -distance);
                }
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.getClipBounds(mCanvasClipBounds);

        lOffset = mProgressLineWidth / 2;
        linesCount = mCanvasClipBounds.width() / (mProgressLineWidth + mProgressLineMargin);

        if(firstDraw) {
            invalidateValue();
            firstDraw = false;
        }

        float deltaX = (mTotalScrollDistance) % (float) (mProgressLineMargin + mProgressLineWidth);

        if ((linesCount % 2) == 0) {
            linesCount += 1;

            if(linesCount * (mProgressLineWidth + mProgressLineMargin) > mCanvasClipBounds.width()) {
                linesCount -= 2;
                lOffset += mProgressLineWidth + mProgressLineMargin;
            }
        }

        for (int i = 0; i < linesCount; i++) {
            //mProgressLinePaint.setAlpha(255);

            canvas.drawLine(
                    -deltaX+ lOffset + mCanvasClipBounds.left + i * (mProgressLineWidth + mProgressLineMargin) + mTotalScrollDistance ,
                    mCanvasClipBounds.centerY() - mProgressLineHeight / 4.0f,
                    -deltaX + lOffset + mCanvasClipBounds.left + i * (mProgressLineWidth + mProgressLineMargin) + mTotalScrollDistance,
                    mCanvasClipBounds.centerY() + mProgressLineHeight / 4.0f, mProgressLinePaint);
        }

        canvas.drawLine(mCanvasClipBounds.centerX() + mProgressLineWidth / 2, mCanvasClipBounds.centerY() - mProgressLineHeight / 2.0f,
                mCanvasClipBounds.centerX() + mProgressLineWidth / 2, mCanvasClipBounds.centerY() + mProgressLineHeight / 4.0f, mProgressMiddleLinePaint);

    }

    public void setValue(int percent, float percentF) {
        currentPercent = percent;
        currentPercentF = percentF;
    }

    public void invalidateValue() {
        mTotalScrollDistance = ((currentPercentF * ((linesCount - 1) / 2)) / percentMax) * (mProgressLineWidth + mProgressLineMargin);
    }

    private void onScrollEvent(MotionEvent event, float distance) {
        mTotalScrollDistance -= distance;

        postInvalidate();
        mLastTouchedPosition = event.getX();
        if (mScrollingListener != null) {
            int percent = ((int)(mTotalScrollDistance / (mProgressLineWidth + mProgressLineMargin)) * percentMax) / ((linesCount - 1) / 2);
            float percentF = ((mTotalScrollDistance / (mProgressLineWidth + mProgressLineMargin)) * percentMax) / ((linesCount - 1) / 2);

            mScrollingListener.onScroll(-percent, percentF);
        }
    }

    private void init() {
        mMiddleLineColor = ContextCompat.getColor(getContext(), R.color.ucrop_mid_line);

        mProgressLineWidth = getContext().getResources().getDimensionPixelSize(R.dimen.crop_horizontal_progress_width);
        mProgressLineHeight = getContext().getResources().getDimensionPixelSize(R.dimen.crop_horizontal_progress_height);
        mProgressLineMargin = getContext().getResources().getDimensionPixelSize(R.dimen.crop_horizontal_progress_margin);

        mProgressLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressLinePaint.setStyle(Paint.Style.STROKE);
        mProgressLinePaint.setStrokeWidth(mProgressLineWidth);
        mProgressLinePaint.setColor(getResources().getColor(R.color.ucrop_wheel_line));

        mProgressMiddleLinePaint = new Paint(mProgressLinePaint);
        mProgressMiddleLinePaint.setColor(mMiddleLineColor);
        mProgressMiddleLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mProgressMiddleLinePaint.setStrokeWidth(getContext().getResources().getDimensionPixelSize(R.dimen.crop_horizontal_progress_width));
    }

    public interface ScrollingListener {

        void onScrollStart();

        void onScroll(int percent, float percentF);

        void onScrollEnd();
    }

}
