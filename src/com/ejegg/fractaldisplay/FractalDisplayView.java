package com.ejegg.fractaldisplay;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.ejegg.fractaldisplay.spatial.Camera;
import com.ejegg.fractaldisplay.touch.MotionEventHandler;
import com.ejegg.fractaldisplay.touch.MotionEventSubscriber;

public class FractalDisplayView extends GLSurfaceView implements MotionEventSubscriber {

	private MotionEventHandler motionEventHandler;
	private FractalDisplay appContext;
	private Camera camera;
	
	public FractalDisplayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setEGLContextClientVersion(2);
		motionEventHandler = new MotionEventHandler(context);
		motionEventHandler.addSubscriber(this);
		appContext = ((FractalDisplay)context.getApplicationContext());
		camera = appContext.getCamera();
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		//Log.d("FractalDisplayView", "onTouch");
		return motionEventHandler.onTouchEvent(e);
	}
	
	public MotionEventHandler getMotionEventHandler() {
		return motionEventHandler;
	}
	
	@Override
	public void tap(float screenX, float screenY) {
		camera.stop();
	}

	@Override
	public void longPress(float screenX, float screenY) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drag(float oldScreenX, float oldScreenY, float newScreenX,
			float newScreenY) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scale(float scaleFactor, float spanX, float spanY) {
		camera.scale(scaleFactor);		
	}

	@Override
	public void rotate(float angle, float focusScreenX, float focusScreenY) {
		Log.d("Camera", "Rotating " + angle);
		camera.rotate(angle);
	}

	@Override
	public void fling(float oldScreenX, float oldScreenY, float newScreenX,
			float newScreenY, float velocityX, float velocityY) {
		camera.spin(newScreenX - oldScreenX, oldScreenY - newScreenY, (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY));
	}

	@Override
	public void down() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void up() {
		// TODO Auto-generated method stub
		
	}
}
