package com.polkapolka.bluetooth.le.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

/**
 * Created by peace_da on 2015/1/17.
 */
public class ViewHelper {

    private static final String LOG_TAG = LogHelper.getNativeSimpleLogTag(ViewHelper.class, LogHelper.DEFAULT_LOG_TAG);

    public static float[] getViewCenterPosition(View v) {
        float x = v.getX() + v.getWidth() / 2;
        float y = v.getY() + v.getHeight() / 2;
        float[] ret = {x, y};
        return ret;
    }


    /**
     *
     * @param w view's width
     * @param h view's height
     * @param centerX target center position x
     * @param centerY target center position y
     * @return
     */
    public static float[] convertCenterPosition2LeftTopPosition(float w, float h, float centerX, float centerY) {
        Log.d(LOG_TAG, "w: " + w + ", h: " + h);
        float x = centerX - w/2;
        float y = centerY - h/2;
        return new float[] {x, y};
    }

    public static float convertDpToPixel(Context ctx, float dp) {
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

}
