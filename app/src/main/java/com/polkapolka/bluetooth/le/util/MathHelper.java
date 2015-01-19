package com.polkapolka.bluetooth.le.util;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by peace_da on 2015/1/17.
 */
public class MathHelper {

    public static ArrayList<ArrayList<Float>> generateCirclePositionArray(float centerX, float centerY, float startDegree, float endDegree, float r, int pointNum) {
        ArrayList<ArrayList<Float>> positionArray = new ArrayList<ArrayList<Float>>();
        float interplotUnit = (endDegree - startDegree) / (pointNum - 1);
        for (int i = 0; i < pointNum; ++i) {
            float degree = startDegree + interplotUnit * i;
            float x = (float) (r * Math.sin(degree * Math.PI / 180) + centerX);
            float y = (float) (r * Math.cos(degree * Math.PI / 180) + centerY);
            ArrayList<Float> position = new ArrayList<Float>(2);
            position.add(x);
            position.add(y);
            positionArray.add(position);
        }
        Collections.reverse(positionArray);

        return positionArray;
    }

    public static float vectorToScalarScroll(float dx, float dy, float x, float y) {
        // get the length of the vector
        float l = (float) Math.sqrt(dx * dx + dy * dy);

        // decide if the scalar should be negative or positive by finding
        // the dot product of the vector perpendicular to (x,y).
        float crossX = -y;
        float crossY = x;

        float dot = (crossX * dx + crossY * dy);
        float sign = Math.signum(dot);

        return l * sign;
    }

    public static float distance(float eventX, float startX, float eventY, float startY) {
        float dx = eventX - startX;
        float dy = eventY - startY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
