package com.ejegg.android.fractaleditor.touch;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class MotionEventHandler implements RotationGestureSubscriber{

	private float mPreviousX;
	private float mPreviousY;
	private boolean touched;
	private boolean dragging;
	private final GestureDetector mGestureDetector;
	private final ScaleGestureDetector mScaleDetector;
	private final RotationGestureListener mRotationListener;
	private final float flingScaleFactor = 0.001f;
	
	private List<MotionEventSubscriber> subscribers = new ArrayList<MotionEventSubscriber>();
    
	public MotionEventHandler(Context context) {
		mGestureDetector = new GestureDetector(context, new GestureListener());
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		mRotationListener = new RotationGestureListener(this);
	}
	
	public void addSubscriber(MotionEventSubscriber subscriber) {
		subscribers.add(subscriber);
	}
	
	public boolean onTouchEvent(MotionEvent e) {
		mGestureDetector.onTouchEvent(e);
		mScaleDetector.onTouchEvent(e);
		mRotationListener.onTouchEvent(e);
		
		float x = e.getX();
		float y = e.getY();
		
		switch(e.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touched = true;
				mPreviousX = x;
				mPreviousY = y;
				down();
				break;
			case MotionEvent.ACTION_UP:
				dragging = false;
				touched = false;
				up();
			case MotionEvent.ACTION_MOVE:
				if (!touched) {
					break;
				}
				if (dragging && !mScaleDetector.isInProgress() && !mRotationListener.isInProgress()) {
					drag(mPreviousX, mPreviousY, x, y);
				} 
				mPreviousX = x;
				mPreviousY = y;
		}
		return true;
	}
	
	class GestureListener extends GestureDetector.SimpleOnGestureListener {
	   @Override
	   public boolean onDown(MotionEvent e) {
	       return true;
	   }
	   @Override
	   public void onLongPress(MotionEvent e) {
		  dragging = true;
		  longPress(e.getX(), e.getY());
	   }
	   @Override
	   public boolean onFling(MotionEvent e1, MotionEvent e2, float velX, float velY) {
		   fling(e1.getX(), e1.getY(), e2.getX(), e2.getY(), flingScaleFactor * velX, flingScaleFactor * velY);
		   return false;
	   }
	   @Override
	   public boolean onSingleTapUp(MotionEvent e) {
		   tap(e.getX(), e.getY());
		   return false;
	   }
	}
	
	class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
	    @SuppressLint("NewApi")
		@Override
	    public boolean onScale(ScaleGestureDetector detector) {
	    	scale(detector.getScaleFactor(), detector.getCurrentSpanX(), detector.getCurrentSpanY());
	        return true;
	    }
	}
	private void longPress(float screenX, float screenY) {
		for (MotionEventSubscriber sub : subscribers) {
    		sub.longPress(screenX, screenY);
    	}
	}
	
	private void tap(float screenX, float screenY) {
		for (MotionEventSubscriber sub : subscribers) {
    		sub.tap(screenX, screenY);
    	}
	}
	private void drag(float oldScreenX, float oldScreenY, float newScreenX, float newScreenY) {
		for (MotionEventSubscriber sub : subscribers) {
    		sub.drag(oldScreenX, oldScreenY, newScreenX, newScreenY);
    	}
	}
	private void scale(float scaleFactor, float spanX, float spanY) {
		for (MotionEventSubscriber sub : subscribers) {
    		sub.scale(scaleFactor, spanX, spanY);
    	}
	}
	public void rotate(float angle, float focusScreenX, float focusScreenY) {
		for (MotionEventSubscriber sub : subscribers) {
    		sub.rotate(angle, focusScreenX, focusScreenY);
    	}
	}
	private void fling(float oldScreenX, float oldScreenY, float newScreenX, float newScreenY, float velocityX, float velocityY){
		for (MotionEventSubscriber sub : subscribers) {
    		sub.fling(oldScreenX, oldScreenY, newScreenX, newScreenY, velocityX, velocityY);
    	}
	}
	private void down() {
		for (MotionEventSubscriber sub : subscribers) {
			sub.down();
		}
	}
	private void up() {
		for (MotionEventSubscriber sub : subscribers) {
			sub.up();
		}
		
	}
	
}
