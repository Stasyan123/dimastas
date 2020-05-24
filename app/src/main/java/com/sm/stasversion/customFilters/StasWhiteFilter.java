package com.sm.stasversion.customFilters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;

import com.daasuu.gpuv.egl.EglUtil;
import com.daasuu.gpuv.egl.filter.GlFilter;

public class StasWhiteFilter extends GlFilter {
    private final static String FRAGMENT_SHADER =
            " uniform lowp sampler2D sTexture;\n" +
                    " varying vec2 vTextureCoord;\n" +

                    " \n" +
                    "uniform lowp float temperature;\n" +
                    "uniform lowp float tint;\n" +
                    "\n" +
                    "const lowp vec3 warmFilter = vec3(0.93, 0.54, 0.0);\n" +
                    "\n" +
                    "const mediump mat3 RGBtoYIQ = mat3(0.299, 0.587, 0.114, 0.596, -0.274, -0.322, 0.212, -0.523, 0.311);\n" +
                    "const mediump mat3 YIQtoRGB = mat3(1.0, 0.956, 0.621, 1.0, -0.272, -0.647, 1.0, -1.105, 1.702);\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "	lowp vec4 source = texture2D(sTexture, vTextureCoord);\n" +
                    "	\n" +
                    "	mediump vec3 yiq = RGBtoYIQ * source.rgb; //adjusting tint\n" +
                    "	yiq.b = clamp(yiq.b + tint*0.5226*0.1, -0.5226, 0.5226);\n" +
                    "	lowp vec3 rgb = YIQtoRGB * yiq;\n" +
                    "\n" +
                    "	lowp vec3 processed = vec3(\n" +
                    "		(rgb.r < 0.5 ? (2.0 * rgb.r * warmFilter.r) : (1.0 - 2.0 * (1.0 - rgb.r) * (1.0 - warmFilter.r))), //adjusting temperature\n" +
                    "		(rgb.g < 0.5 ? (2.0 * rgb.g * warmFilter.g) : (1.0 - 2.0 * (1.0 - rgb.g) * (1.0 - warmFilter.g))), \n" +
                    "		(rgb.b < 0.5 ? (2.0 * rgb.b * warmFilter.b) : (1.0 - 2.0 * (1.0 - rgb.b) * (1.0 - warmFilter.b))));\n" +
                    "\n" +
                    "	gl_FragColor = vec4(mix(rgb, processed, temperature), source.a);\n" +
                    "}";

    private float intensity = 1.0f;

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(final float intensity) {
        this.intensity = intensity;
    }

    public StasWhiteFilter() {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
    }

    @Override
    public void onDraw() {
        GLES20.glUniform1f(getHandle("temperature"), 0.0f);
        GLES20.glUniform1f(getHandle("tint"), 0.0f);
    }
}
