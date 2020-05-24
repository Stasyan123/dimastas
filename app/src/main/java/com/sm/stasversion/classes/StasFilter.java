package com.sm.stasversion.classes;

import android.opengl.GLES20;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageTwoInputFilter;


public class StasFilter extends GPUImageFilter {
    public static final String HIGHLIGHT_SHADOW_FRAGMENT_SHADER = "" +
            " uniform sampler2D inputImageTexture;\n" +
            " varying highp vec2 textureCoordinate;\n" +
            "  \n" +
            " uniform lowp float shadows;\n" +
            " uniform lowp float highlights;\n" +
            " \n" +
            " const mediump vec3 luminanceWeighting = vec3(0.2, 0.2, 0.2);\n" +
            " const mediump vec3 luminanceWeightingH = vec3(0.3, 0.3, 0.3);\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            " 	lowp vec4 source = texture2D(inputImageTexture, textureCoordinate);\n" +
            " 	mediump float luminance = dot(source.rgb, luminanceWeighting);\n" +
            " 	mediump float luminanceH = dot(source.rgb, luminanceWeightingH);\n" +
            " \n" +
            " 	mediump float shadow = clamp((pow(luminance, 1.0/(shadows+1.0)) + (-0.76)*pow(luminance, 2.0/(shadows+1.0))) - luminance, 0.0, 1.0);\n" +
            " 	mediump float highlight = clamp((1.0 - (pow(1.0-luminanceH, 1.0/(2.0-highlights)) + (-0.8)*pow(1.0-luminanceH, 2.0/(2.0-highlights)))) - luminanceH, -1.0, 0.0);\n" +
            " 	lowp vec3 result = vec3(0.0, 0.0, 0.0) + ((luminance + shadow + highlight) - 0.0) * ((source.rgb - vec3(0.0, 0.0, 0.0))/(luminance - 0.0));\n" +
            " \n" +
            " 	gl_FragColor = vec4(result.rgb, source.a);\n" +
            " }";

    private int shadowsLocation;
    private float shadows;
    private int highlightsLocation;
    private float highlights;

    public StasFilter() {
        this(0.0f, 1.0f);
    }

    public StasFilter(final float shadows, final float highlights) {
        super(NO_FILTER_VERTEX_SHADER, HIGHLIGHT_SHADOW_FRAGMENT_SHADER);
        this.highlights = highlights;
        this.shadows = shadows;
    }

    @Override
    public void onInit() {
        super.onInit();
        highlightsLocation = GLES20.glGetUniformLocation(getProgram(), "highlights");
        shadowsLocation = GLES20.glGetUniformLocation(getProgram(), "shadows");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setHighlights(highlights);
        setShadows(shadows);
    }

    public void setHighlights(final float highlights) {
        this.highlights = highlights;
        setFloat(highlightsLocation, this.highlights);
    }

    public void setShadows(final float shadows) {
        this.shadows = shadows;
        setFloat(shadowsLocation, this.shadows);
    }
}
