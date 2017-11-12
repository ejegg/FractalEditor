package com.ejegg.android.fractaleditor.touch;

import java.util.ArrayList;
import java.util.List;

import android.view.MotionEvent;
/*
 * Mostly from pretobomba on Stack Overflow (http://stackoverflow.com/questions/10682019/android-two-finger-rotation)
 */
public class RotationGestureListener {
    private static final int INVALID_POINTER_ID = -1;
    private float fX, fY, sX, sY, focalX, focalY;
    private int ptrID1, ptrID2;
    private boolean discardedFirst = false;
    private final List<RotationGestureSubscriber> subscribers = new ArrayList<RotationGestureSubscriber>();
    
    public RotationGestureListener(){
        ptrID1 = INVALID_POINTER_ID;
        ptrID2 = INVALID_POINTER_ID;
    }
    
    public RotationGestureListener(RotationGestureSubscriber subscriber){
    	this();
    	subscribers.add(subscriber);
    }
    
    public boolean isInProgress() {
    	return ptrID1 != INVALID_POINTER_ID && ptrID2 != INVALID_POINTER_ID;
    }

    public boolean onTouchEvent(MotionEvent event){
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                sX = event.getX();
                sY = event.getY();
                ptrID1 = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                fX = event.getX();
                fY = event.getY();
                focalX = getMidpoint(fX, sX);
                focalY = getMidpoint(fY, sY);
                ptrID2 = event.getPointerId(event.getActionIndex());
                break;
            case MotionEvent.ACTION_MOVE:

                if(isInProgress()){
                    float nfX, nfY, nsX, nsY;
                    nfX = event.getX(event.findPointerIndex(ptrID1));
                    nfY = event.getY(event.findPointerIndex(ptrID1));
                    nsX = event.getX(event.findPointerIndex(ptrID2));
                    nsY = event.getY(event.findPointerIndex(ptrID2));
                    float angle = angleBtwLines(fX, fY, sX, sY, nfX, nfY, nsX, nsY);
                    if (discardedFirst) {
                    	for (RotationGestureSubscriber sub : subscribers) {
                    		sub.rotate(angle, focalX, focalY);
                    	}
                    } else {
                    	discardedFirst = true;
                    }
                    fX = nfX;
                    fY = nfY;
                    sX = nsX;
                    sY = nsY;
                }
                break;
            case MotionEvent.ACTION_UP:
                ptrID1 = INVALID_POINTER_ID;
                discardedFirst = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                ptrID2 = INVALID_POINTER_ID;
                discardedFirst = false;
                break;
        }
        return false;
    }

    private float getMidpoint(float a, float b){
        return (a + b) / 2;
    }
    
    private float angleBtwLines (float fx1, float fy1, float fx2, float fy2, float sx1, float sy1, float sx2, float sy2){
        float angle1 = (float) Math.atan2(fy1 - fy2, fx1 - fx2);
        float angle2 = (float) Math.atan2(sy1 - sy2, sx1 - sx2);
        return (float) Math.toDegrees(angle2-angle1) % 360.0f;
    }
}