package com.ttop.cassette.util;

import android.graphics.Color;

public class ColorUtil {

    public int desaturateColor(int color, float ratio){
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = hsv[1] / 1 * ratio + 0.2f * (1.0f - ratio);

        return Color.HSVToColor(hsv);
    }
}
