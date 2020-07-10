package org.wysaid.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import org.wysaid.common.Common;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static org.wysaid.view.VideoPlayerGLSurfaceView.LOG_TAG;

public class VideoPlayerGlTextureView extends TextureView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    public VideoPlayerGlTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Log.i(LOG_TAG, "MyGLSurfaceView Construct...");

        /*setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 8, 0);
        getHolder().setFormat(PixelFormat.RGBA_8888);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        setZOrderOnTop(true);*/

        Log.i(LOG_TAG, "MyGLSurfaceView Construct OK...");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        Log.i(LOG_TAG, "video player onSurfaceCreated...");

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_STENCIL_TEST);

        /*if (mOnCreateCallback != null) {
            mOnCreateCallback.createOK();
        }

        if (mVideoUri != null && (mSurfaceTexture == null || mVideoTextureID == 0)) {
            mVideoTextureID = Common.genSurfaceTextureID();
            mSurfaceTexture = new SurfaceTexture(mVideoTextureID);
            mSurfaceTexture.setOnFrameAvailableListener(VideoPlayerGLSurfaceView.this);
            _useUri();
        }*/
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        /*if (mSurfaceTexture == null || mFrameRenderer == null) {
            return;
        }

        mSurfaceTexture.updateTexImage();

        if (!mPlayer.isPlaying()) {
            return;
        }

        mSurfaceTexture.getTransformMatrix(mTransformMatrix);
        Matrix.rotateM(mTransformMatrix, 0, 45, 0, 0, 1);
        //Matrix.rotateM(mTransformMatrix, 0, 90, 0, 0, 1);
        Matrix.translateM(mTransformMatrix, 0, 0, -0.5f, 0);
        mFrameRenderer.update(mVideoTextureID, mTransformMatrix);

        mFrameRenderer.runProc();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_BLEND);
        mFrameRenderer.render(mRenderViewport.x, mRenderViewport.y, mRenderViewport.width, mRenderViewport.height);
        GLES20.glDisable(GLES20.GL_BLEND);*/

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        /*mViewWidth = width;
        mViewHeight = height;

        calcViewport();*/
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        /*requestRender();

        if (mLastTimestamp2 == 0)
            mLastTimestamp2 = System.currentTimeMillis();

        long currentTimestamp = System.currentTimeMillis();

        ++mFramesCount2;
        mTimeCount2 += currentTimestamp - mLastTimestamp2;
        mLastTimestamp2 = currentTimestamp;
        if (mTimeCount2 >= 1e3) {
            Log.i(LOG_TAG, String.format("播放帧率: %d", mFramesCount2));
            mTimeCount2 -= 1e3;
            mFramesCount2 = 0;
        }*/
    }
}
