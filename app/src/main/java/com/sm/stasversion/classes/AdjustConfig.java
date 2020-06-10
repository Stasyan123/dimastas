package com.sm.stasversion.classes;

import com.sm.stasversion.utils.EffectType;

import org.wysaid.view.ImageGLSurfaceView;

public class AdjustConfig {
    private float minValue, originValue, maxValue;
    private int index;
    public float intensity, slierIntensity;
    public String mRule;
    public Boolean additional;
    public EffectType type;
    public AdjustConfig additionaItem;
    public Boolean startEditing = false;

    public AdjustConfig(int _index, float _minValue, float _originValue, float _maxValue,
                        String rule, float _slierIntensity, boolean _additional, EffectType _type) {
        index = _index;
        minValue = _minValue;
        originValue = _originValue;
        slierIntensity = _slierIntensity;
        maxValue = _maxValue;
        intensity = _originValue;
        mRule = rule;
        additional = _additional;
        type = _type;
    }

    public void setAdditional(AdjustConfig _additionaItem) {
        additionaItem = _additionaItem;
    }

    public float calcIntensity(float _intensity) {
        float result;
        if (_intensity <= 0.0f) {
            result = minValue;
        } else if (_intensity >= 1.0f) {
            result = maxValue;
        } else if (_intensity <= 0.5f) {
            result = minValue + (originValue - minValue) * _intensity * 2.0f;
        } else {
            result = maxValue + (originValue - maxValue) * (1.0f - _intensity) * 2.0f;
        }
        return result;
    }

    //_intensity range: [0.0, 1.0], 0.5 for the origin.
    public void setIntensity(float _intensity, boolean shouldProcess, ImageGLSurfaceView mImageView) {
        if (mImageView != null) {
            slierIntensity = _intensity;
            intensity = calcIntensity(_intensity);
            if(shouldProcess) {
                mImageView.setFilterIntensityForIndex(intensity, index, shouldProcess);
            }
        }
    }

    public void setIntensityWithParam(int config, float _intensity, float _intensity2, ImageGLSurfaceView mImageView, boolean shouldProcess) {
        if (mImageView != null) {
            slierIntensity = _intensity;
            intensity = calcIntensity(_intensity);

            if(additional) {
                additionaItem.slierIntensity = _intensity2;
                additionaItem.intensity = additionaItem.calcIntensity(_intensity2);
            }

            if(shouldProcess) {
                mImageView.setParamAtIndex(config, intensity, additionaItem.intensity, index);
            }
        }
    }

    public void setTempIntensity(float _intensity, boolean shouldProcess, ImageGLSurfaceView mImageView) {
        if (mImageView != null) {
            mImageView.setFilterIntensityForIndex(calcIntensity(_intensity), index, shouldProcess);
        }
    }

    public void setTempIntensityWithParam(int config, float _intensity, float _intensity2, ImageGLSurfaceView mImageView) {
        if (mImageView != null) {
            mImageView.setParamAtIndex(config, calcIntensity(_intensity), this.additionaItem.calcIntensity(_intensity2), index);
        }
    }

    public Float getIntensity() {
        return intensity;
    }

    public String getRule() {
        String rule = mRule + " " + intensity;

        if(additional) {
            rule += " " + additionaItem.intensity;
        }
        return rule;
    }
}
