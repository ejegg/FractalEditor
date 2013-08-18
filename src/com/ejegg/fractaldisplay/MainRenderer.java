package com.ejegg.fractaldisplay;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.ejegg.fractaldisplay.persist.FractalStateManager;
import com.ejegg.fractaldisplay.spatial.Camera;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

public class MainRenderer implements GLSurfaceView.Renderer{

	private FractalRenderer fractalRenderer;
	private CubeRenderer cubeRenderer;
	private Camera camera;
	private FractalStateManager stateManager;
	
	public MainRenderer(Camera camera, FractalStateManager stateManager) {
		this.camera = camera;
		this.stateManager = stateManager;
		this.fractalRenderer = new FractalRenderer(camera, stateManager);
		this.cubeRenderer = new CubeRenderer(camera, stateManager);
	}
	
	@Override
	public void onDrawFrame(GL10 gl) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		if (stateManager.getEditMode()) {
			cubeRenderer.draw();
		} else {
			fractalRenderer.draw();
		}
		if (camera.isMoving()) {
			camera.spinStep(); //TODO: make this time-based instead of frame based
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		Log.d("MainRenderer", "onSurfaceChanged");
		GLES20.glViewport(0, 0, width, height);
		camera.setScreenDimensions(width,height);
		fractalRenderer.initialize();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		Log.d("MainRenderer", "Starting onSurfaceCreated");
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glClearDepthf(-10.0f);
		GLES20.glEnable( GLES20.GL_DEPTH_TEST );
		GLES20.glDepthFunc( GLES20.GL_GEQUAL );
    	GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glDepthMask( true );

    	GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
	}
}
