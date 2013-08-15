package com.ejegg.fractaldisplay;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.ejegg.fractaldisplay.touch.MotionEventHandler;

public class FractalDisplayView extends GLSurfaceView {

	private MotionEventHandler motionEventHandler;
	
	public FractalDisplayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setEGLContextClientVersion(2);
		motionEventHandler = new MotionEventHandler(context);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		//Log.d("FractalDisplayView", "onTouch");
		return motionEventHandler.onTouchEvent(e);
	}
	
	public MotionEventHandler getMotionEventHandler() {
		return motionEventHandler;
	}
}
