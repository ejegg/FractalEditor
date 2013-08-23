package com.ejegg.fractaldisplay.render;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.ejegg.fractaldisplay.MessagePasser;
import com.ejegg.fractaldisplay.MessagePasser.MessageType;
import com.ejegg.fractaldisplay.persist.FractalStateManager;
import com.ejegg.fractaldisplay.spatial.Camera;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

public class MainRenderer implements GLSurfaceView.Renderer, MessagePasser.MessageListener{

	private FractalRenderer fractalRenderer;
	private CubeRenderer cubeRenderer;
	private TextureRenderer textureRenderer;
	private Camera camera;
	private FractalStateManager stateManager;
	private boolean accumulatePoints = false;
	private boolean editMode = false;
	private MessagePasser passer;
	private long lastCameraPosition;
	private long newCameraPosition;
	private GLSurfaceView view;
	
	public MainRenderer(Camera camera, FractalStateManager stateManager, MessagePasser passer, GLSurfaceView view) {
		this.camera = camera;
		this.stateManager = stateManager;
		this.passer = passer;
		this.view = view;
		
		passer.Subscribe(this, MessageType.NEW_POINTS_AVAILABLE, MessageType.ACCUMULATION_MOTION_CHANGED);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		editMode = stateManager.isEditMode();
		Log.d("MainRenderer", "Requested frame, edit mode is " + editMode);
		if (editMode) {
			clear();
			cubeRenderer.draw();
		} else {
			newCameraPosition = camera.getLastMoveId();
			if ((newCameraPosition == lastCameraPosition) && accumulatePoints) {
				textureRenderer.preRender();
				fractalRenderer.draw(true);
				textureRenderer.draw();
				if (fractalRenderer.getBufferIndex() > 0) {
					view.requestRender();
				}
			} else {
				clear();
				fractalRenderer.draw(false);	
			}
			lastCameraPosition = newCameraPosition;
		}
			
		if (camera.isMoving()) {
			camera.spinStep();
		} 
	}
	
	private void clear() {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		Log.d("MainRenderer", "onSurfaceChanged");
		GLES20.glViewport(0, 0, width, height);
		camera.setScreenDimensions(width,height);
		if (textureRenderer != null) {
			textureRenderer.freeResources();
		}
		textureRenderer = new TextureRenderer(camera, stateManager, passer);
		Log.d("MainRenderer", "Done onSurfaceChanged");
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		Log.d("MainRenderer", "Starting onSurfaceCreated");
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glClearDepthf(1.0f);
		GLES20.glEnable( GLES20.GL_DEPTH_TEST );
		GLES20.glDepthFunc( GLES20.GL_LEQUAL );
    	GLES20.glEnable(GLES20.GL_CULL_FACE);
    	GLES20.glCullFace(GLES20.GL_BACK);
		GLES20.glDepthMask( true );
    	GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		fractalRenderer = new FractalRenderer(camera, stateManager);
		cubeRenderer = new CubeRenderer(camera, stateManager);
		
		Log.d("MainRenderer", "Done onSurfaceCreated");
	}

	@Override
	public void ReceiveMessage(MessageType type, boolean value) {
		switch(type){
			case NEW_POINTS_AVAILABLE:
				accumulatePoints = value;
				break;
			case ACCUMULATION_MOTION_CHANGED:
				accumulatePoints = accumulatePoints || value;
				break;
		}
	}
}
