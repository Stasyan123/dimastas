package com.sm.stasversion.classes;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageTwoInputFilter;


public class OldStasFilter extends GPUImageTwoInputFilter {
    public static final String HARD_LIGHT_BLEND_FRAGMENT_SHADER = "varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2;\n" +
            "\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            "\n" +
            " const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +
            "\n" +
            " void main()\n" +
            " {\n" +
            "     mediump vec4 base = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     mediump vec4 overlay = texture2D(inputImageTexture2, textureCoordinate2);\n" +
            "\n" +
            "     vec2 uv = gl_FragCoord.xy / iResolution.x;\n" +
            "     gl_FragColor = texture(inputImageTexture, uv);\n" +
            " }";

    public OldStasFilter() {
        super(HARD_LIGHT_BLEND_FRAGMENT_SHADER);
    }
}
