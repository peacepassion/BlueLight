package com.polkapolka.bluetooth.le.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

/*
File:              RoundKnobButton
Version:           1.0.0
Release Date:      November, 2013
License:           GPL v2
Description:	   A round knob button to control volume and toggle between two states

****************************************************************************
Copyright (C) 2013 Radu Motisan  <radu.motisan@gmail.com>

http://www.pocketmagic.net

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
****************************************************************************/

public class RoundKnobButton extends RelativeLayout implements OnGestureListener {

    private GestureDetector gestureDetector;
    private float mAngleDown, mAngleUp;
    private ImageView ivRotor;
    private Bitmap bmpRotorOn, bmpRotorOff;
    private boolean mState = false;
    private int m_nWidth = 0, m_nHeight = 0;

    private float mCurrentViewAngle;  // the degree used by system to control rotation
    private float mCurrentLogicAngle; // the degree used by user
    private boolean mAllowRotation;
    private float[] mRotateRange;

    interface RoundKnobButtonListener {
        public void onStateChange(boolean newstate);

        public void onRotate(int percentage);
    }

    private RoundKnobButtonListener mRotateListener;

    public void setListener(RoundKnobButtonListener l) {
        mRotateListener = l;
    }

    public void SetState(boolean state) {
        mState = state;
        ivRotor.setImageBitmap(state ? bmpRotorOn : bmpRotorOff);
    }

    public RoundKnobButton(Context context, int rotoron, int rotoroff, final int w, final int h, float[] rotateRange) {
        super(context);
        // we won't wait for our size to be calculated, we'll just store out fixed size
        m_nWidth = w;
        m_nHeight = h;
        mRotateRange = rotateRange;
        // create stator
        // load rotor images
        Bitmap srcon = BitmapFactory.decodeResource(context.getResources(), rotoron);
        Bitmap srcoff = BitmapFactory.decodeResource(context.getResources(), rotoroff);
        float scaleWidth = ((float) w) / srcon.getWidth();
        float scaleHeight = ((float) h) / srcon.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        bmpRotorOn = Bitmap.createBitmap(
                srcon, 0, 0,
                srcon.getWidth(), srcon.getHeight(), matrix, true);
        bmpRotorOff = Bitmap.createBitmap(
                srcoff, 0, 0,
                srcoff.getWidth(), srcoff.getHeight(), matrix, true);
        // create rotor
        ivRotor = new ImageView(context);
        ivRotor.setImageBitmap(bmpRotorOn);
        LayoutParams lp_ivKnob = new LayoutParams(w, h);//LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp_ivKnob.addRule(RelativeLayout.CENTER_IN_PARENT);
        addView(ivRotor, lp_ivKnob);
        // set initial state
        SetState(mState);
        // enable gesture detector
        gestureDetector = new GestureDetector(getContext(), this);
    }

    /**
     * math..
     *
     * @param x
     * @param y
     * @return
     */
    private float cartesianToPolar(float x, float y) {
        float ret = (float) -Math.toDegrees(Math.atan2(x - 0.5f, y - 0.5f));
        ret += 180;
        return ret;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) return true;
        else return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {
        float x = event.getX() / ((float) getWidth());
        float y = event.getY() / ((float) getHeight());
        mAngleDown = cartesianToPolar(1 - x, 1 - y);// 1- to correct our custom axis direction
        Log.d("Peace", "call onDown, mAngleDown: " + mAngleDown);
        mAllowRotation = true;
        return true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        float x = e.getX() / ((float) getWidth());
        float y = e.getY() / ((float) getHeight());
        mAngleUp = cartesianToPolar(1 - x, 1 - y);// 1- to correct our custom axis direction

        // if we click up the same place where we clicked down, it's just a button press
        if (!Float.isNaN(mAngleDown) && !Float.isNaN(mAngleUp) && Math.abs(mAngleUp - mAngleDown) < 10) {
            SetState(!mState);
            if (mRotateListener != null) mRotateListener.onStateChange(mState);
        }
        return true;
    }

    private void setRotorPosAngle(float deg) {
        float logicAngle = deg + 180;
        if (logicAngle > 360) {
            logicAngle -= 360;
        }
        if (logicAngle >= mRotateRange[0] && logicAngle <= mRotateRange[1]) {
            mCurrentLogicAngle = logicAngle;
            Log.d("Peace", "current logic angle: " + mCurrentLogicAngle);
            Matrix matrix = new Matrix();
            ivRotor.setScaleType(ScaleType.MATRIX);
            matrix.postRotate(deg, m_nWidth / 2, m_nHeight / 2);//getWidth()/2, getHeight()/2);
            ivRotor.setImageMatrix(matrix);
            mCurrentViewAngle = deg;
            if (mRotateListener != null) {
                mRotateListener.onRotate(getCurrentPercent());
            }
        }
    }

    /**
     * percentage -> degree
     * 0          -> mRotateRange[0] + 180
     * 100        -> mRotateRange[1] + 180
     *
     * @param percentage
     */
    public void setRotorPercentage(int percentage) {
        if (percentage <= 100 && percentage >= 0) {
            float posDegree = mRotateRange[0] + 180 + ((mRotateRange[1] - mRotateRange[0]) / 100) * percentage;
            setRotorPosAngle(posDegree);
        }
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//		Log.d("Peace", "e1: " + e1 + ", e2: " + e2);
//		if (e2.getAction() == MotionEvent.ACTION_DOWN) {
//			mAllowRotation = true;
//		}
        float x = e2.getX() / ((float) getWidth());
        float y = e2.getY() / ((float) getHeight());
        float rotDegrees = cartesianToPolar(1 - x, 1 - y);// 1- to correct our custom axis direction

        if (!Float.isNaN(rotDegrees)) {
            // instead of getting 0-> 180, -180 0 , we go for 0 -> 360
            float posDegrees = rotDegrees;
//			if (rotDegrees < 0) posDegrees = 360 + rotDegrees;

            // deny full rotation, start start and stop point, and get a linear scale
            float dAng = posDegrees - mAngleDown;
            Log.w("Peace", "posDegrees: " + posDegrees + ", mAngleDown: " + mAngleDown + ", mCurrentAng: " + mCurrentViewAngle);
            posDegrees = mCurrentViewAngle + dAng;
            if (posDegrees > 360) {
                posDegrees -= 360;
            }
            setRotorPosAngle(posDegrees);
            mAngleDown += dAng;
            if (mAngleDown < 0) {
                mAngleDown += 360;
            }
            if (mAngleDown > 360) {
                mAngleDown -= 360;
            }
            return true; //consumed
        } else
            return false; // not consumed
    }

    public void onShowPress(MotionEvent e) {
        // TODO Auto-generated method stub

    }

    public int getCurrentPercent() {
        return (int) ((mCurrentLogicAngle - mRotateRange[0]) / (mRotateRange[1] - mRotateRange[0]) * 100);
    }

    public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    public void onLongPress(MotionEvent e) {
    }

    public float calculateRealDegDiff(float deg1, float deg2) {
        float d = deg2 - deg1;
        if (d > 180) {
            d = 360 - d;
        } else if (d < -180) {
            d = 360 + d;
        }
        return d;
    }


}
