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
	private MessagePasser messagePasser;
	
	public FractalDisplayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setEGLContextClientVersion(2);
		motionEventHandler = new MotionEventHandler(context);
		motionEventHandler.addSubscriber(this);
		appContext = ((FractalDisplay)context.getApplicationContext());
		camera = appContext.getCamera();
		stateManager = appContext.getStateManager();
		messagePasser = appContext.getMessagePasser();
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
		} 
		select(screenX, screenY);
	}

	@Override
	public void longPress(float screenX, float screenY) {
		select(screenX, screenY);
	}

	@Override
	public void drag(float oldScreenX, float oldScreenY, float newScreenX,
			float newScreenY) {
		Log.d("View", "drag");
		if (manipulating()) {
			float[] oldNear = new float[4];
			float[] oldFar = new float[4];
			camera.getTouchRay(oldNear, oldFar, oldScreenX, oldScreenY);
			
			float[] newNear = new float[4];
			float[] newFar = new float[4];
			camera.getTouchRay(newNear, newFar, newScreenX, newScreenY);
			
			stateManager.moveSelectedTransform(oldNear, oldFar, newNear, newFar);
		} else { 
			camera.turn(newScreenX - oldScreenX, oldScreenY - newScreenY);
		}
	}

	@Override
	public void scale(float scaleFactor, float spanX, float spanY) {
		if (manipulating()) {
			float[] focus = new float[4];
			float[] endPoint = new float[4];
			camera.getTouchPoint(focus, 0, 0, 0);
			camera.getTouchPoint(endPoint, spanX, spanY, 0);
			stateManager.scaleSelectedTransform(scaleFactor, focus, endPoint);
		} else {
			camera.scale(scaleFactor);
		}
	}

	@Override
	public void rotate(float angle, float focusScreenX, float focusScreenY) {
		Log.d("Camera", "Rotating " + angle);
		if (manipulating()) {
			stateManager.rotateSelectedTransform(angle, camera.intoScreen());
		} else {
			camera.rotate(angle);
		}
	}

	@Override
	public void fling(float oldScreenX, float oldScreenY, float newScreenX,
			float newScreenY, float velocityX, float velocityY) {
		camera.spin(newScreenX - oldScreenX, oldScreenY - newScreenY, (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY));
	}

	@Override
	public void down() {
		messagePasser.SendMessage(MessagePasser.MessageType.SCREEN_TOUCHED, true);
		
		if (manipulating()) {
			stateManager.startManipulation();
		}
	}

	@Override
	public void up() {
		messagePasser.SendMessage(MessagePasser.MessageType.SCREEN_TOUCHED, false);
		
		if (manipulating()) {
			stateManager.finishManipulation();
		}
	}
	
	private void select(float screenX, float screenY) {
        float[] nearPoint = new float[4];
        float[] farPoint = new float[4];
		camera.getTouchRay(nearPoint, farPoint, screenX, screenY);
		stateManager.select(nearPoint, farPoint);
	}
	
	private boolean manipulating() {
		Log.d("FractalDisplayView", "Edit mode is : " + stateManager.isEditMode());
		Log.d("FractalDisplayView", "selected is : " + stateManager.getState().anyTransformSelected());
		return stateManager.isEditMode() && stateManager.getState().anyTransformSelected();
	}
}
