package com.ejegg.fractaldisplay;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.ejegg.fractaldisplay.persist.FractalStateManager;
import com.ejegg.fractaldisplay.spatial.Camera;
import com.ejegg.fractaldisplay.touch.MotionEventHandler;
import com.ejegg.fractaldisplay.touch.MotionEventSubscriber;

public class FractalDisplayView extends GLSurfaceView implements MotionEventSubscriber {

	private MotionEventHandler motionEventHandler;
	private FractalDisplay appContext;
	private FractalStateManager stateManager;
	private Camera camera;

	
	public FractalDisplayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setEGLContextClientVersion(2);
		motionEventHandler = new MotionEventHandler(context);
		motionEventHandler.addSubscriber(this);
		appContext = ((FractalDisplay)context.getApplicationContext());
		camera = appContext.getCamera();
		stateManager = appContext.getStateManager();
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
		if (camera.isMoving()) {
			camera.stop();
		} else if (stateManager.getEditMode()) {
			select(screenX, screenY);
		}
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
	
	private void select(float screenX, float screenY) {
        float[] nearPoint = new float[4];
        float[] farPoint = new float[4];
		camera.getTouchRay(nearPoint, farPoint, screenX, screenY);
		stateManager.select(nearPoint, farPoint);
	}
}
