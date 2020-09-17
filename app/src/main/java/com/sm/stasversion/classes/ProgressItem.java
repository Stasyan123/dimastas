package com.sm.stasversion.classes;

public class ProgressItem {
    public int id;
    public int max;
    public int current;
    public int correctionCurrent = 0;
    public int prev;
    public boolean finished = false;

    public ProgressItem(int _id, int _max, int _current, int _prev) {
        id = _id;
        max = _max;
        current = _current;
        prev = _prev;
    }
}
