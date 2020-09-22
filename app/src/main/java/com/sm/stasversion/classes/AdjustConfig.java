package com.sm.stasversion.classes;

import android.util.Log;

import com.sm.stasversion.utils.EffectType;

import org.wysaid.view.ImageGLSurfaceView;
import org.wysaid.view.VideoPlayerGLSurfaceView;

import java.lang.reflect.Array;
import java.util.List;

import static org.wysaid.view.ImageGLSurfaceView.LOG_TAG;

public class AdjustConfig {
    private float minValue, originValue, maxValue;
    private int index;
    public int position;
    public float intensity, slierIntensity, tempIntensity;
    public String mRule;
    public Boolean additional;
    public EffectType type;
    public transient AdjustConfig additionaItem;
    public float[][] hsl = null;
    public float[][] tempHsl = null;
    public int hslPos = 0;
    public int isSharpen = 0;
    public Boolean startEditing = false;
    public int parentId = -1;

    public Boolean editorOpen = false;

    public Boolean active = false;
    public int filterPosition = 0;

    public transient VideoPlayerGLSurfaceView mPlayerView;

    //texture settings
    public Float[] rotate = {0f, 0f};
    public Float[] horizontal = {1f, 1f};
    public Float[] vertical = {1f, 1f};
    public Boolean[] diff = {false, false};
    public Boolean startUsing = false;
    public String name = "";

    public boolean[] isEdited;

    public AdjustConfig() {

    }

    public AdjustConfig(int _index, float _minValue, float _originValue, float _maxValue,
                        String rule, float _slierIntensity, boolean _additional, EffectType _type, VideoPlayerGLSurfaceView _mPlayerView) {
        index = _index;
        minValue = _minValue;
        originValue = _originValue;
        slierIntensity = _slierIntensity;
        maxValue = _maxValue;
        intensity = _originValue;
        mRule = rule;
        additional = _additional;
        type = _type;
        mPlayerView = _mPlayerView;

        if(type == EffectType.Sharpness) {
            isSharpen = 1;
        }
    }

    public void setPlayerView(VideoPlayerGLSurfaceView _mPlayerView) {
        mPlayerView = _mPlayerView;
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

    public int calculateProgress(float value) {
        float result;

        if (value <= -1.0f) {
            result = minValue;
        } else if (value >= 1.0f) {
            result = maxValue;
        } else if (value <= 0.0f) {
            result = (value - minValue) / ((originValue - minValue) * 2.0f);
        } else {
            result = - (value - maxValue) / ((originValue - maxValue) * 2.0f);
            result += 1.0f;
        }

        return (int)(result * 100);
    }

    //_intensity range: [0.0, 1.0], 0.5 for the origin.
    public void setIntensity(float _intensity, boolean shouldProcess, ImageGLSurfaceView mImageView) {
        slierIntensity = _intensity;
        intensity = calcIntensity(_intensity);

        if(shouldProcess) {
            if (mImageView != null) {
                mImageView.setFilterIntensityForIndex(intensity, filterPosition, shouldProcess, isSharpen);
            } else if (mPlayerView != null) {
                mPlayerView.setFilterIntensityAtIndex(intensity, filterPosition, isSharpen);
            }
        }
    }

    public boolean checkEmptyHSL() {
        for (float[] config : hsl) {
            if(config[0] != 0.0f || config[1] != 0.0f ||config[2] != 0.0f) {
                return false;
            }
        }

        return true;
    }

    public boolean checkOriginal() {
        Boolean result;

        if(type == EffectType.HSL) {
            result = checkEmptyHSL();
        } else if(type == EffectType.Highlight || type == EffectType.Shadow || type == EffectType.Temperature) {
            result = intensity == originValue && additionaItem.intensity == additionaItem.originValue;
        } else {
            result = intensity == originValue;
        }

        return result;
    }

    public void setIntensityWithParam(int config, float _intensity, float _intensity2, ImageGLSurfaceView mImageView, boolean shouldProcess) {
        slierIntensity = _intensity;
        intensity = calcIntensity(_intensity);

        if(additional) {
            additionaItem.slierIntensity = _intensity2;
            additionaItem.intensity = additionaItem.calcIntensity(_intensity2);
        }

        if(shouldProcess) {
            if (mImageView != null) {
                mImageView.setParamAtIndex(config, intensity, additionaItem.intensity, 0.0f, filterPosition);
            } else if (mPlayerView != null) {
                mPlayerView.setParamAtIndex(config, intensity, additionaItem.intensity, 0.0f, filterPosition);
            }
        }
    }

    public void setTempIntensity(float _intensity, boolean shouldProcess, ImageGLSurfaceView mImageView) {
        tempIntensity = calcIntensity(_intensity);

        if (mImageView != null) {
            mImageView.setFilterIntensityForIndex(tempIntensity, filterPosition, shouldProcess, isSharpen);
        } else if(mPlayerView != null) {
            mPlayerView.setFilterIntensityAtIndex(tempIntensity, filterPosition, isSharpen);
        }
    }

    public void setTempIntensityWithParam(int config, float _intensity, float _intensity2, float _intensity3, ImageGLSurfaceView mImageView) {
        tempIntensity = calcIntensity(_intensity);

        if(type == EffectType.HSL) {
            tempHsl[hslPos][0] = tempIntensity;
            tempHsl[hslPos][1] = _intensity2;
            tempHsl[hslPos][2] = _intensity3;
        } else {
            additionaItem.tempIntensity = _intensity2;
        }

        if (mImageView != null) {
            mImageView.setParamAtIndex(config, tempIntensity, _intensity2, _intensity3, filterPosition);
        } else if(mPlayerView != null) {
            mPlayerView.setParamAtIndex(config, tempIntensity, _intensity2, _intensity3, filterPosition);
        }
    }

    public Float getIntensity() {
        return intensity;
    }

    public float[] getHslConfig(boolean temp) {
        if(temp) {
            return tempHsl[hslPos];
        } else {
            return hsl[hslPos];
        }
    }

    public void setRule(Boolean isDiff) {
        mRule = "@blend";

        if(isDiff) {
            mRule += " sub";
        } else {
            mRule += " sr";
        }

        mRule += " " + name;
    }

    public void textureConfig(Float r, Float h, Float v, Boolean _diff, Boolean _startUsing) {
        rotate[0] = r;
        rotate[1] = r;
        vertical[0] = v;
        vertical[1] = v;
        horizontal[0] = h;
        horizontal[1] = h;
        diff[0] = _diff;
        diff[1] = _diff;
        startUsing = _startUsing;
    }

    public String getRuleTemporary() {
        String rule = mRule;

        if(additional) {
            rule += " " + tempIntensity + " " + additionaItem.tempIntensity;
        } else if(hsl != null) {
            for (float[] config : tempHsl) {
                rule += " " + config[0];
                rule += " " + config[1];
                rule += " " + config[2];
            }
        } else if(type == EffectType.Texture || type == EffectType.Grain) {
            rule += " " + tempIntensity * 100;
        } else {
            rule += " " + tempIntensity;
        }

        return rule;
    }

    public String getRule() {
        String rule = mRule;

        if(additional) {
            rule += " " + intensity + " " + additionaItem.intensity;
        } else if(hsl != null) {
            for (float[] config : hsl) {
                rule += " " + config[0];
                rule += " " + config[1];
                rule += " " + config[2];
            }
        } else if(type == EffectType.Texture || type == EffectType.Grain) {
            rule += " " + intensity * 100;
        } else {
            rule += " " + intensity;
        }
        return rule;
    }
}
