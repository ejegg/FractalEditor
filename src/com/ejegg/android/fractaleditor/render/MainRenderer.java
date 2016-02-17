package com.ejegg.android.fractaleditor.render;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.ejegg.android.fractaleditor.MessagePasser;
import com.ejegg.android.fractaleditor.MessagePasser.MessageType;
import com.ejegg.android.fractaleditor.persist.FractalStateManager;
import com.ejegg.android.fractaleditor.spatial.Camera;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

public class MainRenderer implements GLSurfaceView.Renderer, MessagePasser.MessageListener{

	private FractalRenderer fractalRenderer;
	private CubeRenderer cubeRenderer;
	private TextureRenderer textureRenderer;
	private Camera camera;
	private FractalStateManager stateManager;
	private boolean accumulatePoints = false;
	private boolean editMode = false;
	private boolean renderThumbnail = false;
	private MessagePasser passer;
	private long lastCameraPosition;
	private long newCameraPosition;
	private float minDist = 100;
	private float maxDist = -100;
	private Bitmap thumbnail;
	
	public MainRenderer(Camera camera, FractalStateManager stateManager, MessagePasser passer) {
		this.camera = camera;
		this.stateManager = stateManager;
		this.passer = passer;
		calculateMinMaxDist();
		lastCameraPosition = camera.getLastMoveId();
		passer.Subscribe(this, MessageType.NEW_POINTS_AVAILABLE, MessageType.ACCUMULATION_MODE_CHANGED, MessageType.CAMERA_MOTION_CHANGED, MessageType.STATE_CHANGED);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		editMode = stateManager.isEditMode();
		//Log.d("MainRenderer", "Requested frame, edit mode is " + editMode);
		if (editMode) {
			clear();
			cubeRenderer.draw();
		} else {
			newCameraPosition = camera.getLastMoveId();
			if (newCameraPosition == lastCameraPosition) {
				textureRenderer.preRender();
				fractalRenderer.draw(true, minDist, maxDist);
				textureRenderer.draw();
				stateManager.incrementBufferIndex();
			} else {
				clear();
				fractalRenderer.draw(false, 0, 0);	
			}
			lastCameraPosition = newCameraPosition;
		}
		if (renderThumbnail) {
			createBitmap();
			renderThumbnail = false;
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
		
		//Log.d("MainRenderer", "Done onSurfaceChanged");
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		//Log.d("MainRenderer", "Starting onSurfaceCreated");
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
		
		//Log.d("MainRenderer", "Done onSurfaceCreated");
	}

	@Override
	public void ReceiveMessage(MessageType type, boolean value) {
		switch(type){
			case NEW_POINTS_AVAILABLE:
				accumulatePoints = value;
				if (minDist > maxDist) {
					Log.d("MainRenderer", String.format("MinDist is %f, MaxDist is %f", minDist, maxDist));
					calculateMinMaxDist();
				}
				if (stateManager.getBufferIndex() == 1) {
					renderThumbnail = true;
				}
				break;
			case ACCUMULATION_MODE_CHANGED:
				accumulatePoints = accumulatePoints || value;
				break;
			case STATE_CHANGED:
				minDist = 100;
				maxDist = -100;
				textureRenderer.clear();
				break;
			case CAMERA_MOTION_CHANGED:
				if (value) {
					calculateMinMaxDist();
				}
		}
	}

	private void calculateMinMaxDist() {
		minDist = 100;
		maxDist = -100;
		if (!stateManager.hasPoints()) {return;}
		float[] bbox = stateManager.getBoundingBox();
		float[] mvpMatrix = camera.getMVPMatrix();
		float[] vec = new float[4];
		Log.d("MainRenderer", String.format("Calculating max/min distances: X: [%f, %f], Y: [%f, %f], Z: [%f, %f] ", 
				bbox[0], bbox[3], bbox[1], bbox[4], bbox[2], bbox[5]));
		float dist;
		for (int i = 0; i < 8 ; i++ ) {
			int xInd = i < 4 ? 0 : 3;
			int yInd = i % 4 < 2 ? 1 : 4;
			int zInd = i % 2 == 0 ? 2 : 5;
			float[] vertex = new float[] {bbox[xInd], bbox[yInd], bbox[zInd], 1.0f};

			Matrix.multiplyMV(vec, 0, mvpMatrix, 0, vertex, 0);

			dist = vec[2];
			Log.d("MainRenderer", String.format("Vertex %d (%f, %f, %f): Distance is %f ", 
					i, vertex[0], vertex[1], vertex[2], dist));
			minDist = Math.min(minDist, dist);
			maxDist = Math.max(maxDist, dist);
		}
	}

	public Bitmap getThumbnail() {
		return thumbnail;
	}

	private void createBitmap() {
		int width = camera.getWidth();
		int height = camera.getHeight();
		Log.d("MainRenderer", String.format("Creating bitmap, width=%s, height=%s", width, height));
		int b[]=new int[width*height];
		int bt[]=new int[width*height];
		IntBuffer ib = IntBuffer.wrap(b);
		Log.d("MainRenderer", "Created buffers");
		ib.position(0);
		GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
		Log.d("MainRenderer", "did glReadPixels");
		for(int i=0, k=0; i<height; i++, k++) {
			// OpenGL bitmap is incompatible with Android bitmap
			// and so, some correction is needed.
			for(int j=0; j<width; j++) {
				int pix=b[i*width+j];
				int pb=(pix>>16)&0xff;
				int pr=(pix<<16)&0x00ff0000;
				int pix1=(pix&0xff00ff00) | pr | pb;
				bt[(height-k-1)*width+j]=pix1;
			}
		}
		Log.d("MainRenderer", "Transformed buffer to android format");
		thumbnail = Bitmap.createBitmap(bt, width, height, Bitmap.Config.ARGB_8888);
		Log.d("MainRenderer", "Created bitmap");
	}
}
