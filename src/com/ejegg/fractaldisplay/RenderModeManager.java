package com.ejegg.fractaldisplay;

import android.opengl.GLSurfaceView;
import android.util.Log;

import com.ejegg.fractaldisplay.MessagePasser.MessageType;
import com.ejegg.fractaldisplay.persist.FractalStateManager;
import com.ejegg.fractaldisplay.spatial.Camera;

public class RenderModeManager implements MessagePasser.MessageListener{

	private FractalStateManager stateManager;
	private FractalDisplayView view;
	private boolean cameraMoving;
	private boolean screenTouched = false;
    private boolean continuousCalculation = false;
        
	public RenderModeManager(MessagePasser messagePasser, FractalStateManager stateManager, Camera camera, FractalDisplayView view) {
		messagePasser.Subscribe(this, MessageType.CAMERA_MOTION_CHANGED, MessageType.SCREEN_TOUCHED, 
				MessageType.CAMERA_MOVED,
				MessageType.STATE_CHANGED,
				MessageType.STATE_CHANGING,
				MessageType.NEW_POINTS_AVAILABLE,
				MessageType.EDIT_MODE_CHANGED);
		cameraMoving = camera.isMoving();
		this.view = view;
		this.stateManager = stateManager;
		checkAccumulate();
	}

	private void checkAccumulate() {
		boolean newValue = !(screenTouched || cameraMoving);
		if (newValue != continuousCalculation) {
			stateManager.setContinuousCalculation(newValue);
		}
		continuousCalculation = newValue;
	}

	@Override
	public void ReceiveMessage(MessageType t, boolean value) {
		Log.d("RenderModeManager", "Got message of type " + t + ", value= "+ value);
		switch (t) {
			case CAMERA_MOTION_CHANGED:
				cameraMoving = value;
				view.setRenderMode(cameraMoving ? GLSurfaceView.RENDERMODE_CONTINUOUSLY : GLSurfaceView.RENDERMODE_WHEN_DIRTY);
				break;
			case SCREEN_TOUCHED:
				screenTouched = value;
				break;
			case EDIT_MODE_CHANGED:
				stateManager.setContinuousCalculation(!value);
			case CAMERA_MOVED:
			case STATE_CHANGED:
			case STATE_CHANGING:
			case NEW_POINTS_AVAILABLE:
				view.requestRender();
		}
		checkAccumulate();
	}
	
}
