package com.sm.stasversion.videoUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.daasuu.gpuv.egl.filter.GlBilateralFilter;
import com.daasuu.gpuv.egl.filter.GlBoxBlurFilter;
import com.daasuu.gpuv.egl.filter.GlBrightnessFilter;
import com.daasuu.gpuv.egl.filter.GlContrastFilter;
import com.daasuu.gpuv.egl.filter.GlCrosshatchFilter;
import com.daasuu.gpuv.egl.filter.GlExposureFilter;
import com.daasuu.gpuv.egl.filter.GlFilter;
import com.daasuu.gpuv.egl.filter.GlGammaFilter;
import com.daasuu.gpuv.egl.filter.GlHazeFilter;
import com.daasuu.gpuv.egl.filter.GlHighlightShadowFilter;
import com.daasuu.gpuv.egl.filter.GlHueFilter;
import com.daasuu.gpuv.egl.filter.GlLuminanceThresholdFilter;
import com.daasuu.gpuv.egl.filter.GlMonochromeFilter;
import com.daasuu.gpuv.egl.filter.GlOpacityFilter;
import com.daasuu.gpuv.egl.filter.GlPixelationFilter;
import com.daasuu.gpuv.egl.filter.GlPosterizeFilter;
import com.daasuu.gpuv.egl.filter.GlRGBFilter;
import com.daasuu.gpuv.egl.filter.GlSaturationFilter;
import com.daasuu.gpuv.egl.filter.GlSharpenFilter;
import com.daasuu.gpuv.egl.filter.GlSolarizeFilter;
import com.daasuu.gpuv.egl.filter.GlSwirlFilter;
import com.daasuu.gpuv.egl.filter.GlVibranceFilter;
import com.daasuu.gpuv.egl.filter.GlVignetteFilter;
import com.daasuu.gpuv.egl.filter.GlWhiteBalanceFilter;
import com.sm.stasversion.R;
import com.sm.stasversion.customFilters.StasFilter;

import java.util.Arrays;
import java.util.List;

public enum FilterType {
    DEFAULT,
    BILATERAL_BLUR,
    BOX_BLUR,
    BRIGHTNESS,
    BULGE_DISTORTION,
    CGA_COLORSPACE,
    CONTRAST,
    CROSSHATCH,
    EXPOSURE,
    FILTER_GROUP_SAMPLE,
    GAMMA,
    GAUSSIAN_FILTER,
    GRAY_SCALE,
    HALFTONE,
    HAZE,
    HIGHLIGHT_SHADOW,
    HUE,
    INVERT,
    LOOK_UP_TABLE_SAMPLE,
    LUMINANCE,
    FILTER1,
    FILTER2,
    FILTER3,
    LUMINANCE_THRESHOLD,
    MONOCHROME,
    OPACITY,
    OVERLAY,
    PIXELATION,
    POSTERIZE,
    RGB,
    SATURATION,
    SEPIA,
    SHARP,
    SOLARIZE,
    SPHERE_REFRACTION,
    SWIRL,
    TONE_CURVE_SAMPLE,
    TONE,
    VIBRANCE,
    VIGNETTE,
    WATERMARK,
    WEAK_PIXEL,
    WHITE_BALANCE,
    ZOOM_BLUR,
    BITMAP_OVERLAY_SAMPLE;


    public static List<FilterType> createFilterList() {
        return Arrays.asList(FilterType.values());
    }

    public static StasFilter createGlFilter(FilterType filterType, Context context) {
        switch (filterType) {
            case FILTER1:
                Bitmap bitmap1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.filter1);
                return new StasFilter(bitmap1);
            case FILTER2:
                Bitmap bitmap2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.filter2);
                return new StasFilter(bitmap2);
            case FILTER3:
                Bitmap bitmap3 = BitmapFactory.decodeResource(context.getResources(), R.drawable.filter3);
                return new StasFilter(bitmap3);
            default:
                return null;
        }
    }

    public static FilterAdjuster createFilterAdjuster(FilterType filterType) {
        switch (filterType) {
            case BILATERAL_BLUR:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlBilateralFilter) filter).setBlurSize(range(percentage, 0.0f, 1.0f));
                    }
                };
            case BOX_BLUR:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlBoxBlurFilter) filter).setBlurSize(range(percentage, 0.0f, 1.0f));
                    }
                };
            case BRIGHTNESS:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlBrightnessFilter) filter).setBrightness(range(percentage, -1.0f, 1.0f));
                    }
                };
            case CONTRAST:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlContrastFilter) filter).setContrast(range(percentage, 0.0f, 2.0f));
                    }
                };
            case CROSSHATCH:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlCrosshatchFilter) filter).setCrossHatchSpacing(range(percentage, 0.0f, 0.06f));
                        ((GlCrosshatchFilter) filter).setLineWidth(range(percentage, 0.0f, 0.006f));
                    }
                };
            case EXPOSURE:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlExposureFilter) filter).setExposure(range(percentage, -10.0f, 10.0f));
                    }
                };
            case GAMMA:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlGammaFilter) filter).setGamma(range(percentage, 0.0f, 3.0f));
                    }
                };
            case HAZE:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlHazeFilter) filter).setDistance(range(percentage, -0.3f, 0.3f));
                        ((GlHazeFilter) filter).setSlope(range(percentage, -0.3f, 0.3f));
                    }
                };
            case HIGHLIGHT_SHADOW:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlHighlightShadowFilter) filter).setShadows(range(percentage, 0.0f, 1.0f));
                        ((GlHighlightShadowFilter) filter).setHighlights(range(percentage, 0.0f, 1.0f));
                    }
                };
            case HUE:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlHueFilter) filter).setHue(range(percentage, 0.0f, 360.0f));
                    }
                };
            case LUMINANCE_THRESHOLD:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlLuminanceThresholdFilter) filter).setThreshold(range(percentage, 0.0f, 1.0f));
                    }
                };
            case MONOCHROME:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlMonochromeFilter) filter).setIntensity(range(percentage, 0.0f, 1.0f));
                    }
                };
            case OPACITY:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlOpacityFilter) filter).setOpacity(range(percentage, 0.0f, 1.0f));
                    }
                };
            case PIXELATION:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlPixelationFilter) filter).setPixel(range(percentage, 1.0f, 100.0f));
                    }
                };
            case POSTERIZE:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        // In theorie to 256, but only first 50 are interesting
                        ((GlPosterizeFilter) filter).setColorLevels((int) range(percentage, 1, 50));
                    }
                };
            case RGB:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlRGBFilter) filter).setRed(range(percentage, 0.0f, 1.0f));
                    }
                };
            case SATURATION:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlSaturationFilter) filter).setSaturation(range(percentage, 0.0f, 2.0f));
                    }
                };
            case SHARP:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlSharpenFilter) filter).setSharpness(range(percentage, -4.0f, 4.0f));
                    }
                };
            case SOLARIZE:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlSolarizeFilter) filter).setThreshold(range(percentage, 0.0f, 1.0f));
                    }
                };
            case SWIRL:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlSwirlFilter) filter).setAngle(range(percentage, 0.0f, 2.0f));
                    }
                };
            case VIBRANCE:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlVibranceFilter) filter).setVibrance(range(percentage, -1.2f, 1.2f));
                    }
                };
            case VIGNETTE:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlVignetteFilter) filter).setVignetteStart(range(percentage, 0.0f, 1.0f));
                    }
                };
            case WHITE_BALANCE:
                return new FilterAdjuster() {
                    @Override
                    public void adjust(GlFilter filter, int percentage) {
                        ((GlWhiteBalanceFilter) filter).setTemperature(range(percentage, 2000.0f, 8000.0f));
                    }
                };
            default:
                return null;
        }
    }

    private static float range(int percentage, float start, float end) {
        return (end - start) * percentage / 100.0f + start;
    }
}
