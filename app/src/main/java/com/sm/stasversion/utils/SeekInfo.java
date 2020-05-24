package com.sm.stasversion.utils;

public class SeekInfo {
    private Float sMin;
    private Float sMax;
    private Float sCurrent;
    private String sName;
    private EffectType sType;

    public SeekInfo(Float min, Float max, Float current, String name, EffectType type) {
        sMin = min;
        sMax = max;
        sCurrent = current;
        sName = name;
        sType = type;
    }

    public void setMin(Float min) {
        sMin = min;
    }

    public void setMax(Float m) {
        sMax = m;
    }

    public void setCurrent(Float c) {
        sCurrent = c;
    }

    public void setName(String n) {
        sName = n;
    }

    public void setType(EffectType t) {
        sType = t;
    }


    public Float getMin() {
        return sMin;
    }

    public Float getMax() {
        return sMax;
    }

    public Float getCurrent() {
        return sCurrent;
    }

    public String getName() {
        return sName;
    }

    public EffectType getType() {
        return sType;
    }
}
