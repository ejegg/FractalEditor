package com.ejegg.fractaldisplay.render;

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
	private TextureRenderer textureRenderer;
	private Camera camera;
	private FractalStateManager stateManager;
	private boolean screenTouched = false;
	
	public MainRenderer(Camera camera, FractalStateManager stateManager) {
		this.camera = camera;
		this.stateManager = stateManager;
	}
	
	@Override
	public void onDrawFrame(GL10 gl) {
		
		if (stateManager.isEditMode()) {
			clear();
			cubeRenderer.draw();
		} else if (camera.isMoving() || screenTouched) {
			clear();
			fractalRenderer.draw(false);
		} else {
			textureRenderer.preRender();
			fractalRenderer.draw(true);
			textureRenderer.draw();
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
		textureRenderer = new TextureRenderer(camera, stateManager);
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

	public void setScreenTouched(boolean screenTouched) {
		this.screenTouched = screenTouched;
	}
}