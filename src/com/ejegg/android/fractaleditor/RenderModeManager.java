package com.ejegg.android.fractaleditor;

import android.opengl.GLSurfaceView;
import android.util.Log;

import com.ejegg.android.fractaleditor.MessagePasser.MessageType;
import com.ejegg.android.fractaleditor.persist.FractalStateManager;
import com.ejegg.android.fractaleditor.spatial.Camera;

public class RenderModeManager implements MessagePasser.MessageListener{

	private FractalStateManager stateManager;
	private FractalEditView view;
	private boolean cameraMoving;
	private boolean screenTouched = false;
        
	public RenderModeManager(MessagePasser messagePasser, FractalStateManager stateManager, Camera camera, FractalEditView view) {
		messagePasser.Subscribe(this, MessageType.CAMERA_MOTION_CHANGED, MessageType.SCREEN_TOUCHED, 
				MessageType.CAMERA_MOVED,
				MessageType.STATE_CHANGED,
				MessageType.STATE_CHANGING,
				MessageType.NEW_POINTS_AVAILABLE,
				MessageType.EDIT_MODE_CHANGED);
		cameraMoving = camera.isMoving();
		this.view = view;
		this.stateManager = stateManager;
		Log.d("RenderModeManager", "constructed");
		checkAccumulate();
	}

	public void checkAccumulate() {
		stateManager.setContinuousCalculation(!(screenTouched || cameraMoving));
	}

	@Override
	public void ReceiveMessage(MessageType t, boolean value) {
		Log.d("RenderModeManager", "Got message of type " + t + ", value= "+ value);
		switch (t) {
			case CAMERA_MOTION_CHANGED:
				cameraMoving = value;
				view.setRenderMode(cameraMoving ? GLSurfaceView.RENDERMODE_CONTINUOUSLY : GLSurfaceView.RENDERMODE_WHEN_DIRTY);
				checkAccumulate();
				if (!value) {
					view.requestRender();
				}
				break;
			case SCREEN_TOUCHED:
				screenTouched = value;
				checkAccumulate();
				break;
			case EDIT_MODE_CHANGED:
				if (value) {
					stateManager.setContinuousCalculation(false);
				} else {
					checkAccumulate();
				}
			case CAMERA_MOVED:
			case STATE_CHANGED:
			case STATE_CHANGING:
			case NEW_POINTS_AVAILABLE:
				view.requestRender();		
		}
	}
	
}
